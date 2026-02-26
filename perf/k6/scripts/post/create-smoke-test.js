import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, TEST_USERS, TEST_PASSWORD, login } from '../common.js';
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';

const CREATE_STEP = Math.max(1, Math.min(2, Number(__ENV.POST_CREATE_STEP || 1)));
const CATEGORY_ID = Number(__ENV.POST_CATEGORY_ID || 1);
const IMAGE_200KB_PATH = __ENV.POST_IMAGE_200KB_PATH || './assets/image-200kb.jpg';

const IMAGE_200KB_BIN = open(IMAGE_200KB_PATH, 'b');

export const options = {
  scenarios: {
    smoke_once: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '1m',
    },
  },
  thresholds: {
    checks: ['rate==1.0'],
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<3000'],
  },
};

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
  form.append('title', `SMOKE-POST-${uid}`);
  form.append('content', `SMOKE post create payload ${uid}`);
  form.append('price', '10000');
  form.append('categoryId', String(CATEGORY_ID));

  if (CREATE_STEP === 2) {
    form.append('images', http.file(IMAGE_200KB_BIN, 'image-200kb.jpg', 'image/jpeg'));
  }

  return form;
}

export default function () {
  const credentials = login(TEST_USERS[0], TEST_PASSWORD);
  check(credentials, { 'login success': (v) => !!v });
  if (!credentials) return;

  const authHeader = {
    Authorization: `Bearer ${credentials.apiKey} ${credentials.accessToken}`,
  };

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
        scenario: 'post_create_smoke',
        endpoint: 'post_create',
        method: 'POST',
        create_step: `step${CREATE_STEP}`,
      },
    }
  );

  const createBody = parseJson(createRes);
  const createOk = check(createRes, {
    'post create - status 201': (r) => r.status === 201,
    'post create - resultCode 2xx': () => !!createBody?.resultCode && String(createBody.resultCode).startsWith('2'),
    'post create - data.id exists': () => Number.isFinite(createBody?.data?.id),
  });
  if (!createOk) {
    console.error(`post create failed: status=${createRes.status}, body=${createRes.body}`);
  }

  const postId = Number(createBody?.data?.id);
  if (!createOk || !Number.isFinite(postId) || postId <= 0) return;

  const deleteRes = http.del(
    `${BASE_URL}/api/v1/posts/${postId}`,
    null,
    {
      headers: authHeader,
      tags: {
        scenario: 'post_create_smoke',
        endpoint: 'post_delete_cleanup',
        method: 'DELETE',
        create_step: `step${CREATE_STEP}`,
      },
    }
  );

  const deleteBody = parseJson(deleteRes);
  check(deleteRes, {
    'post delete cleanup - status 200': (r) => r.status === 200,
    'post delete cleanup - resultCode 2xx': () => !!deleteBody?.resultCode && String(deleteBody.resultCode).startsWith('2'),
  });
}
