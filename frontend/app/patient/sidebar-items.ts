import {
  LayoutDashboard,
  Calendar,
  FileText,
  MessageSquare,
  Bell,
  Settings,
  Pill,
  CreditCard,
  Stethoscope,
} from 'lucide-react';

export const patientSidebarItems = [
  { icon: LayoutDashboard, label: 'Dashboard', href: '/patient/dashboard' },
  { icon: Calendar, label: 'Appointments', href: '/patient/appointments' },
  { icon: Stethoscope, label: 'Doctors', href: '/patient/doctors' },
  { icon: FileText, label: 'Medical History', href: '/patient/medical-history' },
  { icon: Pill, label: 'Prescriptions', href: '/patient/prescriptions' },
  { icon: CreditCard, label: 'Payments', href: '/patient/payments' },
  { icon: MessageSquare, label: 'AI Chatbot', href: '/patient/chatbot' },
  { icon: Bell, label: 'Notifications', href: '/patient/notifications' },
  { icon: Settings, label: 'Profile', href: '/patient/profile' },
];

