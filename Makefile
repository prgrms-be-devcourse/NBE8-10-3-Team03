.PHONY: help local-up k6 local-probe-up probe-k6 local-prodlike-up prodlike-k6 local-up down limits ps logs clean perf-check-env perf cloud-monitor-up cloud-monitor-down

help:
	@echo ""
	@echo "============================"
	@echo " Docker Compose Control"
	@echo "============================"
	@echo ""
	@echo "[기본 실행]"
	@echo "  make local-up            # 기본 실행 (모니터링 포함)"
	@echo "  make k6                  # 기본 k6 실행"
	@echo ""
	@echo "[Probe 환경 - 문제 탐지]"
	@echo "  make local-probe-up      # probe 환경 실행"
	@echo "  make probe-k6            # probe 환경에서 k6 실행"
	@echo ""
	@echo "[Prodlike 환경 - 운영 유사]"
	@echo "  make local-prodlike-up   # prodlike 환경 실행"
	@echo "  make perf PERF_SCENARIO=smoke-test ENV=local DOMAIN=auction    # prodlike 환경에서 k6 실행"
	@echo ""
	@echo "[Cloud 환경]"
	@echo "  make local-up ENV=cloud  # cloud 환경 실행"
	@echo "  make perf ENV=cloud DOMAIN=auction PERF_SCENARIO=smoke-test  # 로컬 k6로 원격 서버 대상 부하테스트"
	@echo "  make cloud-monitor-up    # VM 전용 모니터링 스택(prometheus+grafana) 실행"
	@echo "  make cloud-monitor-down  # VM 전용 모니터링 스택 종료"
	@echo ""
	@echo "[유틸]"
	@echo "  make down               # 컨테이너 종료"
	@echo "  make limits             # CPU/MEM 제한 확인"
	@echo "  make ps                 # 실행중 컨테이너 확인"
	@echo "  make logs               # 로그 확인"
	@echo "  make clean              # 볼륨 포함 전체 삭제"
	@echo ""


# ----------------------------
# 로컬 환경 기본 (자원 제한 없음)
# ----------------------------
local-up:
	docker compose \
		-f docker-compose.yml \
		-f docker/compose/docker-compose.monitoring.yml \
		up -d

k6:
	docker compose \
		-f docker/compose/docker-compose.k6.yml \
		run --rm k6

# ----------------------------
# 로컬 환경 probe (문제 탐지)
# ----------------------------
local-probe-up:
	docker compose \
		-f docker-compose.yml \
		-f docker/compose/docker-compose.monitoring.yml \
		-f docker/compose/docker-compose.probe.yml \
		-f docker/compose/docker-compose.local.yml \
		up -d

probe-k6:
	docker compose \
		-f docker-compose.yml \
		-f docker/compose/docker-compose.probe.yml \
		-f docker/compose/docker-compose.k6.yml \
		run --rm k6

# ----------------------------
# 로컬 환경 prodlike (운영 유사)
# ----------------------------
local-prodlike-up:
	docker compose \
		-f docker-compose.yml \
		-f docker/compose/docker-compose.monitoring.yml \
		-f docker/compose/docker-compose.prodlike.yml \
		-f docker/compose/docker-compose.local.yml \
		up -d

prodlike-k6:
	docker compose \
		-f docker-compose.yml \
		-f docker-compose.prodlike.yml \
		--profile k6 run --rm k6

# ----------------------------
# 클라우드 환경 (자원 제한 없음)
# ----------------------------
cloud-up:
	docker compose \
		-f docker-compose.yml \
		-f docker/compose/docker-compose.monitoring.yml \
		-f docker/compose/docker-compose.cloud.yml \
		up -d

cloud-monitor-up:
	docker compose \
		-p monitoring-cloud \
		-f docker/compose/docker-compose.monitoring.cloud.yml \
		up -d

cloud-monitor-down:
	docker compose \
		-p monitoring-cloud \
		-f docker/compose/docker-compose.monitoring.cloud.yml \
		down

# ----------------------------
# 공통
# ----------------------------
down:
	docker compose \
	    -f docker-compose.yml \
	    -f docker/compose/docker-compose.monitoring.yml \
	    -f docker/compose/docker-compose.prodlike.yml \
	    -f docker/compose/docker-compose.local.yml \
	    down

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
	docker ps

logs:
	docker compose logs -f

clean:
	docker compose \
	    -f docker-compose.yml \
		-f docker/compose/docker-compose.monitoring.yml \
		down -v

# ----------------------------
# perf env 분기 실행 (local/cloud)
# ----------------------------
# 사용법:
#   make perf PERF_SCENARIO=stress ENV=local
#   make perf PERF_SCENARIO=load ENV=cloud
# ----------------------------
ENV ?= local
ENV_FILE := perf/env/$(ENV).env

PERF_SCENARIO ?= test
PERF_SCRIPT   ?= /scripts/$(DOMAIN)/$(PERF_SCENARIO).js

PERF_RESULTS_ROOT := perf/results
PERF_TS := $(shell date +"%Y%m%d-%H%M%S")
PERF_OUT_DIR := $(PERF_RESULTS_ROOT)/$(ENV)/$(DOMAIN)/$(PERF_SCENARIO)
PERF_OUT_JSON := /results/$(ENV)/$(DOMAIN)/$(PERF_SCENARIO)/$(PERF_SCENARIO)-$(PERF_TS).json

PERF_COMPOSE_FILE := docker/compose/docker-compose.k6.yml
ifeq ($(ENV),cloud)
PERF_COMPOSE_FILE := docker/compose/docker-compose.k6.cloud.yml
endif

perf-check-env:
	@test -f "$(ENV_FILE)" || (echo "❌ env file not found: $(ENV_FILE)"; exit 1)
	@mkdir -p "$(PERF_OUT_DIR)"

perf: perf-check-env
	@echo "▶ Running k6 scenario=$(PERF_SCENARIO) ENV=$(ENV) compose=$(PERF_COMPOSE_FILE)"
	MSYS_NO_PATHCONV=1 docker compose \
	  -f $(PERF_COMPOSE_FILE) \
	  --profile k6 run --rm \
	  $$(grep -vE '^\s*#|^\s*$$' "$(ENV_FILE)" | sed 's/\r$$//' | awk -F= '{printf "-e %s=%s ", $$1, $$2}') \
	  k6 run \
	  --summary-export="$(PERF_OUT_JSON)" \
	  "$(PERF_SCRIPT)"
	@echo "✅ Saved: $(PERF_OUT_DIR)/$(PERF_SCENARIO)-$(PERF_TS).json"
