import http from "k6/http";
import { sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8080";

// vus 10명. 1, 10~50, 100, 1000
// duration 30초
export const options = {
  vus: 10,
  duration: "30s",
};

export default function () {
  http.get(`${BASE_URL}/`);
  sleep(1);
}
