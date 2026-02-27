import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export default function () {
  const res = http.get(`${BASE_URL}/actuator/health`, { timeout: '3s' });
  console.log(`status=${res.status} body=${res.body}`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
