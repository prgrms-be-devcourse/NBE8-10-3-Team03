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

const CASES = [
    { name: "open_all_newest", params: "status=OPEN&sort=newest&size=20" },
    { name: "open_cat_newest", params: "status=OPEN&categoryName=디지털기기&sort=createdAt,desc&size=20" },
    { name: "open_all_endingSoon", params: "status=OPEN&sort=endingSoon&size=20" },
    { name: "open_cat_endingSoon", params: "status=OPEN&categoryName=유아동&sort=createdAt,asc&size=20" },
    { name: "closed_all_newest", params: "status=CLOSED&size=20" },
    { name: "open_all_price_desc", params: "status=OPEN&sort=createdAt,asc&size=20" },
];

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

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export default function () {
  const headers = getHeaders();
  if (!headers) return;

  const c = pick(CASES);

  // ── 📌 경매 도메인 (GET은 permitAll) ──
  group('Auction API', () => {
    const start = Date.now();
    const url = `${BASE_URL}/api/v1/auctions?${encodeURI(c.params)}`;

    const res = http.get(url, { tags: { case: c.name } });

    check(res, {
      "경매 조회 성공": (r) => r.status === 200,
    });


    auctionDuration.add(Date.now() - start);
    errorRate.add(res.status !== 200);
    sleep(1);
  });
}