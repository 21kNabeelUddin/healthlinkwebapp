# Deployment Checklist

## Pre-Deployment

### Backend
- [ ] All tests passing
- [ ] Environment variables documented
- [ ] CORS configured for production frontend URL
- [ ] Database migrations ready (Hibernate auto-update or manual)
- [ ] JWT secrets generated (256-bit minimum)
- [ ] Logging configured appropriately
- [ ] Health checks working (`/actuator/health`)

### Frontend
- [ ] Build succeeds (`npm run build`)
- [ ] Environment variables set
- [ ] API URL points to production backend
- [ ] No hardcoded localhost URLs

## Deployment Steps

### 1. Database (Neon)
- [ ] Database created
- [ ] Connection string obtained
- [ ] Test connection works
- [ ] Initial schema created (if not using auto-update)

### 2. Backend Deployment
- [ ] Choose platform (Railway/Render/Fly.io)
- [ ] Connect GitHub repository
- [ ] Set all environment variables:
  - [ ] `SPRING_DATASOURCE_URL`
  - [ ] `SPRING_DATASOURCE_USERNAME`
  - [ ] `SPRING_DATASOURCE_PASSWORD`
  - [ ] `HEALTHLINK_JWT_SECRET`
  - [ ] `HEALTHLINK_SECURITY_JWT_SECRET`
  - [ ] `HEALTHLINK_CORS_ALLOWED_ORIGINS`
  - [ ] Email configuration (if using)
  - [ ] Storage configuration (if using)
- [ ] Deploy backend
- [ ] Verify backend is running
- [ ] Test health endpoint: `https://your-backend.com/actuator/health`
- [ ] Test API endpoint: `https://your-backend.com/api/v1/auth/register`

### 3. Frontend Deployment (Vercel)
- [ ] Push frontend code to GitHub
- [ ] Connect to Vercel
- [ ] Set environment variable: `NEXT_PUBLIC_API_URL`
- [ ] Deploy
- [ ] Verify frontend loads
- [ ] Test signup/login flow

### 4. Post-Deployment Testing
- [ ] User registration works
- [ ] Email OTP received (if configured)
- [ ] Login works
- [ ] JWT tokens stored correctly
- [ ] Protected routes work
- [ ] API calls succeed
- [ ] CORS errors resolved
- [ ] Database operations work
- [ ] File uploads work (if applicable)

### 5. Optional Services
- [ ] Redis configured (if using rate limiting)
- [ ] Elasticsearch configured (if using search)
- [ ] RabbitMQ configured (if using async tasks)
- [ ] Storage service configured (MinIO/S3)

## Security Checklist
- [ ] JWT secrets are strong (256-bit minimum)
- [ ] Database credentials are secure
- [ ] CORS only allows your frontend domain
- [ ] Environment variables not committed to Git
- [ ] HTTPS enabled on all services
- [ ] Rate limiting configured (if using)
- [ ] Error messages don't leak sensitive info

## Monitoring
- [ ] Health checks configured
- [ ] Logging set up
- [ ] Error tracking (optional: Sentry)
- [ ] Uptime monitoring (optional: UptimeRobot)

## Documentation
- [ ] API documentation accessible (Swagger)
- [ ] Deployment process documented
- [ ] Environment variables documented
- [ ] Rollback procedure documented

