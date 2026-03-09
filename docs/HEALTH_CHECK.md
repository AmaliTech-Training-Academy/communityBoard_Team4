# Health Check Endpoint

## Overview
The health check endpoint provides a way to monitor the application's status and dependencies.

## Implementation

### 1. Add Spring Boot Actuator Dependency
Add to `backend/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2. Configure Health Check
Add to `backend/src/main/resources/application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
```

### 3. Custom Health Indicator (Optional)
Create `backend/src/main/java/com/amalitech/communityboard/health/DatabaseHealthIndicator.java`:
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            return Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("status", "Connected")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## Endpoints
- `GET /actuator/health` - Basic health status
- `GET /actuator/info` - Application information

## Response Format
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "status": "Connected"
      }
    }
  }
}
```

## Docker Compose Health Check
Add to `docker-compose.yml`:
```yaml
backend:
  # ... existing config
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 40s
```
