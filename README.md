# Boardify 🚀

Event-driven collaborative task manager (Spring Boot service).

## Quick checklist
- [x] Java 17
- [x] Maven (or use the included `mvnw` / `mvnw.cmd`)
- [x] PostgreSQL
- [x] Redis (or a Redis-compatible provider)
- [x] SMTP credentials for outgoing mail 🔒

## What this service is
`boardify-service` is a backend REST API built with Spring Boot. It stores data in PostgreSQL, caches/sessions in Redis, sends email via SMTP, and uses JWT for authentication.

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
└── user/                    # User management
    ├── dto/
    ├── entity/
    └── repository/
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

Example `.env` (do NOT commit secrets):

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/boardify
SPRING_DATASOURCE_USERNAME=boardify_user
SPRING_DATASOURCE_PASSWORD=boardify_pass
SPRING_DATA_REDIS_URL=rediss://:redis_password@redis-host:6379
MAIL_PASSWORD=your_smtp_password
JWT_SECRET=change-me-to-a-secure-random-string
JWT_EXP_MS=7200000
```

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
```bash
# Build the Docker image
docker build -t boardify-service .

# Run the container
docker run --env-file .env -p 8080:8080 boardify-service
```

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
- API Documentation: See API section above
