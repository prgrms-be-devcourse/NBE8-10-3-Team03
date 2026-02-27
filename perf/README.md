# perf (k6 성능/부하 테스트)

이 폴더는 **k6 기반 성능 테스트**를 실행하고, 결과를 **Prometheus/Grafana**로 관측하기 위한 스크립트/설정/결과를 관리합니다.

- 목표: API 병목 탐지, 성능 개선 전/후 비교, 운영 유사(prodlike) 부하 검증
- 실행 방식: `docker compose` + `k6` 컨테이너
- 결과 저장: `perf/results/<ENV>/...json` (k6 summary-export)

---

## 빠른 시작

### 1) 환경변수 파일 준비

`perf/env/<env>.env` 파일에 k6에서 사용할 환경 변수를 넣습니다.

예시(`perf/env/local.env`):


BASE_URL=http://localhost:8080

API_KEY=...


> `make perf`는 env 파일을 읽어서 `-e KEY=VALUE` 형태로 k6 컨테이너에 주입합니다.

---

### 2) 실행

Makefile에 등록된 타겟을 사용합니다.

- 기본 실행(서비스 띄우기)
    - `make local-up`
- k6 실행
    - `make k6` 또는 `make perf`

예시:

```bash
make local-up
make perf ENV=local PERF_SCENARIO=smoke PERF_SCRIPT=/scripts/smoke-test.js
```

>ENV, PERF_SCENARIO, PERF_SCRIPT는 Makefile 기본값이 있으면 생략 가능합니다.

---

### 3) Makefile 타겟 정리 (요약)

실제 타겟명/설명은 Makefile 기준으로 동기화하세요.

- `make local-up` : 로컬 실행(자원 제한 없음)
- `make k6` : 기본 k6 실행
- `make local-probe-up` / `make local-probe-k6` : 문제 탐지용(probe) 환경/테스트
- `make local-prodlike-up` / `make local-prodlike-k6` : 운영 유사(prodlike) 환경/테스트
- `make down` : 컨테이너 종료
- `make limits` : 컨테이너 CPU/MEM 제한 확인
- `make logs`, `make ps`, `make clean` 등

---

### 4) perf 실행 방식 (핵심)

`make perf`는 보통 아래 흐름입니다.

1. `perf/env/<ENV>.env` 읽기
2. env 파일의 각 라인을 `-e KEY=VALUE`로 변환하여 k6 컨테이너에 주입
3. `k6 run --summary-export=... <script>` 실행
4. 결과 JSON 저장

예시(개념):
```bash
docker compose --profile k6 run --rm \
  $(cat perf/env/local.env | ... | awk '{printf "-e %s=%s ", $1, $2}') \
  k6 run \
  --summary-export="perf/results/local/<scenario>-<timestamp>.json" \
  "/scripts/<script>.js"
```

---

### 5) 시나리오 설계 가이드 (Load / Stress)
**기본 원칙**

- ** VU(가상유저)는 목표 RPS가 아니라 “동시성(대기/네트워크 포함)” **에 가깝습니다.
- RPS 목표가 있으면 constant-arrival-rate(도착률 기반) 시나리오를 고려할 수 있습니다.
- 로컬/단일 VM에서는 VU를 과도하게 올리면 테스트 도구(k6) 자체가 병목이 됩니다.

**권장 접근**
Load Test (동시 1000명 요구가 있을 때)
- “동시 1000명”은 실제로는 동시 세션/요청 패턴 정의가 필요합니다.
- 단일 k6 인스턴스로 1000 VU가 무조건 정답은 아닙니다.
- 시작점 예시:
    - 50~200 VU로 시작 → 병목 확인 → 단계적으로 증가
    - 또는 도착률 기반으로 RPS 100 같은 목표를 먼저 고정

Stress Test (동시 5000명 요구가 있을 때)
- 단일 머신/단일 k6로 5000 VU는 현실적으로 과할 수 있습니다.
- 목표: “깨지는 지점(에러율 증가/응답시간 폭증/리소스 한계)” 찾기
- 방법:
    - 스텝 업 방식(예: 100 → 200 → 400 → 800…)
    - 또는 arrival-rate를 점증

>결론: “VU를 숫자로 고정”하기보다, 목표(응답시간/에러율/SLO) + 트래픽 모델을 먼저 잡고 VU/RPS를 맞추는 방식 추천.

---

### 6) 결과 파일 확인

k6 실행 후 다음 경로로 결과가 저장됩니다.
- `perf/results/<ENV>/<DOMAIN>/<scenario>-<timestamp>.json`

예:
- `perf/results/local/auction/smoke-test-20260225-173220.json`

이 JSON에는:
- 테스트 요약(평균/퍼센타일/에러율 등)
- 커스텀 메트릭
- threshold 결과

---

### 7) 자원 제한(CPU/MEM) 운영 가이드
**로컬에서 자원 제한을 거는 이유**

- 로컬 PC가 너무 강하면 “운영 환경보다 과하게 좋은 수치”가 나올 수 있음
- 운영(예: 소형 인스턴스급) 환경을 흉내내려면 제한을 두는 게 의미가 있음

**prodlike 환경에서 제한이 더 큰 경우**
- prodlike는 모니터링 스택이 더 무거워질 수 있어(prometheus/grafana)
    - 제한을 적절히 늘리는 건 합리적일 수 있음
- 단, 로컬/클라우드 비교 시에는:
    - 테스트 결과 해석 시 제한 조건을 함께 기록해야 함

**k6 컨테이너는 제한을 안 거는 게 보통 유리**
- k6가 병목이 되면 테스트가 왜곡됨
- 단일 VM에서 리소스가 빡빡하면 최소한으로만 제한하거나 분산 실행 고려

---

### 8) Windows / macOS 주의사항
`MSYS_NO_PATHCONV=1`는 누구에게 영향?
- 이 옵션은 주로 Git Bash / MSYS 환경의 경로 변환 문제를 막기 위한 것
- macOS에는 보통 영향 없음
- Makefile에 포함해도 mac 사용자는 대부분 문제 없이 지나갑니다.

---

### 9) 클라우드에서 돌릴 때 체크리스트

- 테스트 전용 VM(애플리케이션 + DB/Redis를 같은 VM에 올릴지 여부 결정)
- 외부에서 접근할 BASE_URL 설정
- (선택) nginx를 앞단에 붙일 경우:
    - keep-alive / timeouts / proxy buffer 등 설정에 따라 응답시간에 영향
- 로컬과 수치 비교 시:
    - VM 스펙, 자원 제한 여부, 네트워크 레이턴시를 기록

---

### 10) 병목 판정 기준 (Bottleneck Criteria)

성능 이슈는 보통 **증상(k6)** 과 **원인(서버/DB/GC/IO)** 을 같이 봐야 정확히 판정됩니다.
아래 기준은 “어디가 먼저 무너지는지”를 빠르게 가르는 체크리스트입니다.

**(1) k6(클라이언트 관점)에서 병목 의심 신호**

✅ 응답시간 악화 패턴
- p95가 지속적으로 상승하거나, 부하를 조금만 올려도 기울기가 급격히 증가하면 병목 가능성 큼
- p99가 p95 대비 과도하게 튀는 경우(꼬리 지연) → 락/GC/DB 대기 같은 “간헐적 대기” 의심

권장 SLO 예시(팀 상황에 맞게 수정):
- p95 < 300~500ms
- p99 < 1~2s
- error rate < 1%

✅ 에러율 증가

- 에러율 0.5~1% 넘어가며 증가 추세면 임계 근접/초과
- 5xx 증가: 서버 과부하/예외/타임아웃
- 429 증가: rate limit / queue overflow / 보호 로직 발동
- timeout 증가: downstream(DB/외부 API) 또는 서버 스레드 고갈

✅ 처리량(throughput) 정체
- VU 또는 arrival-rate를 올리는데도 RPS가 더 이상 증가하지 않고 평평해지면 병목 구간
- 이때 동시에 p95/p99가 올라가면 “포화(Queueing)”로 판단

**(2) 서버/애플리케이션 관점에서 병목 판정 (원인별 시그널)**

A. CPU 병목(Compute Bound)

판정 신호
- App/DB/Redis 중 하나라도 CPU가 지속적으로 85~90% 이상
- 처리량 정체 + latency 상승 + context switch 증가

추정 원인
- 비효율 로직, 직렬화/역직렬화 비용, 암호화/압축, 과도한 로그, busy loop

확인 지표
- process/container CPU, system load average

B. 메모리/GC 병목(Java)

판정 신호
- GC pause가 증가하면서 p99가 튀고, 부하 상승에 따라 pause가 같이 늘어남
- Old Gen 사용량이 계속 치솟고 Full GC 발생
- OOM 또는 STW(Stop-The-World)로 지연 스파이크

확인 지표(권장)
- JVM Heap usage(Young/Old), GC pause time, GC count
- jvm_gc_pause_seconds_*, jvm_memory_used_bytes 계열

C. 스레드/커넥션 고갈(Queueing, Thread Pool/DB Pool)

판정 신호
- CPU는 여유 있는데 latency가 증가 + 처리량 정체
- 스레드 풀 큐가 쌓이거나, DB 커넥션 풀 사용률이 지속적으로 90~100%
- timeout이 늘고 “대기 시간” 성격의 지표가 증가

확인 지표
- (Java/Spring) 톰캣/Netty thread pool active/queue
- HikariCP: active/idle/pending(대기) 커넥션

D. DB 병목 (Query / Lock / IO)

판정 신호
- App latency 상승과 동시에 DB latency가 상승
- 슬로우쿼리 수 증가, lock wait 증가
- DB CPU 또는 디스크 IO가 포화

확인 지표
- DB query latency / connections
- InnoDB lock waits, row lock time
- disk iowait, read/write throughput
- slow query log + EXPLAIN(튜닝)

E. 네트워크/외부 의존성 병목 (Downstream)

판정 신호
- 특정 API만 유독 느려지고, 서버 자원(CPU/메모리)은 여유
- 외부 API 호출 시간이 증가하면 전체 latency가 연쇄적으로 증가
- timeout/5xx가 “특정 경로”에 집중

확인 지표
- upstream/downstream 별 latency(가능하면 분리)
- 외부 호출 실패율/timeout

---

### 11) 원인 분류 빠른 결론 규칙 (실전)

- **RPS 정체 + CPU 90%↑** → CPU 병목
- **RPS 정체 + CPU 여유 + DB pool 90~100%** → DB 커넥션/쿼리 병목
- **p99만 튐 + GC pause spikes** → GC/Stop-the-world 병목
- **CPU 여유 + thread/queue 증가 + timeout 증가** → 스레드/큐 적체(동시성/블로킹) 병목
- **특정 API만 느림** → 해당 API의 DB 쿼리/락/외부 호출부터 파보기

---

### 12) “테스트 도구(k6) 병목”도 배제하기

k6가 먼저 병목이면 결과가 왜곡됩니다. 아래 증상이면 k6 리소스도 확인하세요.

- k6 컨테이너 CPU가 90%↑ / 메모리 부족 / 드롭 증가
- VU를 올리면 서버가 아니라 k6에서 먼저 limit
- (권장) k6는 자원 제한을 최소화하거나, 필요 시 분산 실행 고려

---

### 13) 운영/공유용 기록 템플릿

테스트 결과를 남길 때 아래를 같이 적어두면 나중에 “왜 좋아졌지/나빠졌지” 해석이 쉬워집니다.

- 테스트 날짜/시간:
- 환경(로컬/prodlike/클라우드, VM 스펙):
- 앱 버전/커밋:
- DB/Redis 배치(같은 VM/별도):
- 시나리오(도착률 or VU, duration, ramp):
- 결과 요약(p95/p99, error rate, RPS):
- 병목 추정(GC/DB pool/slow query/CPU/IO):
- 개선사항 및 재측정 결과: