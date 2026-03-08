// k6/scripts/load-test.js
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL } from '../common.js';

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

export default function () {
  // ── 📌 거래 도메인 (GET은 permitAll) ──
  group('Auction API', () => {
    const start = Date.now();
    const list = http.get(`${BASE_URL}/api/v1/posts`);
    check(list, { '거래 목록 조회 성공': (r) => r.status === 200 });

    auctionDuration.add(Date.now() - start);
    errorRate.add(list.status !== 200);
    sleep(1);
  });
}
