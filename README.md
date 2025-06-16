# Kafka Study - Spring Boot with Kotlin

A comprehensive Spring Boot application demonstrating integration with Kafka, MySQL, Redis, and MyBatis using Kotlin.

## Features

- **Spring Boot 3.2.0** with **Kotlin**
- **Apache Kafka** for message streaming (Producer & Consumer)
- **MySQL** database with **MyBatis** for data persistence
- **Redis** for caching with cache-first strategy
- **REST API** endpoints for CRUD operations
- **Comprehensive test suite** with embedded Kafka and H2 for testing
- **Docker Compose** setup for easy development environment

## Architecture

The application implements a **cache-first pattern**:
1. **Redis Cache**: First attempts to retrieve data from Redis cache
2. **MySQL Database**: Falls back to MySQL if data not found in cache
3. **Kafka Events**: Publishes events for all user operations

## Prerequisites

- **Java 17** or higher
- **Docker** and **Docker Compose**
- **Gradle** (wrapper included)

## Quick Start

### 1. Start Infrastructure Services

```bash
# Start Kafka, MySQL, and Redis
docker-compose up -d

# Verify services are running
docker-compose ps
```

### 2. Run the Application

```bash
# Build and run the application
./gradlew bootRun

# Or build jar and run
./gradlew build
java -jar build/libs/kafka-study-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

### 3. Test the APIs

#### User Management APIs

**Create a User:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "name": "Test User",
    "age": 25
  }'
```

**Get User by ID (demonstrates cache-first pattern):**
```bash
curl http://localhost:8080/api/users/1
```

**Get User by Email:**
```bash
curl http://localhost:8080/api/users/email/test@example.com
```

**Get All Users:**
```bash
curl http://localhost:8080/api/users
```

**Update User:**
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "name": "Updated User",
    "age": 30
  }'
```

**Delete User:**
```bash
curl -X DELETE http://localhost:8080/api/users/1
```

#### Kafka Testing APIs

**Send User Event:**
```bash
curl -X POST http://localhost:8080/api/kafka/send-user-event \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-event-1",
    "message": "Test user event message"
  }'
```

**Send Notification:**
```bash
curl -X POST "http://localhost:8080/api/kafka/send-notification?message=Test notification"
```

**Get Processed Messages:**
```bash
curl http://localhost:8080/api/kafka/processed-messages
```

**Test Complete Kafka Flow:**
```bash
curl -X POST "http://localhost:8080/api/kafka/test-kafka-flow?message=Complete flow test"
```

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test Classes

```bash
# Kafka integration tests
./gradlew test --tests "KafkaIntegrationTest"

# Redis service tests
./gradlew test --tests "RedisServiceTest"

# User service integration tests
./gradlew test --tests "UserServiceIntegrationTest"
```

## Project Structure

```
src/
├── main/
│   ├── kotlin/com/example/kafkastudy/
│   │   ├── config/           # Configuration classes
│   │   ├── controller/       # REST Controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── entity/          # JPA Entities
│   │   ├── kafka/           # Kafka Producer & Consumer
│   │   ├── mapper/          # MyBatis Mappers
│   │   └── service/         # Business Logic Services
│   └── resources/
│       ├── mapper/          # MyBatis XML Mappers
│       ├── application.yml  # Application Configuration
│       └── schema.sql       # Database Schema
└── test/
    └── kotlin/com/example/kafkastudy/
        ├── kafka/           # Kafka Tests
        └── service/         # Service Tests
```

## Configuration

### Application Properties

Key configurations in `src/main/resources/application.yml`:

- **Server Port**: 8080
- **Kafka Bootstrap Servers**: localhost:9092
- **MySQL Database**: kafka_study_db
- **Redis**: localhost:6379

### Environment Variables

You can override default configurations using environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/your_db
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password
export SPRING_DATA_REDIS_HOST=your_redis_host
export SPRING_KAFKA_BOOTSTRAP_SERVERS=your_kafka_servers
```

## Key Components

### 1. UserService - Cache-First Pattern

The `UserService` implements a cache-first pattern:
- Checks Redis cache first
- Falls back to MySQL database if not found
- Updates cache after database queries
- Publishes Kafka events for all operations

### 2. Kafka Integration

- **Producer**: Sends user events and notifications
- **Consumer**: Processes messages with manual acknowledgment
- **Topics**: `user-events` and `notifications`

### 3. MyBatis Integration

- Annotation-based mappers for simple queries
- XML-based mappers for complex queries
- Support for both approaches demonstrated

### 4. Redis Caching

- String-based serialization for simple operations
- JSON serialization for complex objects
- Configurable TTL (30 minutes default)

## Development

### Adding New Features

1. **New Entity**: Add to `entity/` package and create corresponding mapper
2. **New API**: Add controller to `controller/` package
3. **New Service**: Add business logic to `service/` package
4. **Kafka Integration**: Use existing producers/consumers or extend them

### Database Changes

1. Update `schema.sql` for production changes
2. Add migration scripts if needed
3. Update entity classes and mappers

## Monitoring

The application includes Spring Boot Actuator for monitoring:

- **Health Check**: `http://localhost:8080/actuator/health`
- **Info**: `http://localhost:8080/actuator/info`
- **Metrics**: `http://localhost:8080/actuator/metrics`

## Troubleshooting

### Common Issues

1. **Kafka Connection Issues**:
   ```bash
   # Check if Kafka is running
   docker-compose logs kafka
   ```

2. **MySQL Connection Issues**:
   ```bash
   # Check MySQL logs
   docker-compose logs mysql
   ```

3. **Redis Connection Issues**:
   ```bash
   # Check Redis logs
   docker-compose logs redis
   ```

4. **Application Won't Start**:
   - Ensure all services are running: `docker-compose ps`
   - Check application logs for specific errors
   - Verify Java 17+ is installed

### Logs

Application logs are configured to show:
- **DEBUG** level for application packages
- **INFO** level for Spring Kafka
- All SQL queries (for development)

## Performance Considerations

- **Redis Caching**: Significantly reduces database load
- **Kafka Async Processing**: Non-blocking message publishing
- **Connection Pooling**: Configured for MySQL and Redis
- **MyBatis**: Efficient SQL mapping with caching

## Security Notes

This is a development/study project with basic security:
- No authentication/authorization implemented
- Default passwords in docker-compose
- Not production-ready without security enhancements

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is for educational purposes and is available under the MIT License. 