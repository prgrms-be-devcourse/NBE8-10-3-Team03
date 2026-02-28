// k6/scripts/bid-only-test.js
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, buildAuthList, pickAuth, authHeaders } from '../common.js';

// 커스텀 메트릭
const errorRate = new Rate('errors');
const bidScenarioDuration = new Trend('bid_scenario_duration');

export const options = {
  scenarios: {
    load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },
        { duration: '5m', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '5m', target: 200 },
        { duration: '2m', target: 300 },
        { duration: '5m', target: 300 },
        { duration: '5m', target: 0 },
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

export function setup() {
  return { authList: buildAuthList() };
}

export default function (data) {
  const credentials = pickAuth(data?.authList);
  if (!credentials) return;
  const headers = authHeaders(credentials);
  const myUsername = credentials.username;

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
      `${BASE_URL}/api/v1/auctions/${auctionId}/bids?page=0&size=1&sort=price,desc`,
      { tags: { name: 'GET /api/v1/auctions/:id/bids (top1)' } }
    );
    check(bidList, { '입찰 목록 조회 성공': (r) => r.status === 200 });
    errorRate.add(bidList.status !== 200);

    if (bidList.status !== 200) return;

    const bidListBody = bidList.json();
    const topBid = bidListBody?.data?.content?.[0];

    const topBidder = topBid?.username || topBid?.memberUsername || '';
    const topPrice = topBid?.price || 0;

    // 3) 내가 최상위가 아닐 때만 입찰
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

    bidScenarioDuration.add(Date.now() - start);
    sleep(1);
  });
}
