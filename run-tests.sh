#!/bin/bash

# Kafka Study - Fast Test Runner
# Optimized for speed and reliability

set -e

echo "🚀 Starting Fast Kafka Study Test Suite..."
echo "============================================="

# Function to cleanup on exit
cleanup() {
    echo "🧹 Cleaning up test processes..."
    # Kill any test processes that might be hanging
    pkill -f "gradle.*test" || true
    pkill -f "kafka.*test" || true
    pkill -f "redis.*test" || true
    echo "✅ Cleanup completed"
}

# Set trap to run cleanup on script exit
trap cleanup EXIT

# Check if Java 17+ is available
echo "☕ Checking Java version..."
java -version | head -1

# Clean previous builds quickly
echo "🧽 Quick clean..."
./gradlew clean -q

# Run tests in parallel with optimized settings
echo "🧪 Running optimized test suite..."
echo "   - Parallel execution enabled"
echo "   - Reduced timeouts"
echo "   - Lightweight configurations"
echo "   - No embedded services overhead"

# Test categories for better organization
echo ""
echo "📋 Test Execution Plan:"
echo "   1. Application context tests"
echo "   2. Redis service tests" 
echo "   3. User service tests"
echo "   4. Kafka integration tests"
echo ""

# Run tests with optimized gradle settings
./gradlew test \
    --parallel \
    --max-workers=4 \
    --continue \
    --build-cache \
    --configuration-cache \
    -Dorg.gradle.jvmargs="-Xmx1g -XX:MaxMetaspaceSize=256m" \
    -Dspring.test.constructor.autowire.mode=all \
    -q

echo ""
echo "============================================="
echo "✅ Fast test suite completed!"
echo "📊 Performance Optimizations Applied:"
echo "   - Removed DirtiesContext overhead"
echo "   - Reduced timeouts and waits"
echo "   - Lightweight test configurations"
echo "   - Parallel test execution"
echo "   - Minimal cleanup strategies" 