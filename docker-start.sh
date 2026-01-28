#!/bin/bash

# ====================================
# Docker 환경 시작 스크립트
# ====================================

echo "🚀 Docker 환경 시작..."
echo ""

# 0. 기존 MySQL/Redis 컨테이너 확인
echo "🔍 기존 컨테이너 확인 중..."
MYSQL_RUNNING=$(docker ps --filter "name=mysql-1" --format "{{.Names}}")
REDIS_RUNNING=$(docker ps --filter "name=redis-auction" --format "{{.Names}}")

if [ -z "$MYSQL_RUNNING" ]; then
    echo "⚠️  경고: MySQL 컨테이너(mysql-1)가 실행 중이 아닙니다."
    echo "   다음 명령어로 MySQL을 시작하세요:"
    echo "   docker start mysql-1"
    echo ""
    read -p "계속 진행하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ 중단되었습니다."
        exit 1
    fi
else
    echo "✅ MySQL 컨테이너 실행 중: $MYSQL_RUNNING"
fi

if [ -z "$REDIS_RUNNING" ]; then
    echo "⚠️  경고: Redis 컨테이너(redis-auction)가 실행 중이 아닙니다."
    echo "   다음 명령어로 Redis를 시작하세요:"
    echo "   docker start redis-auction"
    echo ""
    read -p "계속 진행하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ 중단되었습니다."
        exit 1
    fi
else
    echo "✅ Redis 컨테이너 실행 중: $REDIS_RUNNING"
fi

echo ""

# 1. 기존 컨테이너 중지 및 삭제 (Spring Boot만)
echo "📦 기존 Spring Boot 컨테이너 정리 중..."
docker-compose down

# 2. 이미지 빌드
echo "🔨 Spring Boot 이미지 빌드 중..."
docker-compose build --no-cache app

# 3. 컨테이너 시작
echo "▶️  컨테이너 시작 중..."
docker-compose up -d

# 4. 로그 확인
echo ""
echo "✅ Docker 환경 시작 완료!"
echo ""
echo "📊 실시간 로그 확인:"
echo "   docker-compose logs -f app"
echo ""
echo "🔍 컨테이너 상태 확인:"
echo "   docker-compose ps"
echo ""
echo "🌐 접속 정보:"
echo "   - Spring Boot: http://localhost:8080"
echo "   - MySQL: localhost:3306 (기존 컨테이너)"
echo "   - Redis: localhost:6379 (기존 컨테이너)"
echo ""
echo "🛑 중지 명령어:"
echo "   docker-compose down"
echo ""

# 로그 자동 표시 (선택)
read -p "로그를 실시간으로 확인하시겠습니까? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
    docker-compose logs -f app
fi

