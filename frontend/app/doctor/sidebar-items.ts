import {
  LayoutDashboard,
  Calendar,
  Building2,
  FileText,
  Shield,
  Stethoscope,
  Bell,
} from 'lucide-react';

export const doctorSidebarItems = [
  { icon: LayoutDashboard, label: 'Dashboard', href: '/doctor/dashboard' },
  { icon: Calendar, label: 'Appointments', href: '/doctor/appointments' },
  { icon: Building2, label: 'Clinics', href: '/doctor/clinics' },
  { icon: Stethoscope, label: 'Emergency', href: '/doctor/emergency/new' },
  { icon: FileText, label: 'Prescriptions', href: '/doctor/prescriptions' },
  { icon: Bell, label: 'Notifications', href: '/doctor/notifications' },
  { icon: Shield, label: 'Profile / Settings', href: '/doctor/profile' },
];

