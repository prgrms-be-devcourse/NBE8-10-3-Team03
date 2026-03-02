import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL } from '../common.js';

const errorRate = new Rate('errors');
const searchDuration = new Trend('search_duration');

function parseList(raw, fallback) {
  if (!raw) return fallback;
  const values = raw
    .split(',')
    .map((v) => v.trim())
    .filter((v) => v.length > 0);
  return values.length > 0 ? values : fallback;
}

const HIT_AUCTION_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_HIT_AUCTION, ['아이폰', '갤럭시', '노트북']);
const HIT_POST_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_HIT_POST, ['[LT-POST]', 'loadtest seed content']);
const RARE_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_RARE, ['모니터', '키보드', '마우스']);
const MISS_KEYWORDS = parseList(__ENV.SEARCH_KEYWORDS_MISS, ['없는키워드1', '없는키워드2']);

function pickKeyword() {
  const roll = Math.random() * 100;
  if (roll < 35) return HIT_AUCTION_KEYWORDS[__ITER % HIT_AUCTION_KEYWORDS.length];
  if (roll < 70) return HIT_POST_KEYWORDS[__ITER % HIT_POST_KEYWORDS.length];
  if (roll < 95) return RARE_KEYWORDS[__ITER % RARE_KEYWORDS.length];
  return MISS_KEYWORDS[__ITER % MISS_KEYWORDS.length];
}

// 정렬 분포 (relevance 70 / newest 20 / oldest 10)
function pickSort() {
  const roll = Math.random() * 100;
  if (roll < 70) return 'relevance';
  if (roll < 90) return 'newest';
  return 'oldest';
}

function pickSize() {
  const s = parseInt(__ENV.SEARCH_SIZE || '20', 10);
  if (Number.isNaN(s) || s <= 0) return 20;
  return s;
}

// 한 VU가 한 번에 몇 페이지까지 “연속”으로 넘겨보는지 (기본 1~3)
function pickPagesToWalk() {
  const max = parseInt(__ENV.SEARCH_CURSOR_WALK_PAGES || '3', 10);
  if (Number.isNaN(max) || max <= 1) return 1;
  return 1 + (__ITER % max);
}

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
    http_req_duration: ['p(95)<2000'],
    errors: ['rate<0.005'],
  },
};

export default function () {
  const keyword = pickKeyword();
  const sort = pickSort();
  const size = pickSize();
  const pagesToWalk = pickPagesToWalk();

  group('Search API (cursor)', () => {
    let cursor = null;

    for (let i = 0; i < pagesToWalk; i++) {
      const start = Date.now();

      const url =
        `${BASE_URL}/api/v1/search` +
        `?keyword=${encodeURIComponent(keyword)}` +
        `&sort=${encodeURIComponent(sort)}` +
        `&size=${size}` +
        (cursor ? `&cursor=${encodeURIComponent(cursor)}` : '');

      const res = http.get(url, { tags: { case: 'search', sort, step: String(i) } });

      const ok = check(res, {
        '검색 성공(200)': (r) => r.status === 200,
        'content가 배열': (r) => Array.isArray(r.json()?.data?.content),
      });

      searchDuration.add(Date.now() - start, { sort, step: String(i) });
      errorRate.add(!ok);

      if (!ok) break;

      // 다음 커서 획득 (Slice 기반이면 data.nextCursor가 있어야 함)
      const body = res.json();
      const nextCursor = body?.data?.nextCursor;

      // 다음 페이지가 없거나 커서가 없으면 종료
      if (!nextCursor || body?.data?.hasNext === false) break;

      cursor = nextCursor;

      // “사용자가 더보기 누르는 텀” 흉내 (너무 짧게 하면 비현실적)
      sleep(0.3);
    }

    sleep(1);
  });
}