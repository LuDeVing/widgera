# Widgera

Structured output from LLMs. Define fields, get JSON back.

## What it does

1. You write a prompt
2. You define what fields you want (name, type)
3. Optionally attach an image
4. Gemini returns structured JSON matching your schema

## Run it

```bash
cp .env.example .env
# fill in your keys
docker compose up --build -d
```

Open `http://localhost`

## Environment

```env
# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=widgera
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Server
SERVER_PORT=8080

# JWT
JWT_SECRET=secret-jwt-key
JWT_EXPIRATION=86400000

# Google Gemini API
GEMINI_API_KEY=gemini-api-key

# AWS S3
AWS_S3_BUCKET=bucket-name
AWS_REGION=us-east-1
AWS_ACCESS_KEY=aws-access-key
AWS_SECRET_KEY=aws-secret-key
```

## API

All endpoints except auth require `Authorization: Bearer <token>` header.

### Auth
```
POST /api/auth/register  { username, password }
POST /api/auth/login     { username, password } -> { token, username }
```

### Images
```
POST /api/images/upload  multipart/form-data (file) -> { imageId, imageUrl }
GET  /api/images/{id}/url -> presigned S3 URL (1 hour)
```

### Prompts
```
POST /api/prompt
{
  "prompt": "Who is this?",
  "fields": [
    { "name": "fullName", "type": "string" },
    { "name": "age", "type": "number" }
  ],
  "imageId": 1  // optional
}

GET /api/prompt/history
```

## Architecture decisions

**Why separate image upload?** Images go to S3 first, then you reference them by ID. This prevents re-uploading the same image (we hash and dedupe), and keeps the prompt endpoint simple.

**Why presigned URLs?** S3 bucket is private. Backend generates time-limited URLs so images work in browser without exposing credentials.

**Why Redis cache on history?** History data (prompts, fields, outputs, raw S3 URLs) is cached in Redis with no TTL - only evicted when new history is added. Presigned URLs are generated fresh on each request at the controller layer.

**Why hash images?** SHA-256 of file content. Same image = same hash = no duplicate upload for that user.

## Project structure

```
backend/
  src/main/java/com/widgera/
    controller/   # REST endpoints
    service/      # Business logic (Gemini, S3, prompts)
    entity/       # User, UserImage, PromptHistory
    config/       # Security, AWS, Redis

frontend/
  src/
    pages/        # Login, Register, Dashboard, History
    components/   # ImageUpload, FieldBuilder, PrivateRoute
    services/     # Axios API client
    context/      # Auth state
```
