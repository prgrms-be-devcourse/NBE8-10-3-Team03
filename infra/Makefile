.PHONY: help up k6 probe-up probe-k6 prodlike-up prodlike-k6 down limits ps logs clean

help:
	@echo ""
	@echo "Usage:"
	@echo "  make up            # 기본 실행 (자원 제한 없음)"
	@echo "  make k6            # 기본 k6 실행"
	@echo ""
	@echo "  make probe-up      # probe(문제 탐지) 실행"
	@echo "  make probe-k6      # probe k6 실행"
	@echo ""
	@echo "  make prodlike-up   # prodlike(운영 유사) 실행"
	@echo "  make prodlike-k6   # prodlike k6 실행"
	@echo ""
	@echo "  make down          # 컨테이너 종료"
	@echo "  make limits        # 전체 컨테이너 CPU/MEM 제한 확인"
	@echo ""

# ----------------------------
# 기본 (자원 제한 없음)
# ----------------------------
up:
	docker compose up -d prometheus grafana

k6:
	docker compose --profile k6 run --rm k6

# ----------------------------
# probe (문제 탐지)
# ----------------------------
probe-up:
	docker compose \
		-f docker-compose.yml \
		-f docker-compose.probe.yml \
		up -d prometheus grafana

probe-k6:
	docker compose \
		-f docker-compose.yml \
		-f docker-compose.probe.yml \
		--profile k6 run --rm k6

# ----------------------------
# prodlike (운영 유사)
# ----------------------------
prodlike-up:
	docker compose \
		-f docker-compose.yml \
		-f docker-compose.prodlike.yml \
		up -d prometheus grafana

prodlike-k6:
	docker compose \
		-f docker-compose.yml \
		-f docker-compose.prodlike.yml \
		--profile k6 run --rm k6

# ----------------------------
# 공통
# ----------------------------
down:
	docker compose down

limits:
	docker ps --format '{{.Names}}' | while read name; do \
		docker inspect "$$name" \
		--format '{{.Name}} {{.HostConfig.NanoCpus}} {{.HostConfig.Memory}}' \
		| awk '{ \
			cpu=($$2==0?0:$$2/1000000000); \
			mem=($$3==0?0:$$3/1024/1024); \
			printf "%s CPU=%.2f MEM=%.0fMB\n", $$1, cpu, mem \
		}'; \
	done

ps:
	docker compose ps

logs:
	docker compose logs -f

clean:
	docker compose down -v

# ----------------------------
# bid concurrency test
# ----------------------------

BASE_URL ?= http://host.docker.internal:8080
AUCTION_ID ?= 1
SPIKE_VUS ?= 200
BASE_PRICE ?= 11000
PRICE_STEP ?= 1
TOKENS_FILE ?= k6/tokens.txt

tokens:
	python3 k6/gen_tokens.py --base-url http://host.docker.internal:8080 --count 100 --out $(TOKENS_FILE)

K6_RESULTS_DIR := k6/results
TOKENS_FILE   := k6/tokens.txt

bid-spike:
	@mkdir -p $(K6_RESULTS_DIR)
	docker compose --profile k6 run --rm \
	  -v "$(PWD)/$(K6_RESULTS_DIR):/results" \
	  -e BASE_URL="$(BASE_URL)" \
	  -e AUCTION_ID="$(AUCTION_ID)" \
	  -e SPIKE_VUS="$(SPIKE_VUS)" \
	  -e BASE_PRICE="$(BASE_PRICE)" \
	  -e PRICE_STEP="$(PRICE_STEP)" \
	  -e TOKENS="$$(cat $(TOKENS_FILE))" \
	  k6 run \
	  --out experimental-prometheus-rw \
	  --summary-export=/results/summary.json \
	  /scripts/bid_concurrency.js


bid-spike-300:
	$(MAKE) bid-spike SPIKE_VUS=300

bid-spike-1000:
	$(MAKE) bid-spike SPIKE_VUS=1000

bid-spike-10000:
	$(MAKE) bid-spike SPIKE_VUS=10000

bid-spike-30000:
	$(MAKE) bid-spike SPIKE_VUS=30000