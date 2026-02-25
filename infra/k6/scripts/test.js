import http from "k6/http";
import { sleep } from "k6";

// vus 10명. 1, 10~50, 100, 1000
// duration 30초
export const options = {
  vus: 10,
  duration: "30s",
};

export default function () {
  // 백엔드 로컬 8080일 시 필요함
  http.get("http://host.docker.internal:8080/");
  sleep(1);
}
