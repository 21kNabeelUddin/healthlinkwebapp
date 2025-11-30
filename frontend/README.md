# HealthLink+ Frontend

A modern Next.js frontend application for the HealthLink+ healthcare management system.

## Features

- **Patient Portal**
  - Sign up, login, and OTP verification
  - Browse and search doctors by specialization
  - Book online (Zoom) or onsite appointments
  - Manage medical history (CRUD operations)
  - View and manage appointments
  - Access Zoom meeting links for online appointments

- **Doctor Portal**
  - Sign up, login, and OTP verification
  - Manage clinics (create, update, delete, activate/deactivate)
  - View and manage appointments
  - Confirm, reject, or complete appointments
  - Access Zoom meeting links for online appointments

- **Admin Portal**
  - Login and dashboard access
  - System administration features

## Tech Stack

- **Next.js 14** - React framework with App Router
- **TypeScript** - Type-safe development
- **Tailwind CSS** - Utility-first CSS framework
- **React Hook Form** - Form management
- **Axios** - HTTP client for API calls
- **React Hot Toast** - Toast notifications
- **date-fns** - Date formatting utilities

## Getting Started

### Prerequisites

- Node.js 18+ and npm/yarn
- Backend API running on `http://localhost:8080`

### Installation

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Create a `.env.local` file (optional, defaults are set):
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

4. Run the development server:
```bash
npm run dev
```

5. Open [http://localhost:3000](http://localhost:3000) in your browser

## Project Structure

```
frontend/
├── app/                    # Next.js App Router pages
│   ├── auth/              # Authentication pages
│   │   ├── patient/       # Patient auth (signup, login, verify-otp)
│   │   ├── doctor/        # Doctor auth (signup, login, verify-otp)
│   │   └── admin/         # Admin auth (login)
│   ├── patient/           # Patient dashboard and pages
│   ├── doctor/            # Doctor dashboard and pages
│   └── admin/             # Admin dashboard
├── components/            # Reusable React components
│   ├── ui/               # UI components (Button, Input, Card)
│   └── layout/           # Layout components (Navbar, DashboardLayout)
├── contexts/             # React contexts (AuthContext)
├── lib/                  # Utilities and API client
│   ├── api.ts           # API service functions
│   └── auth.ts          # Authentication utilities
├── types/               # TypeScript type definitions
└── public/              # Static assets
```

## API Integration

The frontend communicates with the backend API at `http://localhost:8080`. All API calls are handled through the `lib/api.ts` file which exports:

- `patientApi` - Patient-related API calls
- `doctorApi` - Doctor-related API calls
- `adminApi` - Admin-related API calls

## Authentication

Authentication is handled through:
- JWT tokens stored in localStorage
- User data stored in localStorage
- Protected routes using `DashboardLayout` component
- Automatic token injection in API requests

## Environment Variables

- `NEXT_PUBLIC_API_URL` - Backend API URL (default: `http://localhost:8080`)

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run start` - Start production server
- `npm run lint` - Run ESLint

## Building for Production

```bash
npm run build
npm run start
```

## Notes

- The frontend assumes the backend is running on `http://localhost:8080`
- CORS must be properly configured on the backend
- All API responses follow the `ApiResponse<T>` format
- Error handling is done through toast notifications
- Forms use React Hook Form for validation

## Support

For API documentation, refer to `HealthLink_API_Documentation.txt` in the project root.

