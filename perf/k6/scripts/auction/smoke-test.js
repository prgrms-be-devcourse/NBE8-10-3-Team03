import http from 'k6/http';
import { check, sleep } from 'k6';
import { apiUrl, login } from '../common.js';

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
    const credentials = login(user.username, user.password);
    const success = check(credentials, {
      'login success': (v) => !!v,
      'apiKey exists': (v) => !!v?.apiKey,
      'accessToken exists': (v) => !!v?.accessToken,
    });

    if (!success) {
      throw new Error(`Login failed for user: ${user.username}`);
    }

    authList.push(credentials);
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
    apiUrl('/api/v1/members/me/auctions'),
    { headers }
  );

  check(listRes, {
    'auctions list status is 200': (r) => r.status === 200,
  });

  sleep(1); // 사용자 think time
}
