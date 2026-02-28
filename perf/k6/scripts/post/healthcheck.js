import http from 'k6/http';
import { check } from 'k6';
import { apiUrl } from '../common.js';

export default function () {
  const res = http.get(apiUrl('/actuator/health'), { timeout: '3s' });
  console.log(`status=${res.status} body=${res.body}`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
