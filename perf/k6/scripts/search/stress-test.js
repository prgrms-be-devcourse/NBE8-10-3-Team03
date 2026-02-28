import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from '../common.js';

const SCENARIO_TAG = 'search_stress';

const HIT_AUCTION_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_HIT_AUCTION, ['아이폰', '갤럭시', '노트북']);
const HIT_POST_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_HIT_POST, ['[LT-POST]', 'loadtest seed content']);
const RARE_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_RARE, ['모니터', '키보드', '마우스']);
const MISS_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_MISS, ['없는키워드1', '없는키워드2']);

function parseList(raw, fallback) {
  if (!raw) return fallback;

  const values = raw
    .split(',')
    .map((v) => v.trim())
    .filter((v) => v.length > 0);

  return values.length > 0 ? values : fallback;
}

function pickKeyword() {
  const roll = Math.random() * 100;

  // Keep total hit ratio at 70%, split evenly across auction/post seeds.
  if (roll < 35) return HIT_AUCTION_KEYWORDS[__ITER % HIT_AUCTION_KEYWORDS.length];
  if (roll < 70) return HIT_POST_KEYWORDS[__ITER % HIT_POST_KEYWORDS.length];
  if (roll < 95) return RARE_KEYWORDS[__ITER % RARE_KEYWORDS.length];
  return MISS_KEYWORDS[__ITER % MISS_KEYWORDS.length];
}

export const options = {
  scenarios: {
    search_stress_mode: {
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
      exec: 'searchStress',
      tags: { scenario: SCENARIO_TAG },
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.005'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{scenario:search_stress,endpoint:search_list,method:GET}': ['p(95)<500', 'p(99)<1000'],
  },
};

export function searchStress() {
  const keyword = pickKeyword();
  const url = `${BASE_URL}/api/v1/search?keyword=${encodeURIComponent(keyword)}&page=0&size=10`;

  const response = http.get(url, {
    tags: {
      scenario: SCENARIO_TAG,
      endpoint: 'search_list',
      method: 'GET',
    },
  });

  const body = response.json();
  check(response, {
    'search status is 200': (r) => r.status === 200,
    'search resultCode is success': () => (body?.resultCode || '').startsWith('2'),
    'search content is array': () => Array.isArray(body?.data?.content),
    'search content type is valid': () =>
      Array.isArray(body?.data?.content)
        ? body.data.content.every((item) => item?.type === 'POST' || item?.type === 'AUCTION')
        : false,
  });

  sleep(1);
}

export default searchStress;
