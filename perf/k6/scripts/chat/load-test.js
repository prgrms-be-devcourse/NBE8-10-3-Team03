import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, TEST_USERS, buildAuthList, pickAuth, authHeader } from '../common.js';
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';

const CHAT_ITEM_ID = Number(__ENV.CHAT_ITEM_ID || 1);
const CHAT_TX_TYPE = (__ENV.CHAT_TX_TYPE || 'POST').trim().toUpperCase();
const CHAT_THINK_TIME = Number(__ENV.CHAT_THINK_TIME || 1);
const CHAT_MAX_USERS = Math.max(5, Number(__ENV.CHAT_MAX_USERS || 100));

const errorRate = new Rate('errors');
const roomCreateDuration = new Trend('chat_room_create_duration');
const sendDuration = new Trend('chat_send_duration');
const messagesDuration = new Trend('chat_messages_duration');
const listDuration = new Trend('chat_list_duration');
const roomIdByUser = new Map();

export const options = {
  scenarios: {
    load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 30 },
        { duration: '5m', target: 30 },
        { duration: '2m', target: 60 },
        { duration: '5m', target: 60 },
        { duration: '2m', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<700', 'p(99)<1200'],
    errors: ['rate<0.01'],
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
      tags: { scenario: 'chat_load', endpoint: 'chat_room_create', method: 'POST' },
    }
  );
  roomCreateDuration.add(res.timings.duration);
  const body = parseJson(res);
  const ok = check(res, {
    'chat room create - status 200': (r) => r.status === 200,
    'chat room create - resultCode 2xx': () => !!body?.resultCode && String(body.resultCode).startsWith('2'),
    'chat room create - roomId exists': () => typeof body?.data?.roomId === 'string' && body.data.roomId.length > 0,
  });
  errorRate.add(!ok);
  return { ok, roomId: body?.data?.roomId };
}

function sendMessage(credentials, roomId) {
  const form = new FormData();
  form.append('roomId', roomId);
  form.append('message', `LOAD chat message ${__VU}-${__ITER}`);

  const res = http.post(
    `${BASE_URL}/api/v1/chat/send`,
    form.body(),
    {
      headers: {
        ...authHeader(credentials),
        'Content-Type': `multipart/form-data; boundary=${form.boundary}`,
      },
      tags: { scenario: 'chat_load', endpoint: 'chat_send', method: 'POST' },
    }
  );
  sendDuration.add(res.timings.duration);
  const body = parseJson(res);
  const ok = check(res, {
    'chat send - status 200': (r) => r.status === 200,
    'chat send - resultCode 2xx': () => !!body?.resultCode && String(body.resultCode).startsWith('2'),
    'chat send - message id exists': () => Number.isFinite(body?.data?.chatId),
  });
  errorRate.add(!ok);
  return ok;
}

function getMessages(credentials, roomId) {
  const res = http.get(
    `${BASE_URL}/api/v1/chat/room/${roomId}`,
    {
      headers: authHeader(credentials),
      tags: { scenario: 'chat_load', endpoint: 'chat_messages', method: 'GET' },
    }
  );
  messagesDuration.add(res.timings.duration);
  const body = parseJson(res);
  const ok = check(res, {
    'chat messages - status 200': (r) => r.status === 200,
    'chat messages - resultCode 2xx': () => !!body?.resultCode && String(body.resultCode).startsWith('2'),
    'chat messages - data array': () => Array.isArray(body?.data),
  });
  errorRate.add(!ok);
}

function getChatList(credentials) {
  const res = http.get(
    `${BASE_URL}/api/v1/chat/list`,
    {
      headers: authHeader(credentials),
      tags: { scenario: 'chat_load', endpoint: 'chat_list', method: 'GET' },
    }
  );
  listDuration.add(res.timings.duration);
  const body = parseJson(res);
  const ok = check(res, {
    'chat list - status 200': (r) => r.status === 200,
    'chat list - resultCode 2xx': () => !!body?.resultCode && String(body.resultCode).startsWith('2'),
    'chat list - data array': () => Array.isArray(body?.data),
  });
  errorRate.add(!ok);
}

export function setup() {
  // user1은 기본 아이템 판매자일 가능성이 커서 제외
  const users = TEST_USERS.slice(1, 1 + CHAT_MAX_USERS);
  return { authList: buildAuthList(users) };
}

export default function (data) {
  const credentials = pickAuth(data?.authList);
  if (!credentials) {
    errorRate.add(true);
    return;
  }

  group('Chat API', () => {
    const username = credentials.username;
    let roomId = roomIdByUser.get(username);

    if (!roomId) {
      const created = createRoom(credentials);
      if (!created.ok || !created.roomId) return;
      roomId = created.roomId;
      roomIdByUser.set(username, roomId);
    }

    const sent = sendMessage(credentials, roomId);
    if (!sent) {
      roomIdByUser.delete(username);
      return;
    }

    getMessages(credentials, roomId);
    getChatList(credentials);
    sleep(CHAT_THINK_TIME);
  });
}
