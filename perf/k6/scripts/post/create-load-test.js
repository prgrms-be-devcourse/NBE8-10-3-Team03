import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, TEST_USERS, TEST_PASSWORD, login } from '../common.js';
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';

const errorRate = new Rate('errors');
const postCreateDuration = new Trend('post_create_duration');
const postDeleteDuration = new Trend('post_delete_duration');

const CREATE_STEP = Math.max(1, Math.min(2, Number(__ENV.POST_CREATE_STEP || 1)));
const CATEGORY_ID = Number(__ENV.POST_CATEGORY_ID || 1);
const IMAGE_200KB_PATH = __ENV.POST_IMAGE_200KB_PATH || './assets/image-200kb.jpg';

const IMAGE_200KB_BIN = open(IMAGE_200KB_PATH, 'b');

let cachedAuthHeader = null;

export const options = {
  scenarios: {
    load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 20 },
        { duration: '5m', target: 20 },
        { duration: '2m', target: 40 },
        { duration: '5m', target: 40 },
        { duration: '2m', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
    errors: ['rate<0.005'],
  },
};

function getAuthHeader() {
  if (!cachedAuthHeader) {
    const username = TEST_USERS[(__VU - 1) % TEST_USERS.length];
    const credentials = login(username, TEST_PASSWORD);
    if (!credentials) return null;
    cachedAuthHeader = {
      Authorization: `Bearer ${credentials.apiKey} ${credentials.accessToken}`,
    };
  }
  return cachedAuthHeader;
}

function parseJson(res) {
  try {
    return res.json();
  } catch (_) {
    return null;
  }
}

function buildCreatePayload() {
  const uid = `${__VU}-${__ITER}`;
  const form = new FormData();
  form.append('title', `LT-POST-${uid}`);
  form.append('content', `LT-POST content for create load test ${uid}`);
  form.append('price', String(10000 + __ITER));
  form.append('categoryId', String(CATEGORY_ID));

  if (CREATE_STEP === 2) {
    form.append('images', http.file(IMAGE_200KB_BIN, 'image-200kb.jpg', 'image/jpeg'));
  }

  return form;
}

export default function () {
  const authHeader = getAuthHeader();
  if (!authHeader) return;

  group('Post Create API', () => {
    const createPayload = buildCreatePayload();
    const createRes = http.post(
      `${BASE_URL}/api/v1/posts`,
      createPayload.body(),
      {
        headers: {
          ...authHeader,
          'Content-Type': `multipart/form-data; boundary=${createPayload.boundary}`,
        },
        tags: {
          name: 'POST /api/v1/posts',
          scenario: 'post_create_load',
          endpoint: 'post_create',
          method: 'POST',
          create_step: `step${CREATE_STEP}`,
        },
      }
    );

    postCreateDuration.add(createRes.timings.duration);
    const createBody = parseJson(createRes);
    const createOk = check(createRes, {
      'post create - status 201': (r) => r.status === 201,
      'post create - resultCode 2xx': () => !!createBody?.resultCode && String(createBody.resultCode).startsWith('2'),
      'post create - data.id exists': () => Number.isFinite(createBody?.data?.id),
    });
    errorRate.add(!createOk);

    const postId = Number(createBody?.data?.id);
    if (Number.isFinite(postId) && postId > 0) {
      const deleteRes = http.del(
        `${BASE_URL}/api/v1/posts/${postId}`,
        null,
        {
          headers: authHeader,
          tags: {
            name: 'DELETE /api/v1/posts/:id',
            scenario: 'post_create_load',
            endpoint: 'post_delete_cleanup',
            method: 'DELETE',
            create_step: `step${CREATE_STEP}`,
          },
        }
      );

      postDeleteDuration.add(deleteRes.timings.duration);
      const deleteBody = parseJson(deleteRes);
      const deleteOk = check(deleteRes, {
        'post delete cleanup - status 200': (r) => r.status === 200,
        'post delete cleanup - resultCode 2xx': () => !!deleteBody?.resultCode && String(deleteBody.resultCode).startsWith('2'),
      });
      errorRate.add(!deleteOk);
    }

    sleep(1);
  });
}
