import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * 테스트 옵션 (점진적 부하)
 */
export const options = {
  stages: [
    { duration: '30s', target: 5 },   // 워밍업
    { duration: '1m', target: 20 },   // 정상 트래픽
    { duration: '30s', target: 5 },   // 쿨다운
  ],
};

/**
 * 테스트용 계정 목록
 */
const users = [
  { username: 'user1', password: '1234' },
  { username: 'user2', password: '1234' },
  { username: 'user3', password: '1234' }
];

/**
 * setup()
 * - 테스트 시작 전에 1번만 실행
 * - 모든 계정 로그인 → apiKey + accessToken 확보
 */
export function setup() {
  const authList = [];

  for (const user of users) {
    const loginRes = http.post(
      'http://host.docker.internal:8080/api/v1/members/login',
      JSON.stringify({
        username: user.username,
        password: user.password,
      }),
      {
        headers: { 'Content-Type': 'application/json' },
      }
    );

    const success = check(loginRes, {
      'login status is 200': (r) => r.status === 200,
      'apiKey exists': (r) => r.json('apiKey') !== null,
      'accessToken exists': (r) => r.json('accessToken') !== null,
    });

    if (!success) {
      console.error(loginRes.body);
      throw new Error(`Login failed for user: ${user.username}`);
    }

    const apiKey = loginRes.json('data.apiKey');
    const accessToken = loginRes.json('data.accessToken');

    if (!apiKey || !accessToken) {
    console.error(loginRes.body);
    throw new Error('apiKey or accessToken is null');
    }

    authList.push({ apiKey, accessToken });

  }

  return { authList };
}

/**
 * 실제 부하 로직
 * - 각 VU가 랜덤 계정 선택
 * - 인증 필요한 API 호출
 */
export default function (data) {
  const auth =
    data.authList[Math.floor(Math.random() * data.authList.length)];

  const headers = {
    Authorization: `Bearer ${auth.apiKey} ${auth.accessToken}`,
  };


  // 1️⃣ 내 정보 조회
  const listRes = http.get(
    'http://host.docker.internal:8080/api/v1/members/me/auctions',
    { headers }
  );

  check(listRes, {
    'auctions list status is 200': (r) => r.status === 200,
  });

  sleep(1); // 사용자 think time
}
