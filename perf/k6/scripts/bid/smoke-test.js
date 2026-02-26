// k6/scripts/bid-only-test.js
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, TEST_USERS, TEST_PASSWORD, login, authHeaders } from '../common.js';

// 커스텀 메트릭
const errorRate = new Rate('errors');
const bidScenarioDuration = new Trend('bid_scenario_duration');

export const options = {
  scenarios: {
    load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 5 },   // 워밍업
        { duration: '1m', target: 20 },   // 정상 트래픽
        { duration: '30s', target: 5 },   // 쿨다운
      ],
    },
  },
  thresholds: {
    // 전체 요청 p95
    http_req_duration: ['p(95)<500'],

    // 엔드포인트별 p95 (원하는 기준으로 조정)
    'http_req_duration{name:GET /api/v1/auctions (open list)}': ['p(95)<400'],
    'http_req_duration{name:GET /api/v1/auctions/:id/bids (top1)}': ['p(95)<400'],
    'http_req_duration{name:POST /api/v1/auctions/:id/bids}': ['p(95)<700'],

    errors: ['rate<0.005'],
  },
};

// ── 로그인 & 인증 헤더 (VU당 1회만 로그인, 이후 캐싱) ──
let cachedHeaders = null;
let cachedUsername = null;

function getAuth() {
  if (!cachedHeaders) {
    const username = TEST_USERS[(__VU - 1) % TEST_USERS.length];
    const credentials = login(username, TEST_PASSWORD);
    if (!credentials) return null;

    cachedUsername = username;
    cachedHeaders = authHeaders(credentials);
  }
  return { headers: cachedHeaders, username: cachedUsername };
}

export default function () {
  const auth = getAuth();
  if (!auth) return;

  const { headers, username: myUsername } = auth;

  group('Bid API Only', () => {
    const start = Date.now();

    // 1) OPEN 상태 경매 목록 조회 (분산 입찰용)
    const auctions = http.get(
      `${BASE_URL}/api/v1/auctions?page=0&size=10&status=OPEN`,
      { tags: { name: 'GET /api/v1/auctions (open list)' } }
    );
    check(auctions, { 'OPEN 경매 목록 조회 성공': (r) => r.status === 200 });
    errorRate.add(auctions.status !== 200);

    if (auctions.status !== 200) return;

    const auctionsBody = auctions.json();
    const items = auctionsBody?.data?.content ?? [];
    if (items.length === 0) {
      // OPEN 경매가 없으면 그냥 쉬었다가 다음 iteration
      sleep(1);
      return;
    }

    // VU별로 다른 경매를 고르도록 분산
    const idx = (__VU - 1) % items.length;
    const auctionId = items[idx].auctionId || items[idx].id;

    // 2) 최상위 입찰 1건 조회
    const bidList = http.get(
      `${BASE_URL}/api/v1/auctions/${auctionId}/bids`,
      { tags: { name: 'GET /api/v1/auctions/:id/bids (top1)' } }
    );

    const nowAuction = http.get(
              `${BASE_URL}/api/v1/auctions/${auctionId}`,
              { tags: { name: 'GET /api/v1/auctions/:id (top1)' } }
            );

    check(bidList, { '입찰 조회 성공': (r) => r.status === 200 });
    errorRate.add(bidList.status !== 200);

    if (bidList.status !== 200) return;

    const nowAuctionBody = nowAuction.json();
    const bidBody = bidList.json();

    // 입찰 최고가
    // 입찰 최고가가 없다면 (입찰이 없다면) 시작가가 입찰 최고가가 된다.
    const topPrice = nowAuctionBody?.data?.currentHighestBid ?? nowAuctionBody?.data?.startPrice;
    const topBid = bidBody?.data?.content?.[0];

    // 최고가 입찰자
    const topBidder = topBid?.bidderId || '';
    // 판매자 (경매 글 올린 사람)
    const sellerId = nowAuctionBody?.data?.seller?.id;
    // 내 ID (가상 사용자 ID)
    const myId = Number(myUsername?.replace(/\D/g, ''));

    // 3) 내가 최상위가 아닐 때 && 내가 판매자가 아닐때만 입찰
    if (topBidder !== myId && myId !== sellerId) {
      const newPrice = topPrice + 1000;

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

    bidScenarioDuration.add(Date.now() - start);
    sleep(1);
  });
}