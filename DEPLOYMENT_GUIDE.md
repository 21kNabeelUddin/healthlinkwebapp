# HealthLink+ Deployment Guide

## Architecture Overview

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Vercel    │─────▶│   Backend    │─────▶│    Neon     │
│  (Frontend) │      │  (Spring Boot)│      │ (PostgreSQL)│
│   Next.js   │      │              │      │             │
└─────────────┘      └─────────────┘      └─────────────┘
                            │
                            ├──▶ Redis (Optional - Rate Limiting)
                            ├──▶ Elasticsearch (Optional - Search)
                            └──▶ RabbitMQ (Optional - Async Tasks)
```

## Frontend: Vercel ✅

**Why Vercel:**
- Perfect for Next.js (made by the Next.js team)
- Automatic deployments from Git
- Edge network for fast global performance
- Free tier with generous limits
- Built-in SSL certificates
- Environment variable management

**Deployment Steps:**
1. Push your frontend code to GitHub
2. Connect your repo to Vercel
3. Set environment variable: `NEXT_PUBLIC_API_URL=https://your-backend-url.com`
4. Deploy!

## Backend: Recommended Options

### 🥇 **Option 1: Render (Recommended for Start)**

**Why Render:**
- ✅ Easy setup, similar to Heroku
- ✅ Free tier available (with limitations)
- ✅ Automatic deployments from Git
- ✅ Built-in PostgreSQL support (but you can use Neon)
- ✅ Supports all Spring Boot features
- ✅ Good documentation
- ✅ Auto-scaling available

**Pricing:**
- Free tier: Limited hours/month, sleeps after inactivity
- Starter: $7/month - Always on, 512MB RAM
- Standard: $25/month - 2GB RAM, better performance

**Setup:**
1. Connect GitHub repo
2. Select "Web Service"
3. Build command: `./gradlew build`
4. Start command: `./gradlew bootRun` or `java -jar build/libs/*.jar`
5. Set environment variables (see below)

**Pros:**
- Very developer-friendly
- Good free tier to start
- Easy to upgrade

**Cons:**
- Free tier has limitations
- Can be slower than dedicated servers

---

### 🥈 **Option 2: Railway**

**Why Railway:**
- ✅ Excellent developer experience
- ✅ Automatic deployments
- ✅ Great for Spring Boot
- ✅ Easy environment variable management
- ✅ Good performance

**Pricing:**
- Pay-as-you-go: $5/month + usage
- $20/month credit included
- Very cost-effective

**Setup:**
1. Connect GitHub repo
2. Railway auto-detects Spring Boot
3. Set environment variables
4. Deploy!

**Pros:**
- Great UX
- Fair pricing
- Fast deployments

**Cons:**
- Newer platform (but very stable)

---

### 🥉 **Option 3: Fly.io**

**Why Fly.io:**
- ✅ Global edge deployment
- ✅ Excellent performance
- ✅ Good for Spring Boot
- ✅ Docker-based (flexible)

**Pricing:**
- Free tier: 3 shared VMs
- Paid: $1.94/month per VM + usage

**Setup:**
1. Install Fly CLI
2. Run `fly launch`
3. Configure `fly.toml`
4. Deploy with `fly deploy`

**Pros:**
- Global edge network
- Good performance
- Flexible

**Cons:**
- Requires Docker knowledge
- More setup steps

---

### 🏢 **Option 4: AWS/GCP/Azure (For Scale)**

**AWS Options:**
- **Elastic Beanstalk**: Easiest AWS option for Spring Boot
- **ECS/Fargate**: Container-based, more control
- **EC2**: Full control, more setup

**GCP Options:**
- **Cloud Run**: Serverless, pay-per-use, excellent for Spring Boot
- **App Engine**: Managed platform
- **Compute Engine**: VMs with full control

**Azure Options:**
- **App Service**: Managed Spring Boot hosting
- **Container Instances**: Docker-based

**When to Use:**
- You need enterprise features
- You have AWS/GCP credits
- You need specific compliance requirements
- You're scaling to thousands of users

---

## Recommended Setup for Your Project

### **Best for Starting Out: Render or Railway**

Both are excellent choices. I'd recommend **Railway** if you want the best developer experience, or **Render** if you want a free tier to test with.

### **Environment Variables to Set**

#### Backend Environment Variables:

```bash
# Database (Neon PostgreSQL)
SPRING_DATASOURCE_URL=jdbc:postgresql://your-neon-host:5432/healthlink?sslmode=require
SPRING_DATASOURCE_USERNAME=your-neon-user
SPRING_DATASOURCE_PASSWORD=your-neon-password

# JWT Secrets
HEALTHLINK_JWT_SECRET=your-256-bit-secret-key-here-minimum-length-required
HEALTHLINK_SECURITY_JWT_SECRET=your-256-bit-secret-key-here-minimum-length-required

# CORS (Add your Vercel frontend URL)
HEALTHLINK_CORS_ALLOWED_ORIGINS=https://your-app.vercel.app,https://your-custom-domain.com

# Optional Services (if you enable them)
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

ELASTICSEARCH_ENABLED=false  # Set to true if you set up Elasticsearch
ELASTICSEARCH_URIS=http://your-elasticsearch-host:9200

# Email (for OTP and notifications)
MAIL_HOST=smtp.gmail.com  # Or your SMTP provider
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Storage (MinIO or S3)
HEALTHLINK_STORAGE_ENDPOINT=https://your-storage-endpoint
HEALTHLINK_STORAGE_BUCKET=healthlink-records
HEALTHLINK_STORAGE_ACCESS_KEY=your-access-key
HEALTHLINK_STORAGE_SECRET_KEY=your-secret-key

# Rate Limiting (optional)
RATE_LIMIT_ENABLED=false  # Set to true if you set up Redis
```

#### Frontend Environment Variables (Vercel):

```bash
NEXT_PUBLIC_API_URL=https://your-backend-url.com
```

---

## Step-by-Step Deployment

### 1. Deploy Backend to Railway (Example)

```bash
# 1. Push your code to GitHub
git add .
git commit -m "Ready for deployment"
git push origin main

# 2. Go to railway.app
# 3. Click "New Project" → "Deploy from GitHub"
# 4. Select your healthlink_backend repository
# 5. Railway will auto-detect Spring Boot
# 6. Add environment variables (see above)
# 7. Deploy!
```

### 2. Deploy Frontend to Vercel

```bash
# 1. Push frontend code to GitHub (separate repo or monorepo)
# 2. Go to vercel.com
# 3. Click "Add New Project"
# 4. Import your frontend repository
# 5. Set environment variable:
#    NEXT_PUBLIC_API_URL=https://your-railway-backend-url.up.railway.app
# 6. Deploy!
```

### 3. Update CORS in Backend

Update `application.yml` or set environment variable:
```yaml
healthlink:
  cors:
    allowed-origins: ${HEALTHLINK_CORS_ALLOWED_ORIGINS:https://your-app.vercel.app}
```

---

## Optional Services Setup

### Redis (For Rate Limiting)

**Options:**
- **Upstash**: Serverless Redis, free tier available
- **Redis Cloud**: Managed Redis
- **Railway Redis**: If using Railway, they offer Redis addon

### Elasticsearch (For Doctor Search)

**Options:**
- **Elastic Cloud**: Managed Elasticsearch (free trial)
- **Bonsai**: Managed Elasticsearch
- **Self-hosted**: On a VPS (more complex)

### RabbitMQ (For Async Tasks)

**Options:**
- **CloudAMQP**: Managed RabbitMQ (free tier)
- **Self-hosted**: On a VPS

**Note:** All these are optional. Your app will work without them (with reduced features).

---

## Cost Estimate (Starting Out)

### Minimal Setup (Free/Cheap):
- **Frontend (Vercel)**: Free
- **Backend (Railway)**: $5-10/month
- **Database (Neon)**: Free tier available
- **Total**: ~$5-10/month

### With Optional Services:
- **Frontend (Vercel)**: Free
- **Backend (Railway)**: $10-20/month
- **Database (Neon)**: $0-10/month
- **Redis (Upstash)**: Free tier
- **Elasticsearch**: $0-15/month (if needed)
- **Total**: ~$10-45/month

---

## My Recommendation

**For your project, I recommend:**

1. **Frontend**: Vercel ✅ (Perfect for Next.js)
2. **Backend**: **Railway** or **Render** (Both excellent, Railway has better UX)
3. **Database**: Neon ✅ (You're already using it)
4. **Optional Services**: Add as needed (start without them)

**Why this stack:**
- ✅ Easy to set up and maintain
- ✅ Cost-effective for starting out
- ✅ Scales well when you grow
- ✅ Great developer experience
- ✅ All services have good free/low-cost tiers

---

## Next Steps

1. **Choose your backend platform** (Railway or Render recommended)
2. **Set up your Neon database** (you already have this)
3. **Deploy backend** with environment variables
4. **Deploy frontend to Vercel** with `NEXT_PUBLIC_API_URL` pointing to your backend
5. **Test everything** and iterate!

Need help with any specific platform setup? Let me know!

