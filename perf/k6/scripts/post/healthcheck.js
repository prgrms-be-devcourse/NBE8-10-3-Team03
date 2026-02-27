import http from 'k6/http';
import { check } from 'k6';

export default function () {
  const res = http.get('http://host.docker.internal:8080/actuator/health', {
    timeout: '3s',
    tags: { name: 'GET /actuator/health' },
  });
  console.log(`status=${res.status} body=${res.body}`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
