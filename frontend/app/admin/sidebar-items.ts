import { LayoutDashboard, Users, Stethoscope, Building2, Calendar, Bell, Activity, Shield, Settings } from 'lucide-react';
import type { SidebarItem } from '@/marketing/layout/Sidebar';

export const adminSidebarItems: SidebarItem[] = [
  { icon: LayoutDashboard, label: 'Dashboard', href: '/admin/dashboard' },
  { icon: Users, label: 'Users', href: '/admin/users' },
  { icon: Users, label: 'Patients', href: '/admin/patients' },
  { icon: Stethoscope, label: 'Doctors', href: '/admin/doctors' },
  { icon: Building2, label: 'Clinics', href: '/admin/clinics' },
  { icon: Calendar, label: 'Appointments', href: '/admin/appointments' },
  { icon: Bell, label: 'Notifications', href: '/admin/notifications/history' },
  { icon: Activity, label: 'Analytics', href: '/admin/analytics' },
  { icon: Shield, label: 'Audit & Compliance', href: '/admin/audit' },
  { icon: Settings, label: 'Settings', href: '/admin/settings' },
];

