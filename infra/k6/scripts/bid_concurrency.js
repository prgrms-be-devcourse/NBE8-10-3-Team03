import http from "k6/http";
import { check, sleep } from "k6";
import exec from "k6/execution";

const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8080";
const AUCTION_ID = Number(__ENV.AUCTION_ID || "1");

// 토큰 풀: "apiKey accessToken" 한 줄에 하나씩
// 예)
// apiKey1 accessToken1
// apiKey2 accessToken2
const TOKENS = (__ENV.TOKENS || "").trim().split("\n").filter(Boolean);

// 테스트 강도
const SPIKE_VUS = Number(__ENV.SPIKE_VUS || "200");

// 가격 전략
// BASE_PRICE는 현재가보다 충분히 크게 (규칙: 150%/즉시구매가 제한은 너희 데이터에 맞게 설정)
const BASE_PRICE = Number(__ENV.BASE_PRICE || "11000");
const PRICE_STEP = Number(__ENV.PRICE_STEP || "1"); // 1~10 추천 (너무 크면 규칙 위반 날 수 있음)

function authHeaders() {
  if (TOKENS.length === 0) {
    throw new Error("TOKENS env가 비어있음. 'apiKey accessToken' 라인들을 넣어야 함.");
  }
  // VU별로 토큰 고정 (연속 입찰 금지 회피)
  const idx = (exec.vu.idInTest - 1) % TOKENS.length;
  const [apiKey, accessToken] = TOKENS[idx].trim().split(/\s+/);

  // 서버 구현이 "Bearer {apiKey} {accessToken}" 파싱한다고 했으니 그대로 넣는다.
  return {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${apiKey} ${accessToken}`,
    },
  };
}

export const options = {
  scenarios: {
    warmup: {
      executor: "constant-vus",
      vus: 10,
      duration: "10s",
      exec: "placeBids",
    },
    spike: {
      executor: "constant-vus",
      vus: SPIKE_VUS,
      duration: "5s",
      exec: "placeBids",
      startTime: "10s",
    },
    cooldown: {
      executor: "constant-vus",
      vus: 10,
      duration: "10s",
      exec: "placeBids",
      startTime: "15s",
    },
    verify: {
      executor: "per-vu-iterations",
      vus: 1,
      iterations: 1,
      exec: "verifyAuction",
      startTime: "26s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.20"], // 규칙 위반(400) 때문에 실패율 0은 불가능, 대신 상한만 둔다
    http_req_duration: ["p(95)<800"], // 동시성/락 대기 터지면 여기부터 무너짐
  },
};

const successPrices = []; // 성공한 입찰 가격들(샘플)
let successCount = 0;

export function placeBids() {
  const url = `${BASE_URL}/api/v1/auctions/${AUCTION_ID}/bids`;

  // VU별로 가격을 조금씩 다르게(최고가 경쟁이 실제로 생기게)
  // 동시에 들어가면 currentHighestBid 갱신에서 Race가 드러난다.
  const myPrice = BASE_PRICE + (exec.vu.idInTest % 50) * PRICE_STEP;

  const res = http.post(url, JSON.stringify({ price: myPrice }), authHeaders());

  // 200이면 성공
  if (res.status === 200) {
    successCount++;
    if (successPrices.length < 200) successPrices.push(myPrice);
  }

  // 입찰 규칙 위반은 정상적으로 섞일 수 있음(특히 400-3, 400-6)
  check(res, {
    "bid response status is 200/400/401/403/404": (r) =>
      [200, 400, 401, 403, 404].includes(r.status),
  });

  // 너무 빠르게 연타하면 "연속 입찰"이 섞일 수 있으니 아주 짧게 쉰다.
  sleep(0.05);
}

function max(arr) {
  let m = -Infinity;
  for (const v of arr) if (v > m) m = v;
  return m;
}

export function verifyAuction() {
  // 1) 경매 상세 조회
  const detailUrl = `${BASE_URL}/api/v1/auctions/${AUCTION_ID}`;
  const detailRes = http.get(detailUrl, authHeaders());

  check(detailRes, { "auction detail 200": (r) => r.status === 200 });

  const detailJson = detailRes.json();
  const serverHighest = detailJson?.data?.currentHighestBid;
  const serverBidCount = detailJson?.data?.bidCount;

  // 2) 입찰 내역 조회 (size 크게 잡아 totalElements로 검증)
  const bidsUrl = `${BASE_URL}/api/v1/auctions/${AUCTION_ID}/bids?page=0&size=1`;
  const bidsRes = http.get(bidsUrl, authHeaders());
  check(bidsRes, { "bids list 200": (r) => r.status === 200 });

  const bidsJson = bidsRes.json();
  const totalElements = bidsJson?.data?.totalElements;

  // 3) k6가 관측한 성공 가격 최고값(샘플 기반)
  const localMax = max(successPrices);

  // 판정(동시성 버그)
  // 서버 최고가가 (성공 입찰 최고가)보다 작으면 "갱신 누락/꼬임" 확정
  const okHighest = typeof serverHighest === "number" && serverHighest >= localMax;

  // bidCount 정합성
  const okCount =
    typeof serverBidCount === "number" &&
    typeof totalElements === "number" &&
    serverBidCount === totalElements;

  check(
    { okHighest, okCount, serverHighest, localMax, serverBidCount, totalElements },
    {
      "INVARIANT: currentHighestBid >= max(successful bid prices)": (o) => o.okHighest,
      "INVARIANT: bidCount == bids.totalElements": (o) => o.okCount,
    }
  );

  console.log(
    JSON.stringify(
      {
        auctionId: AUCTION_ID,
        k6_successCount_sampled: successCount,
        k6_localMax_sampled: localMax,
        serverHighest,
        serverBidCount,
        bids_totalElements: totalElements,
        verdict: okHighest && okCount ? "PASS" : "FAIL",
      },
      null,
      2
    )
  );
}
