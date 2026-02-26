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

export default function () {
  const headers = getHeaders();
  if (!headers) return;

  // ── 📌 거래(Post) 도메인 ──
  group('Post API', () => {
    const start = Date.now();
    const list = http.get(`${BASE_URL}/api/v1/posts?page=0&size=10`);
    check(list, { '거래글 목록 조회 성공': (r) => r.status === 200 });

    postDuration.add(Date.now() - start);
    errorRate.add(list.status !== 200);
    sleep(1);
  });

}