# HealthLink+ - Healthcare Management Platform

A premium, modern healthcare platform connecting patients, doctors, and admins with unified appointment management, telemedicine, and medical history tools.

## 🚀 Routes

The application includes multiple pages accessible via different routes:

- **`/`** - Landing Page (Marketing site with hero, features, testimonials)
- **`/patient`** - Patient Dashboard (Book appointments, view medical history, join video consultations)
- **`/doctor`** - Doctor Dashboard (Manage appointments, clinics, and patients)
- **`/admin`** - Admin Dashboard (Platform overview, analytics, user management)

## 🎨 Design System

### Colors
- **Primary Gradient**: Teal (500) → Violet (600)
- **Backgrounds**: Powder blue (50), White, Slate (50)
- **Accents**: Deep navy, Teal, Violet, Purple
- **Text**: Slate (900, 700, 600)

### Typography
- Uses system fonts (Inter/SF Pro style)
- Hierarchical sizing with semantic HTML elements
- Weight contrast for visual hierarchy

### Components
- **Glassmorphism cards** with backdrop blur and subtle shadows
- **Gradient buttons** with hover effects
- **Stats cards** with icons and trend indicators
- **Appointment cards** with action buttons
- **Responsive sidebar** navigation
- **Top navigation** with user profile dropdown

## 🏥 Features by Role

### Patient Portal
- View upcoming appointments
- Join Zoom video consultations
- Access medical history timeline
- Book new appointments
- Receive notifications and reminders

### Doctor Portal
- Manage appointment queue
- Confirm/reject appointments
- Manage multiple clinic locations
- View patient records
- Track performance metrics
- Task management

### Admin Portal
- Platform analytics and metrics
- User management (patients, doctors, admins)
- Appointment trends visualization
- System alerts monitoring
- Security & compliance dashboard
- Role distribution insights

## 🔒 Security Features

- HIPAA-ready compliance
- Real-time OTP verification
- Secure Zoom integration
- Data encryption
- Audit logs
- Two-factor authentication

## 📱 Responsive Design

The application is fully responsive with breakpoints for:
- Mobile (375px)
- Tablet (768px)
- Desktop (1440px)

## 🛠️ Tech Stack

- React + TypeScript
- Tailwind CSS v4.0
- Lucide React (icons)
- Shadcn/ui components

## 🎯 Navigation

To navigate between dashboards, you can:
1. Click the portal links in the top navigation
2. Directly visit the routes: `/patient`, `/doctor`, `/admin`
3. Click "Explore HealthLink+" button on landing page (goes to patient dashboard)

## 📦 Components Structure

```
/components
  ├── layout/
  │   ├── TopNav.tsx (Sticky navigation with user menu)
  │   └── Sidebar.tsx (Collapsible sidebar navigation)
  ├── dashboard/
  │   ├── StatsCard.tsx (Metric cards with trends)
  │   └── AppointmentCard.tsx (Appointment display/actions)
  ├── ui/ (Shadcn components)
  └── [Landing page components]

/pages
  ├── LandingPage.tsx
  ├── PatientDashboard.tsx
  ├── DoctorDashboard.tsx
  └── AdminDashboard.tsx
```

## 🎨 Design Highlights

- **Premium feel**: High-quality gradients, shadows, and spacing
- **Glassmorphism**: Frosted glass effects on cards
- **Micro-interactions**: Hover states, transitions, animations
- **Healthcare-focused**: Medical icons, clinical color palette
- **Accessible**: Semantic HTML, proper contrast ratios
- **Trust indicators**: HIPAA badges, security notices throughout

---

Built with ❤️ for modern healthcare teams
