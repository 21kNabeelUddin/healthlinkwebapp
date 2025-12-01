# Quick Deployment Guide

## TL;DR - Recommended Stack

- **Frontend**: Vercel (Next.js) ✅
- **Backend**: Railway or Render (Spring Boot)
- **Database**: Neon (PostgreSQL) ✅

## Quick Start (Railway)

### 1. Deploy Backend to Railway

1. Go to [railway.app](https://railway.app)
2. Sign up/login with GitHub
3. Click "New Project" → "Deploy from GitHub"
4. Select your `healthlink_backend` repository
5. Railway will auto-detect Spring Boot

### 2. Set Environment Variables

In Railway dashboard, add these variables:

```bash
# Database (from Neon dashboard)
SPRING_DATASOURCE_URL=jdbc:postgresql://your-neon-host:5432/healthlink?sslmode=require
SPRING_DATASOURCE_USERNAME=your-neon-user
SPRING_DATASOURCE_PASSWORD=your-neon-password

# JWT Secrets (generate strong random strings, 256-bit minimum)
HEALTHLINK_JWT_SECRET=your-very-long-random-secret-key-here-minimum-256-bits
HEALTHLINK_SECURITY_JWT_SECRET=your-very-long-random-secret-key-here-minimum-256-bits

# CORS (will update after frontend deployment)
HEALTHLINK_CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
```

**Generate JWT Secrets:**
```bash
# On Linux/Mac:
openssl rand -base64 32

# Or use online generator:
# https://generate-secret.vercel.app/32
```

### 3. Deploy Frontend to Vercel

1. Go to [vercel.com](https://vercel.com)
2. Sign up/login with GitHub
3. Click "Add New Project"
4. Import your frontend repository
5. Set environment variable:
   ```
   NEXT_PUBLIC_API_URL=https://your-railway-app.up.railway.app
   ```
6. Deploy!

### 4. Update CORS

After frontend is deployed, update backend CORS:
- Go to Railway dashboard
- Update `HEALTHLINK_CORS_ALLOWED_ORIGINS`:
  ```
  https://your-app.vercel.app
  ```
- Redeploy backend

## Quick Start (Render)

### 1. Deploy Backend to Render

1. Go to [render.com](https://render.com)
2. Sign up/login with GitHub
3. Click "New" → "Web Service"
4. Connect your `healthlink_backend` repository
5. Settings:
   - **Name**: `healthlink-backend`
   - **Environment**: `Java`
   - **Build Command**: `./gradlew build -x test`
   - **Start Command**: `java -jar build/libs/*.jar`
6. Add environment variables (same as Railway above)
7. Deploy!

### 2. Deploy Frontend to Vercel

Same as Railway steps above, but use Render URL:
```
NEXT_PUBLIC_API_URL=https://healthlink-backend.onrender.com
```

## Cost Comparison

| Service | Free Tier | Paid Tier |
|---------|-----------|-----------|
| **Vercel** | ✅ Yes (generous) | $20/month |
| **Railway** | ❌ No | $5-10/month |
| **Render** | ✅ Yes (sleeps) | $7/month |
| **Neon** | ✅ Yes | $0-10/month |

**Starting Cost**: ~$5-10/month (Railway) or Free (Render free tier)

## Environment Variables Cheat Sheet

### Backend (Railway/Render)
```bash
# Required
SPRING_DATASOURCE_URL=...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
HEALTHLINK_JWT_SECRET=...
HEALTHLINK_SECURITY_JWT_SECRET=...
HEALTHLINK_CORS_ALLOWED_ORIGINS=https://your-app.vercel.app

# Optional (for production features)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
RATE_LIMIT_ENABLED=false  # Set true if you add Redis
ELASTICSEARCH_ENABLED=false  # Set true if you add Elasticsearch
```

### Frontend (Vercel)
```bash
NEXT_PUBLIC_API_URL=https://your-backend-url.com
```

## Testing After Deployment

1. **Backend Health Check**:
   ```
   https://your-backend-url.com/actuator/health
   ```

2. **Test Registration**:
   - Go to your Vercel frontend
   - Try signing up a new user
   - Check if OTP email arrives (if email configured)

3. **Test Login**:
   - Login with created user
   - Verify JWT token is stored
   - Check protected routes work

## Troubleshooting

### CORS Errors
- Make sure `HEALTHLINK_CORS_ALLOWED_ORIGINS` includes your Vercel URL
- No trailing slash in the URL
- Include `https://` protocol

### Database Connection Errors
- Check Neon connection string format
- Ensure `?sslmode=require` is in the URL
- Verify credentials are correct

### Build Failures
- Check build logs in Railway/Render
- Ensure `build.gradle` is correct
- Verify Java version (should be 21)

## Next Steps

1. ✅ Deploy backend
2. ✅ Deploy frontend
3. ✅ Test everything
4. 🔄 Add optional services as needed (Redis, Elasticsearch)
5. 🔄 Set up custom domain
6. 🔄 Configure monitoring

Need help? Check `DEPLOYMENT_GUIDE.md` for detailed instructions!

