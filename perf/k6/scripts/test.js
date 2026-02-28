import http from "k6/http";
import { sleep } from "k6";
import { BASE_URL } from "./common.js";

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
