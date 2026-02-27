// k6/scripts/load-test.js
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, TEST_USERS, TEST_PASSWORD, login, authHeaders } from '../common.js';

// 커스텀 메트릭
const errorRate = new Rate('errors');
const searchDuration = new Trend('search_duration');

// 키워드 목록 파싱 함수
function parseList(raw, fallback) {
  if (!raw) return fallback;

  const values = raw
    .split(',')
    .map((v) => v.trim())
    .filter((v) => v.length > 0);

  return values.length > 0 ? values : fallback;
}

// 환경 변수로부터 키워드 설정 (디폴트 키워드도 지정)
const HIT_AUCTION_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_HIT_AUCTION, ['아이폰', '갤럭시', '노트북']);
const HIT_POST_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_HIT_POST, ['[LT-POST]', 'loadtest seed content']);
const RARE_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_RARE, ['모니터', '키보드', '마우스']);
const MISS_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_MISS, ['없는키워드1', '없는키워드2']);

// 키워드 무작위 선택 함수
function pickKeyword() {
  const roll = Math.random() * 100;

  if (roll < 35) return HIT_AUCTION_KEYWORDS[__ITER % HIT_AUCTION_KEYWORDS.length];
  if (roll < 70) return HIT_POST_KEYWORDS[__ITER % HIT_POST_KEYWORDS.length];
  if (roll < 95) return RARE_KEYWORDS[__ITER % RARE_KEYWORDS.length];
  return MISS_KEYWORDS[__ITER % MISS_KEYWORDS.length];
}

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
    http_req_duration: ['p(95)<500'],
    errors: ['rate<0.005'],
  },
};

// ── 로그인 & 인증 헤더 (VU당 1회만 로그인, 이후 캐싱) ──
let cachedHeaders = null;
let cachedUsername = null;

function getHeaders() {
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
  const headers = getHeaders();
  if (!headers) return;

  const keyword = pickKeyword(); // 키워드를 무작위로 선택

  // ── 📌 경매/게시물 검색 API 테스트 ──
  group('Search API', () => {
    const start = Date.now();
    const url = `${BASE_URL}/api/v1/search?keyword=${encodeURIComponent(keyword)}&status=OPEN&size=20`;

    const res = http.get(url, { headers, tags: { case: 'search' } });

    check(res, {
      "검색 성공": (r) => r.status === 200,
      "검색 결과가 배열인지": (r) => Array.isArray(r.json()?.data?.content),
    });

    searchDuration.add(Date.now() - start);
    errorRate.add(res.status !== 200);
    sleep(1);
  });
}