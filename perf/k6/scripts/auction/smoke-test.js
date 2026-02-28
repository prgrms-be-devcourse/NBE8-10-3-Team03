import http from 'k6/http';
import { check, sleep } from 'k6';
import { apiUrl } from '../common.js';

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 20 },
    { duration: '30s', target: 5 },
  ],
};

export default function () {
  const listRes = http.get(apiUrl('/api/v1/auctions?page=0&size=10'));

  check(listRes, {
    'auctions list status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
