#!/bin/bash

echo "🚀 Starting Event-Driven Data Pipeline..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Clean up any existing containers
echo "🧹 Cleaning up existing containers..."
docker-compose down -v

# Start core services (without Java app initially)
echo "🔧 Starting core services..."
docker-compose up -d postgres kafka

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 30

# Check service health
echo "🔍 Checking service health..."
docker-compose ps

# Start Kafka UI and Kestra
echo "🎛️ Starting management interfaces..."
docker-compose up -d kafka-ui kestra

echo "✅ Pipeline is ready!"
echo ""
echo "📊 Access points:"
echo "  - Kafka UI: http://localhost:8090"
echo "  - Kestra: http://localhost:8081"
echo "  - PostgreSQL: localhost:5432"
echo ""
echo "🔧 To build and run Java app:"
echo "  cd java-app && mvn clean package"
echo "  docker-compose --profile java-app up -d java-app"
echo ""
echo "📝 To view logs:"
echo "  docker-compose logs -f [service-name]"
