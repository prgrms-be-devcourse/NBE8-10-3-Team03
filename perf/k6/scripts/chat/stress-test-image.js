import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, TEST_USERS, buildAuthList, pickAuth, authHeader } from '../common.js';
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';

const CHAT_ITEM_ID = Number(__ENV.CHAT_ITEM_ID || 1);
const CHAT_TX_TYPE = (__ENV.CHAT_TX_TYPE || 'POST').trim().toUpperCase();
const CHAT_THINK_TIME = Number(__ENV.CHAT_THINK_TIME || 0.5);
const CHAT_MAX_USERS = Math.max(20, Number(__ENV.CHAT_MAX_USERS || 300));
const CHAT_IMAGE_SIZE = (__ENV.CHAT_IMAGE_SIZE || '200kb').trim().toLowerCase(); // 200kb | 1mb | mixed
const CHAT_IMAGE_RATIO = Math.max(0, Math.min(1, Number(__ENV.CHAT_IMAGE_RATIO || 1))); // 이미지 전송 비율
const CHAT_IMAGE_MIX_1MB_RATIO = Math.max(0, Math.min(1, Number(__ENV.CHAT_IMAGE_MIX_1MB_RATIO || 0.3)));

const IMAGE_200KB_PATH = __ENV.CHAT_IMAGE_200KB_PATH || '../post/assets/image-200kb.jpg';
const IMAGE_1MB_PATH = __ENV.CHAT_IMAGE_1MB_PATH || '../post/assets/image-1mb.jpg';
const IMAGE_200KB_BIN = open(IMAGE_200KB_PATH, 'b');
const IMAGE_1MB_BIN = open(IMAGE_1MB_PATH, 'b');

const errorRate = new Rate('errors');
const roomCreateDuration = new Trend('chat_room_create_duration');
const sendDuration = new Trend('chat_send_duration');
const messagesDuration = new Trend('chat_messages_duration');
const listDuration = new Trend('chat_list_duration');
const roomIdByUser = new Map();

export const options = {
  scenarios: {
    chat_image_stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '2m', target: 150 },
        { duration: '2m', target: 200 },
        { duration: '2m', target: 250 },
        { duration: '2m', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2500', 'p(99)<5000'],
    errors: ['rate<0.05'],
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
      tags: { scenario: 'chat_image_stress', endpoint: 'chat_room_create', method: 'POST' },
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

function pickImagePayload() {
  if (CHAT_IMAGE_SIZE === '1mb') {
    return { bin: IMAGE_1MB_BIN, filename: 'image-1mb.jpg' };
  }

  if (CHAT_IMAGE_SIZE === 'mixed') {
    return Math.random() < CHAT_IMAGE_MIX_1MB_RATIO
      ? { bin: IMAGE_1MB_BIN, filename: 'image-1mb.jpg' }
      : { bin: IMAGE_200KB_BIN, filename: 'image-200kb.jpg' };
  }

  return { bin: IMAGE_200KB_BIN, filename: 'image-200kb.jpg' };
}

function sendMessage(credentials, roomId) {
  const form = new FormData();
  form.append('roomId', roomId);
  form.append('message', `STRESS chat image message ${__VU}-${__ITER}`);

  if (Math.random() < CHAT_IMAGE_RATIO) {
    const image = pickImagePayload();
    form.append('images', http.file(image.bin, image.filename, 'image/jpeg'));
  }

  const res = http.post(
    `${BASE_URL}/api/v1/chat/send`,
    form.body(),
    {
      headers: {
        ...authHeader(credentials),
        'Content-Type': `multipart/form-data; boundary=${form.boundary}`,
      },
      tags: { scenario: 'chat_image_stress', endpoint: 'chat_send', method: 'POST' },
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
      tags: { scenario: 'chat_image_stress', endpoint: 'chat_messages', method: 'GET' },
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
      tags: { scenario: 'chat_image_stress', endpoint: 'chat_list', method: 'GET' },
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
  const users = TEST_USERS.slice(1, 1 + CHAT_MAX_USERS);
  return { authList: buildAuthList(users) };
}

export default function (data) {
  const credentials = pickAuth(data?.authList);
  if (!credentials) {
    errorRate.add(true);
    return;
  }

  group('Chat Image API', () => {
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
