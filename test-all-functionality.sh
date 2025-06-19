#!/bin/bash

# Fast Kafka Study Test Runner - Using Docker Services
# This script runs tests against your running Docker services

set -e

echo "🚀 Fast Kafka Study Test Suite (Using Docker Services)"
echo "======================================================"

# Function to check if Docker services are running
check_services() {
    echo "🔍 Checking Docker services..."
    
    # Check Kafka
    if ! nc -z localhost 9092 2>/dev/null; then
        echo "❌ Kafka is not running on localhost:9092"
        echo "   Please start Kafka with: docker-compose up -d kafka"
        exit 1
    fi
    echo "✅ Kafka is running"
    
    # Check Redis
    if ! nc -z localhost 6379 2>/dev/null; then
        echo "❌ Redis is not running on localhost:6379"
        echo "   Please start Redis with: docker-compose up -d redis"
        exit 1
    fi
    echo "✅ Redis is running"
    
    # Check MySQL
    if ! nc -z localhost 3306 2>/dev/null; then
        echo "❌ MySQL is not running on localhost:3306"
        echo "   Please start MySQL with: docker-compose up -d mysql"
        exit 1
    fi
    echo "✅ MySQL is running"
}

# Check services first
check_services

echo ""
echo "🧪 Running Fast Test Suite..."
echo "   - No embedded services (using Docker)"
echo "   - Optimized timeouts"
echo "   - Essential tests only"
echo ""

# Clean build
echo "🧽 Quick clean..."
./gradlew clean -q

# Run only the essential tests in parallel
echo "📋 Running Essential Tests:"
echo "   1. Application Context Test"
echo "   2. Redis Service Tests"
echo "   3. User Service Tests"
echo "   4. Kafka Integration Tests"
echo ""

# Run tests with optimized settings
./gradlew test \
    --tests "KafkaStudyApplicationTests" \
    --tests "RedisServiceTest" \
    --tests "UserServiceIntegrationTest" \
    --tests "KafkaIntegrationTest" \
    --parallel \
    --max-workers=2 \
    --continue \
    --console=plain \
    -Dorg.gradle.jvmargs="-Xmx512m -XX:MaxMetaspaceSize=128m" \
    --quiet

echo ""
echo "======================================================"
echo "✅ Fast test suite completed!"
echo "🎯 Performance Benefits:"
echo "   - Using Docker services (no embedded startup)"
echo "   - Parallel execution"
echo "   - Reduced memory usage"
echo "   - Essential tests only" 