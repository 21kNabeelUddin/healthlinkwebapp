import {
  LayoutDashboard,
  Calendar,
  FileText,
  MessageSquare,
  Bell,
  Settings,
  Pill,
  CreditCard,
} from 'lucide-react';

export const patientSidebarItems = [
  { icon: LayoutDashboard, label: 'Dashboard', href: '/patient/dashboard' },
  { icon: Calendar, label: 'Appointments', href: '/patient/appointments' },
  { icon: FileText, label: 'Medical History', href: '/patient/medical-history' },
  { icon: Pill, label: 'Prescriptions', href: '/patient/prescriptions' },
  { icon: CreditCard, label: 'Payments', href: '/patient/payments' },
  { icon: MessageSquare, label: 'AI Chatbot', href: '/patient/chatbot' },
  { icon: Bell, label: 'Notifications', href: '/patient/notifications' },
  { icon: Settings, label: 'Profile', href: '/patient/profile' },
];

