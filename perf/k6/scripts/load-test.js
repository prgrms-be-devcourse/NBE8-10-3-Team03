// k6/scripts/load-test.js
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, TEST_USERS, TEST_PASSWORD, login, authHeaders } from '../common.js';

// 커스텀 메트릭
const errorRate = new Rate('errors');
const bidDuration = new Trend('bid_duration');
const auctionDuration = new Trend('auction_duration');
const postDuration = new Trend('post_duration');
const chatDuration = new Trend('chat_duration');

export const options = {
  scenarios: {
    load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 },
        { duration: '5m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '5m', target: 100 },
        { duration: '2m', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    errors: ['rate<0.005'],
  },
};

// ── 로그인 & 인증 헤더 (VU당 1회만 로그인, 이후 캐싱) ──
let cachedHeaders = null;

function getHeaders() {
  if (!cachedHeaders) {
    const username = TEST_USERS[(__VU - 1) % TEST_USERS.length];
    const credentials = login(username, TEST_PASSWORD);
    if (!credentials) return null;
    cachedHeaders = authHeaders(credentials);
  }
  return cachedHeaders;
}

// ── 테스트 진행 ──
export default function () {
  const headers = getHeaders();
  if (!headers) return;

  // ── 📌 멤버 도메인 ──
  group('Member API', () => {
    const me = http.get(`${BASE_URL}/api/v1/members/me`, {
      ...headers,
      tags: { name: 'GET /api/v1/members/me' },
    });
    check(me, { '회원 정보 조회 성공': (r) => r.status === 200 });
    errorRate.add(me.status !== 200);

    const myAuctions = http.get(
      `${BASE_URL}/api/v1/members/me/auctions?page=0&size=10&sort=createdAt,desc`,
      {
        ...headers,
        tags: { name: 'GET /api/v1/members/me/auctions' },
      }
    );
    check(myAuctions, { '내 경매 목록 조회 성공': (r) => r.status === 200 });

    const myPosts = http.get(
      `${BASE_URL}/api/v1/members/me/posts?page=0&size=10`,
      {
        ...headers,
        tags: { name: 'GET /api/v1/members/me/posts' },
      }
    );
    check(myPosts, { '내 거래글 목록 조회 성공': (r) => r.status === 200 });
    sleep(1);
  });

  // ── 📌 경매 도메인 (GET은 permitAll) ──
  group('Auction API', () => {
    const start = Date.now();
    const list = http.get(`${BASE_URL}/api/v1/auctions?page=0&size=10&sort=createdAt,desc`, {
      tags: { name: 'GET /api/v1/auctions' },
    });
    check(list, { '경매 목록 조회 성공': (r) => r.status === 200 });

    const listBody = list.json();
    if (listBody.data && listBody.data.content && listBody.data.content.length > 0) {
      const auctionId = listBody.data.content[0].auctionId || listBody.data.content[0].id;
      const detail = http.get(`${BASE_URL}/api/v1/auctions/${auctionId}`, {
        tags: { name: 'GET /api/v1/auctions/:id' },
      });
      check(detail, { '경매 상세 조회 성공': (r) => r.status === 200 });
    }
    auctionDuration.add(Date.now() - start);
    errorRate.add(list.status !== 200);
    sleep(1);
  });

  // ── 📌 입찰 도메인 ──
	group('Bid API', () => {
	  const start = Date.now();
	  const myUsername = TEST_USERS[(__VU - 1) % TEST_USERS.length];

	  // OPEN 상태 경매 여러 개 조회 (분산 입찰용)
	  const auctions = http.get(`${BASE_URL}/api/v1/auctions?page=0&size=10&status=OPEN`, {
      tags: { name: 'GET /api/v1/auctions (open list)' },
    });
	  const auctionsBody = auctions.json();

	  if (auctionsBody.data && auctionsBody.data.content && auctionsBody.data.content.length > 0) {
	    const items = auctionsBody.data.content;
	    const idx = (__VU - 1) % items.length;
	    const auctionId = items[idx].auctionId || items[idx].id;

	    // 입찰 목록 조회 → 최상위 입찰자 & 가격 확인
	    const bidList = http.get(
	      `${BASE_URL}/api/v1/auctions/${auctionId}/bids?page=0&size=1&sort=price,desc`,
        { tags: { name: 'GET /api/v1/auctions/:id/bids (top1)' } }
	    );
	    check(bidList, { '입찰 목록 조회 성공': (r) => r.status === 200 });

	    const bidListBody = bidList.json();
	    const topBid = bidListBody.data?.content?.[0];
	    const topBidder = topBid?.username || topBid?.memberUsername || '';
	    const topPrice = topBid?.price || 0;

	    // 최상위 입찰자가 자신이 아닐 때만 입찰
	    if (topBidder !== myUsername) {
	      const newPrice = topPrice + Math.floor(Math.random() * 1000) + 100;
	      const bidRes = http.post(
	        `${BASE_URL}/api/v1/auctions/${auctionId}/bids`,
	        JSON.stringify({ price: newPrice }),
	        {
            ...headers,
            tags: { name: 'POST /api/v1/auctions/:id/bids' },
          }
	      );
	      check(bidRes, { '입찰 성공': (r) => r.status === 200 });
	      errorRate.add(bidRes.status !== 200);
	    }
	    // 자신이 최상위 입찰자면 → 다른 경매에 입찰 시도
	    else if (items.length > 1) {
	      const altIdx = (idx + 1) % items.length;
	      const altAuctionId = items[altIdx].auctionId || items[altIdx].id;
	      const altBids = http.get(
	        `${BASE_URL}/api/v1/auctions/${altAuctionId}/bids?page=0&size=1&sort=price,desc`,
          { tags: { name: 'GET /api/v1/auctions/:id/bids (top1)' } }
	      );
	      const altBody = altBids.json();
	      const altTopBid = altBody.data?.content?.[0];
	      const altTopBidder = altTopBid?.username || altTopBid?.memberUsername || '';
	      const altTopPrice = altTopBid?.price || 0;

	      if (altTopBidder !== myUsername) {
	        const altPrice = altTopPrice + Math.floor(Math.random() * 1000) + 100;
	        const bidRes = http.post(
	          `${BASE_URL}/api/v1/auctions/${altAuctionId}/bids`,
	          JSON.stringify({ price: altPrice }),
	          {
              ...headers,
              tags: { name: 'POST /api/v1/auctions/:id/bids' },
            }
	        );
	        check(bidRes, { '대체 경매 입찰 성공': (r) => r.status === 200 });
	        errorRate.add(bidRes.status !== 200);
	      }
	    }
	  }
  bidDuration.add(Date.now() - start);
  sleep(1);
});

  // ── 📌 거래(Post) 도메인 ──
  group('Post API', () => {
    const start = Date.now();
    const list = http.get(`${BASE_URL}/api/v1/posts?page=0&size=10`, {
      tags: { name: 'GET /api/v1/posts' },
    });
    check(list, { '거래글 목록 조회 성공': (r) => r.status === 200 });

    const listBody = list.json();
    if (listBody.data && listBody.data.content && listBody.data.content.length > 0) {
      const postId = listBody.data.content[0].id;
      const detail = http.get(`${BASE_URL}/api/v1/posts/${postId}`, {
        tags: { name: 'GET /api/v1/posts/:id' },
      });
      check(detail, { '거래글 상세 조회 성공': (r) => r.status === 200 });
    }
    postDuration.add(Date.now() - start);
    errorRate.add(list.status !== 200);
    sleep(1);
  });

  // ── 📌 채팅 도메인 (REST API) ──
  group('Chat API', () => {
    const start = Date.now();
    const rooms = http.get(`${BASE_URL}/api/v1/chat/list`, {
      ...headers,
      tags: { name: 'GET /api/v1/chat/list' },
    });
    check(rooms, { '채팅방 목록 조회 성공': (r) => r.status === 200 });

    const roomsBody = rooms.json();
    if (roomsBody.data && roomsBody.data.length > 0) {
      const roomId = roomsBody.data[0].roomId || roomsBody.data[0].id;
      const messages = http.get(`${BASE_URL}/api/v1/chat/room/${roomId}`, {
        ...headers,
        tags: { name: 'GET /api/v1/chat/room/:id' },
      });
      check(messages, { '채팅 메시지 조회 성공': (r) => r.status === 200 });
    }
    chatDuration.add(Date.now() - start);
    errorRate.add(rooms.status !== 200);
    sleep(1);
  });
}
