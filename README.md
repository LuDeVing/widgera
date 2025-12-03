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
docker compose up --build
```

Open `http://localhost`

## Environment

```env
# Required
GEMINI_API_KEY=your-key
AWS_S3_BUCKET=your-bucket
AWS_ACCESS_KEY=xxx
AWS_SECRET_KEY=xxx
JWT_SECRET=at-least-32-chars

# Optional (defaults work)
AWS_REGION=us-east-1
DB_NAME=widgera
DB_USERNAME=postgres
DB_PASSWORD=postgres
SERVER_PORT=8080
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