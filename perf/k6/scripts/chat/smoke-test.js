import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, TEST_USERS, buildAuthList, pickAuth, authHeader } from '../common.js';
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';

const CHAT_ITEM_ID = Number(__ENV.CHAT_ITEM_ID || 1);
const CHAT_TX_TYPE = (__ENV.CHAT_TX_TYPE || 'POST').trim().toUpperCase();

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

function createRoom(credentials) {
  const res = http.post(
    `${BASE_URL}/api/v1/chat/room?itemId=${CHAT_ITEM_ID}&txType=${encodeURIComponent(CHAT_TX_TYPE)}`,
    null,
    {
      headers: authHeader(credentials),
      tags: { scenario: 'chat_smoke', endpoint: 'chat_room_create', method: 'POST' },
    }
  );
  const body = parseJson(res);
  const ok = check(res, {
    'chat room create - status 200': (r) => r.status === 200,
    'chat room create - resultCode 2xx': () => !!body?.resultCode && String(body.resultCode).startsWith('2'),
    'chat room create - roomId exists': () => typeof body?.data?.roomId === 'string' && body.data.roomId.length > 0,
  });
  return { ok, roomId: body?.data?.roomId };
}

function sendMessage(credentials, roomId) {
  const form = new FormData();
  form.append('roomId', roomId);
  form.append('message', `SMOKE chat message ${__VU}-${__ITER}`);

  const res = http.post(
    `${BASE_URL}/api/v1/chat/send`,
    form.body(),
    {
      headers: {
        ...authHeader(credentials),
        'Content-Type': `multipart/form-data; boundary=${form.boundary}`,
      },
      tags: { scenario: 'chat_smoke', endpoint: 'chat_send', method: 'POST' },
    }
  );
  const body = parseJson(res);
  const ok = check(res, {
    'chat send - status 200': (r) => r.status === 200,
    'chat send - resultCode 2xx': () => !!body?.resultCode && String(body.resultCode).startsWith('2'),
    'chat send - message id exists': () => Number.isFinite(body?.data?.chatId),
  });
  return { ok };
}

function getMessages(credentials, roomId) {
  const res = http.get(
    `${BASE_URL}/api/v1/chat/room/${roomId}`,
    {
      headers: authHeader(credentials),
      tags: { scenario: 'chat_smoke', endpoint: 'chat_messages', method: 'GET' },
    }
  );
  const body = parseJson(res);
  return check(res, {
    'chat messages - status 200': (r) => r.status === 200,
    'chat messages - resultCode 2xx': () => !!body?.resultCode && String(body.resultCode).startsWith('2'),
    'chat messages - data array': () => Array.isArray(body?.data),
  });
}

function getChatList(credentials) {
  const res = http.get(
    `${BASE_URL}/api/v1/chat/list`,
    {
      headers: authHeader(credentials),
      tags: { scenario: 'chat_smoke', endpoint: 'chat_list', method: 'GET' },
    }
  );
  const body = parseJson(res);
  return check(res, {
    'chat list - status 200': (r) => r.status === 200,
    'chat list - resultCode 2xx': () => !!body?.resultCode && String(body.resultCode).startsWith('2'),
    'chat list - data array': () => Array.isArray(body?.data),
  });
}

export function setup() {
  // user1은 itemId=1 기본 시드 상품의 판매자일 가능성이 커서 제외
  return { authList: buildAuthList([TEST_USERS[1]]) };
}

export default function (data) {
  const credentials = pickAuth(data?.authList);
  check(credentials, { 'login success': (v) => !!v });
  if (!credentials) return;

  const created = createRoom(credentials);
  if (!created.ok || !created.roomId) {
    console.error(`chat room create failed for ${credentials.username}`);
    return;
  }

  const sent = sendMessage(credentials, created.roomId);
  if (!sent.ok) {
    console.error(`chat send failed for ${credentials.username}, roomId=${created.roomId}`);
    return;
  }

  getMessages(credentials, created.roomId);
  getChatList(credentials);
}
