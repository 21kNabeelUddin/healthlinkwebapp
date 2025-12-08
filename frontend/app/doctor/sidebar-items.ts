import {
  LayoutDashboard,
  Calendar,
  Building2,
  FileText,
  Activity,
  CreditCard,
  Shield,
  Stethoscope,
} from 'lucide-react';

export const doctorSidebarItems = [
  { icon: LayoutDashboard, label: 'Dashboard', href: '/doctor/dashboard' },
  { icon: Calendar, label: 'Appointments', href: '/doctor/appointments' },
  { icon: Building2, label: 'Clinics', href: '/doctor/clinics' },
  { icon: Stethoscope, label: 'Emergency', href: '/doctor/emergency/new' },
  { icon: FileText, label: 'Prescriptions', href: '/doctor/prescriptions' },
  { icon: Activity, label: 'Analytics', href: '/doctor/analytics' },
  { icon: CreditCard, label: 'Payments', href: '/doctor/payments' },
  { icon: Shield, label: 'Profile', href: '/doctor/profile' },
];

