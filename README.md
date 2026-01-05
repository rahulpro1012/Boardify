# Boardify 

Event-driven collaborative task manager (Spring Boot service).

## Quick checklist
- [x] Java 17
- [x] Maven (or use the included `mvnw` / `mvnw.cmd`)
- [x] PostgreSQL
- [x] Redis (or a Redis-compatible provider)
- [x] SMTP credentials for outgoing mail 🔒
- [x] Kafka (for event-driven messaging)
- [x] Event-driven architecture: service publishes domain events to Kafka

## What this service is
`boardify-service` is a backend REST API built with Spring Boot. It stores data in PostgreSQL, caches/sessions in Redis, sends email via SMTP, and uses JWT for authentication. The service follows an event-driven architecture: it publishes domain events (boards, lists, tasks, comments, membership changes) to Kafka so other services or consumers can react asynchronously (notifications, analytics, projections, etc.).

## Project Structure
```
src/main/java/com/boardify/boardify_service/
├── auth/                     # Authentication and authorization
│   ├── config/              # Security configuration
│   ├── controller/          # Auth endpoints (login, register, etc.)
│   ├── dto/                 # Data transfer objects
│   ├── entity/              # JPA entities (tokens, etc.)
│   ├── exception/           # Custom exceptions
│   ├── jwt/                 # JWT utilities
│   └── service/             # Business logic
├── board/                   # Board management
│   ├── controller/
│   ├── dto/
│   └── entity/
├── comment/                 # Comments on tasks
│   ├── controller/
│   ├── dto/
│   └── entity/
├── list/                    # Board lists (columns)
│   └── entity/
├── task/                    # Tasks
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   └── repository/
├── user/                    # User management
│   ├── dto/
│   ├── entity/
│   └── repository/
├── common/                  # Shared utilities for events, kafka, etc.
│   ├── event/               # Event POJOs (TaskCreatedEvent, etc.)
│   └── kafka/               # Event publisher and topic constants
└── config/                  # App-level configuration (KafkaConfig, RedisConfig, SecurityConfig)
```

## Database Schema

### Users & Authentication
- `users` - User accounts
  - `id` (UUID)
  - `email` (String, unique)
  - `password` (String, hashed)
  - `roles` (Set<Role>)

- `refresh_tokens` - JWT refresh tokens
  - `id` (UUID)
  - `token` (String, unique)
  - `user_id` (FK to users)
  - `expiry_date` (Timestamp)

- `password_reset_tokens` - Password reset tokens
  - `id` (Long)
  - `token` (String, unique)
  - `user_id` (FK to users)
  - `expiry_date` (Timestamp)

### Core Domain
- `boards` - Kanban boards
  - `id` (Long)
  - `name` (String)
  - `created_at` (Timestamp)
  - `created_by_id` (FK to users)

- `board_members` - Many-to-many relationship between users and boards
  - `board_id` (FK to boards)
  - `user_id` (FK to users)

- `board_lists` - Columns in a board (e.g., "To Do", "In Progress")
  - `id` (Long)
  - `board_id` (FK to boards)
  - `name` (String)
  - `position` (Double, for ordering)

- `tasks` - Individual tasks/cards
  - `id` (Long)
  - `list_id` (FK to board_lists)
  - `title` (String)
  - `description` (Text)
  - `position` (Integer, for ordering within list)
  - `assigned_to_id` (FK to users, nullable)
  - `created_by_id` (FK to users)
  - `created_at` (Timestamp)

- `comments` - Comments on tasks
  - `id` (Long)
  - `task_id` (FK to tasks)
  - `author_id` (FK to users)
  - `text` (Text)
  - `created_at` (Timestamp)

## Authentication Flow

### 1. Registration
1. Client sends `POST /auth/register` with email and password
2. Server creates user, generates JWT and refresh token
3. Returns JWT in response body, refresh token in HttpOnly cookie

### 2. Login
1. Client sends `POST /auth/login` with email and password
2. Server validates credentials, generates new JWT and refresh token
3. Returns JWT in response body, refresh token in HttpOnly cookie

### 3. Access Token Refresh
1. Client sends `POST /auth/refresh` with refresh token cookie
2. Server validates refresh token, issues new JWT and refresh token
3. Returns new JWT in response body, new refresh token in HttpOnly cookie

### 4. Password Reset
1. User requests password reset via `POST /auth/forgot-password` with email
2. Server generates reset token, stores it, and sends email with reset link
3. User clicks link, submits new password via `POST /auth/reset-password` with token
4. Server updates password and invalidates all sessions

## API Documentation

Note: this service is event-driven — many write operations (create/update/delete on boards, lists, tasks, comments, membership changes) will publish domain events to Kafka. See the "Kafka Integration" section for topics, event shapes, and how to configure the Kafka bootstrap servers.

### Authentication

#### Register a new user
```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

#### Refresh Token
```http
POST /auth/refresh
Cookie: refreshToken=<refresh-token>
```

#### Forgot Password
```http
POST /auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}
```

#### Reset Password
```http
POST /auth/reset-password
Content-Type: application/json

{
  "token": "reset-token-from-email",
  "newPassword": "newSecurePassword123"
}
```

### Boards

#### Create Board
```http
POST /api/boards
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "name": "Project X"
}
```

#### Get User's Boards
```http
GET /api/boards
Authorization: Bearer <jwt-token>
```

### Tasks

#### Create Task
```http
POST /api/tasks
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "listId": 1,
  "title": "Implement feature X",
  "description": "Add new API endpoints for X"
}
```

#### Move Task
```http
PUT /api/tasks/{taskId}/move
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "newListId": 2,
  "newPosition": 0
}
```

## Configuration
Primary config file: `src/main/resources/application.properties`.
You can provide sensitive values via environment variables. Important properties:

- `spring.datasource.url` - Database URL (e.g., `jdbc:postgresql://localhost:5432/boardify`)
- `spring.datasource.username` - Database username
- `spring.datasource.password` - Database password
- `spring.data.redis.url` - Redis connection URL
- `spring.mail.username` - SMTP email address
- `spring.mail.password` - SMTP password (use env var `MAIL_PASSWORD`)
- `app.jwt.secret` - Secret key for JWT signing
- `app.jwt.exp-ms` - JWT expiration in milliseconds (default: 2 hours)
- `app.frontend.url` - Frontend URL for email links

Important note about Gmail and app passwords:
- If you use Gmail (or Google Workspace) as the SMTP provider for the forgot-password / notification emails, you must use an app password (not your normal Google account password) when the account has 2‑step verification enabled. Generate an app password in your Google Account security settings and place that value into the `MAIL_PASSWORD` environment variable (or `MAIL_APP_PASSWORD` if you prefer a clearer name in your environment). Failure to provide a valid SMTP/app password will cause email sending to fail.

Example `.env` (do NOT commit secrets):

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/boardify
SPRING_DATASOURCE_USERNAME=boardify_user
SPRING_DATASOURCE_PASSWORD=boardify_pass
SPRING_DATA_REDIS_URL=rediss://:redis_password@redis-host:6379
MAIL_USERNAME=your.email@gmail.com
MAIL_PASSWORD=your_smtp_or_app_password_here  # For Gmail: use an App Password generated in your Google Account
# Optional explicit alternative name some teams prefer:
# MAIL_APP_PASSWORD=your_google_app_password_here
JWT_SECRET=change-me-to-a-secure-random-string
JWT_EXP_MS=7200000
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092  # Kafka bootstrap server(s) (comma-separated)
```


## Kafka Integration
This application publishes domain events to Kafka so other services (or consumers) can react to board/list/task/comment changes.

Key points:

- Configuration
  - Property: `spring.kafka.bootstrap-servers` (set via environment variable `SPRING_KAFKA_BOOTSTRAP_SERVERS`) — points to one or more Kafka bootstrap servers, e.g. `localhost:9092` or `broker1:9092,broker2:9092`.
  - If you use a secured Kafka cluster (SASL/SSL), set the required Spring Kafka properties in `application.properties` or via environment variables (SASL mechanism, truststore, keystore, etc.).

- Code locations
  - Kafka producer configuration: `src/main/java/com/boardify/boardify_service/config/KafkaConfig.java` — creates a `KafkaTemplate<String,Object>` using `JsonSerializer` for event values.
  - Event publisher wrapper: `src/main/java/com/boardify/boardify_service/common/kafka/EventPublisher.java` — simple component that wraps `KafkaTemplate` and provides `publish(topic, key, event)`.
  - Topic constants: `src/main/java/com/boardify/boardify_service/common/kafka/Topics.java` — contains the topic names used by the application:
    - `boardify.board.events`
    - `boardify.list.events`
    - `boardify.task.events`
    - `boardify.comment.events`
  - Event POJOs live under `src/main/java/com/boardify/boardify_service/common/event` (e.g. `TaskCreatedEvent`, `TaskUpdatedEvent`, `TaskMovedEvent`, etc.).

- How events are published
  - Services (for example `BoardService`, `BoardListService`, `TaskService`, `CommentService`) create small event objects and call `events.publish(topic, key, event)`.
  - The `KafkaConfig` uses `JsonSerializer` which serializes the event POJO to JSON and includes type headers. Events include a simple `version` field so consumers can handle versioning.
  - Keys are typically constructed to help consumers partition messages (for example `task-created-<id>`).

- Topics and deployment
  - The application assumes those topics exist or that the Kafka broker is configured with `auto.create.topics.enable=true` (not recommended for production). Prefer creating topics with appropriate partitions and retention policies prior to running in production.
  - If you run Kafka locally for development, add `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092` to your `.env` (example added above).

- Docker / Compose
  - If you want Kafka + Zookeeper for local development, add a Kafka service to your `docker-compose.yml` or use a ready-made compose file (for example `confluentinc/cp-kafka` images or `bitnami/kafka`). When running with Docker Compose ensure the service name and port match the `SPRING_KAFKA_BOOTSTRAP_SERVERS` value.

- Resilience
  - The current `EventPublisher` publishes fire-and-forget via `KafkaTemplate.send(...)`. In production you may want to add retries, error handling, or transactional guarantees depending on your delivery requirements.

```markdown
- **Development vs. Docker Configuration**
    - **Running Locally (IntelliJ/Eclipse):** Set `SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092`. This connects your local Java app to the Kafka container's exposed port.
    - **Running in Docker Compose:** Set `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092`. This connects the app container to the Kafka container via the internal Docker network.
    
## Build and Run

### Local Development
```bash
# Build the application
./mvnw clean package -DskipTests

# Run the application
java -jar target/boardify-service-0.0.1-SNAPSHOT.jar

# Or run with Maven
./mvnw spring-boot:run
```

### Docker
This project includes a `Dockerfile` and `docker-compose.yml` to make it easier to run the service and its dependencies (PostgreSQL, Redis, Kafka) with Docker.

**Prerequisite:** Ensure your `.env` file is created in the project root.

#### Option A: Run everything with Docker Compose (Recommended)
This will start the Database, Redis, Kafka, Zookeeper, and the Boardify Service all together.

```bash
# Build the app image and start all services
docker compose -f docker/docker-compose.yml --env-file .env up --build -d

Notes when using Docker / Compose:
- Ensure your `.env` contains the SMTP credentials (e.g. `MAIL_USERNAME` and `MAIL_PASSWORD`) so the app can send password reset emails.
- If you use a managed Redis (or a secure Redis instance), set `SPRING_DATA_REDIS_URL` to the proper `redis://` or `rediss://` URI including credentials.
- If your Docker host can't resolve external hostnames used by hosted Redis providers, ensure network/DNS settings are correct.

### Testing
```bash
# Run all tests
./mvnw test

# Run a specific test
./mvnw test -Dtest=AuthControllerTest
```

## Security Considerations

- All passwords are hashed using BCrypt
- JWT tokens are short-lived (default 2 hours)
- Refresh tokens are stored in HttpOnly cookies with Secure and SameSite=Strict flags
- Password reset tokens expire after 15 minutes
- All API endpoints (except auth endpoints) require authentication
- CORS is configured to allow requests only from the frontend domain

## Troubleshooting

### Common Issues

1. **Database Connection Issues**
   - Verify PostgreSQL is running
   - Check connection URL, username, and password in `application.properties`
   - Ensure the database exists and user has proper permissions

2. **Redis Connection Issues**
   - Verify Redis is running
   - Check Redis URL in configuration
   - Ensure proper authentication if required

3. **Email Sending Issues**
   - Verify SMTP settings in `application.properties`
   - Check email provider's security settings (may require enabling "less secure apps" or generating an app password)
   - Verify port and TLS/SSL settings

4. **JWT Issues**
   - Ensure `app.jwt.secret` is set and consistent across application restarts
   - Check token expiration settings
   - Verify token is being sent in the `Authorization: Bearer` header

## Where to Look Next

- Main application class: `src/main/java/com/boardify/boardify_service/BoardifyApplication.java`
- Security configuration: `src/main/java/com/boardify/boardify_service/config/SecurityConfig.java`
- Database configuration: `src/main/resources/application.properties`
- Kafka configuration: `src/main/java/com/boardify/boardify_service/config/KafkaConfig.java`
- Event publisher and topics: `src/main/java/com/boardify/boardify_service/common/kafka/EventPublisher.java`, `.../Topics.java`
- Event POJOs: `src/main/java/com/boardify/boardify_service/common/event`
- API Documentation: See API section above
