import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, TEST_USERS, TEST_PASSWORD, login, authHeaders } from '../common.js';

const errorRate = new Rate('errors');
const postListDuration = new Trend('post_list_duration');
const postDetailDuration = new Trend('post_detail_duration');

const DETAIL_MODE = (__ENV.POST_DETAIL_MODE || 'distributed').trim().toLowerCase();
const HOT_IDS = (__ENV.POST_HOT_IDS || '')
  .split(',')
  .map((v) => Number(v.trim()))
  .filter((v) => Number.isFinite(v) && v > 0);
const CACHED_ID_SIZE = Math.max(1, Number(__ENV.POST_CACHED_ID_SIZE || 3));

let cachedHeaders = null;
let cachedPostIds = [];

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
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    errors: ['rate<0.005'],
  },
};

function getHeaders() {
  if (!cachedHeaders) {
    const username = TEST_USERS[(__VU - 1) % TEST_USERS.length];
    const credentials = login(username, TEST_PASSWORD);
    if (!credentials) return null;
    cachedHeaders = authHeaders(credentials);
  }
  return cachedHeaders;
}

function parseJson(res) {
  try {
    return res.json();
  } catch (_) {
    return null;
  }
}

function listChecks(res) {
  const body = parseJson(res);
  return check(res, {
    'post list - status 200': (r) => r.status === 200,
    'post list - resultCode 2xx': () => !!body?.resultCode && String(body.resultCode).startsWith('2'),
    'post list - data.content array': () => Array.isArray(body?.data?.content),
  });
}

function detailChecks(res) {
  const body = parseJson(res);
  return check(res, {
    'post detail - status 200': (r) => r.status === 200,
    'post detail - resultCode 2xx': () => !!body?.resultCode && String(body.resultCode).startsWith('2'),
    'post detail - data.id exists': () => Number.isFinite(body?.data?.id),
  });
}

function collectIdsFromList(res) {
  const body = parseJson(res);
  const items = body?.data?.content;
  if (!Array.isArray(items)) return [];
  return items
    .map((item) => Number(item?.id))
    .filter((id) => Number.isFinite(id) && id > 0);
}

function pickDetailId(listIds) {
  if (DETAIL_MODE === 'focused_hot') {
    if (HOT_IDS.length === 0) return null;
    return HOT_IDS[__ITER % HOT_IDS.length];
  }

  if (DETAIL_MODE === 'focused_cached') {
    for (const id of listIds) {
      if (cachedPostIds.length >= CACHED_ID_SIZE) break;
      if (!cachedPostIds.includes(id)) cachedPostIds.push(id);
    }
    if (cachedPostIds.length === 0) return null;
    return cachedPostIds[__ITER % cachedPostIds.length];
  }

  if (listIds.length === 0) return null;
  return listIds[Math.floor(Math.random() * listIds.length)];
}

export default function () {
  const headers = getHeaders();
  if (!headers) return;

  group('Post API', () => {
    const listRes = http.get(
      `${BASE_URL}/api/v1/posts?page=0&size=10`,
      { ...headers, tags: { scenario: 'post_get_load', endpoint: 'post_list', method: 'GET', detail_mode: DETAIL_MODE } }
    );
    postListDuration.add(listRes.timings.duration);
    const listOk = listChecks(listRes);
    errorRate.add(!listOk);

    const listIds = collectIdsFromList(listRes);
    const detailId = pickDetailId(listIds);

    if (detailId) {
      const detailRes = http.get(
        `${BASE_URL}/api/v1/posts/${detailId}`,
        { ...headers, tags: { scenario: 'post_get_load', endpoint: 'post_detail', method: 'GET', detail_mode: DETAIL_MODE } }
      );
      postDetailDuration.add(detailRes.timings.duration);
      const detailOk = detailChecks(detailRes);
      errorRate.add(!detailOk);
    }

    sleep(1);
  });
}
