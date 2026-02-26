// k6/scripts/common.js
import http from 'k6/http';

export const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

// 시딩 계정: user1~user5 (정상), user6(SUSPENDED), user7(BANNED), admin, system
// 비밀번호: 전부 '1234'
export const TEST_USERS = Array.from({ length: 50 }, (_, i) => `user${i + 1}`);
export const TEST_PASSWORD = '1234';

// 로그인 후 apiKey + accessToken 획득
export function login(username, password) {
  const res = http.post(`${BASE_URL}/api/v1/members/login`,
    JSON.stringify({ username, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const body = res.json();
  if (!body.resultCode?.startsWith('200')) {
    console.error(`Login failed for ${username}: ${body.msg}`);
    return null;
  }

  return {
    apiKey: body.data.apiKey,
    accessToken: body.data.accessToken,
  };
}

// Authorization 헤더 생성 (Bearer {apiKey} {accessToken} 형식)
export function authHeaders(credentials) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${credentials.apiKey} ${credentials.accessToken}`,
    },
  };
}

// RsData 응답 검증 헬퍼
export function checkRsData(res, label) {
  const body = res.json();
  return {
    [`${label} - status 200`]: () => res.status === 200,
    [`${label} - resultCode OK`]: () => body.resultCode && body.resultCode.startsWith('2'),
  };
}