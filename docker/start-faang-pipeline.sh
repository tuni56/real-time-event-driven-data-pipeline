#!/bin/bash

# 🚀 FAANG-Level Data Pipeline Startup Script
# Demonstrates production-ready infrastructure patterns

set -e

echo "🚀 Starting FAANG-Level Event-Driven Data Pipeline..."
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed. Please install it and try again."
    exit 1
fi

print_status "Starting infrastructure services..."

# Start core infrastructure
docker-compose up -d postgres kafka schema-registry redis prometheus grafana

print_status "Waiting for services to be healthy..."

# Wait for PostgreSQL
print_status "Waiting for PostgreSQL..."
until docker exec postgres pg_isready -U pipeline_user -d pipeline_db > /dev/null 2>&1; do
    sleep 2
done
print_success "PostgreSQL is ready!"

# Wait for Kafka
print_status "Waiting for Kafka..."
until docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
    sleep 2
done
print_success "Kafka is ready!"

# Wait for Schema Registry
print_status "Waiting for Schema Registry..."
until curl -s http://localhost:8082/subjects > /dev/null 2>&1; do
    sleep 2
done
print_success "Schema Registry is ready!"

# Create Kafka topics with optimized configurations
print_status "Creating optimized Kafka topics..."

docker exec kafka kafka-topics --create \
    --topic events \
    --bootstrap-server localhost:9092 \
    --partitions 3 \
    --replication-factor 1 \
    --config cleanup.policy=delete \
    --config retention.ms=604800000 \
    --config segment.ms=86400000 \
    --config compression.type=snappy \
    --if-not-exists

docker exec kafka kafka-topics --create \
    --topic events-dlq \
    --bootstrap-server localhost:9092 \
    --partitions 1 \
    --replication-factor 1 \
    --if-not-exists

print_success "Kafka topics created with FAANG-level optimizations!"

# Start remaining services
print_status "Starting Kafka UI and Kestra..."
docker-compose up -d kafka-ui kestra

print_status "Building and starting Java application..."
docker-compose --profile java-app up -d --build

# Wait for application to be ready
print_status "Waiting for application to start..."
sleep 10

until curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; do
    sleep 2
done

print_success "Application is ready!"

echo ""
echo "🎉 FAANG-Level Pipeline Successfully Started!"
echo "============================================="
echo ""
echo "📊 Service URLs:"
echo "   • Application:      http://localhost:8080"
echo "   • Health Check:     http://localhost:8080/actuator/health"
echo "   • Metrics:          http://localhost:8080/actuator/prometheus"
echo "   • Kafka UI:         http://localhost:8090"
echo "   • Schema Registry:  http://localhost:8082"
echo "   • Kestra:           http://localhost:8081 (admin/admin123)"
echo "   • Prometheus:       http://localhost:9090"
echo "   • Grafana:          http://localhost:3000 (admin/admin123)"
echo ""
echo "🚀 Quick Start Commands:"
echo "   • Produce test event:  curl -X POST 'http://localhost:8080/api/events?eventType=user_signup&userId=user123'"
echo "   • View real-time metrics: curl http://localhost:8080/api/metrics/realtime"
echo "   • Run load test:      ./load-test.sh"
echo ""
echo "📈 Performance Features:"
echo "   ✅ Exactly-once semantics"
echo "   ✅ Schema Registry with Avro"
echo "   ✅ Circuit breaker patterns"
echo "   ✅ Real-time stream processing"
echo "   ✅ Prometheus metrics"
echo "   ✅ Redis caching layer"
echo "   ✅ Optimized Kafka configuration"
echo ""
echo "🎯 Ready for FAANG interviews! 🎯"
