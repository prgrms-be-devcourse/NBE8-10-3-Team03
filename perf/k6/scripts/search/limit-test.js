import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from '../common.js';

const SCENARIO_TAG = 'search_limit';

const HIT_AUCTION_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_HIT_AUCTION, ['아이폰', '갤럭시', '노트북']);
const HIT_POST_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_HIT_POST, ['[LT-POST]', 'loadtest seed content']);
const RARE_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_RARE, ['모니터', '키보드', '마우스']);
const MISS_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_MISS, ['없는키워드1', '없는키워드2']);

const LIMIT_VUS = parseVuList(__ENV.SEARCH_LIMIT_VUS, [10, 20, 30, 40, 50]);
const RAMP_DURATION = __ENV.SEARCH_LIMIT_RAMP_DURATION || '1m';
const HOLD_DURATION = __ENV.SEARCH_LIMIT_HOLD_DURATION || '3m';
const COOLDOWN_DURATION = __ENV.SEARCH_LIMIT_COOLDOWN_DURATION || '2m';

function parseList(raw, fallback) {
  if (!raw) return fallback;

  const values = raw
    .split(',')
    .map((v) => v.trim())
    .filter((v) => v.length > 0);

  return values.length > 0 ? values : fallback;
}

function parseVuList(raw, fallback) {
  if (!raw) return fallback;

  const values = raw
    .split(',')
    .map((v) => Number(v.trim()))
    .filter((v) => Number.isFinite(v) && v > 0);

  return values.length > 0 ? values : fallback;
}

function pickKeyword() {
  const roll = Math.random() * 100;

  if (roll < 35) return HIT_AUCTION_KEYWORDS[__ITER % HIT_AUCTION_KEYWORDS.length];
  if (roll < 70) return HIT_POST_KEYWORDS[__ITER % HIT_POST_KEYWORDS.length];
  if (roll < 95) return RARE_KEYWORDS[__ITER % RARE_KEYWORDS.length];
  return MISS_KEYWORDS[__ITER % MISS_KEYWORDS.length];
}

function buildStages() {
  const stages = [];
  let prev = 0;

  for (const target of LIMIT_VUS) {
    if (target !== prev) {
      stages.push({ duration: RAMP_DURATION, target });
    }
    stages.push({ duration: HOLD_DURATION, target });
    prev = target;
  }

  stages.push({ duration: COOLDOWN_DURATION, target: 0 });
  return stages;
}

export const options = {
  scenarios: {
    search_limit_mode: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: buildStages(),
      exec: 'searchLimit',
      tags: { scenario: SCENARIO_TAG },
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

export function searchLimit() {
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
  });

  sleep(1);
}

export default searchLimit;
