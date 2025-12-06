'use client';

import { useEffect, useMemo, useState } from 'react';
import { format, formatDistanceToNow } from 'date-fns';
import { toast } from 'react-hot-toast';
import {
  Bell,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Calendar as CalendarIcon,
  FileText,
} from 'lucide-react';

import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import { patientApi } from '@/lib/api';
import { Appointment, MedicalHistory } from '@/types';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { patientSidebarItems } from '@/app/patient/sidebar-items';
import { Button } from '@/marketing/ui/button';

type NotificationVariant = 'success' | 'info' | 'warning' | 'danger';

interface NotificationItem {
  id: string;
  title: string;
  description: string;
  timestamp: string;
  variant: NotificationVariant;
  action?: {
    label: string;
    href: string;
  };
}

const variantStyles: Record<NotificationVariant, string> = {
  success: 'bg-emerald-50 border-emerald-200 text-emerald-900',
  info: 'bg-sky-50 border-sky-200 text-sky-900',
  warning: 'bg-amber-50 border-amber-200 text-amber-900',
  danger: 'bg-rose-50 border-rose-200 text-rose-900',
};

const variantIcon: Record<NotificationVariant, typeof Bell> = {
  success: CheckCircle2,
  info: Bell,
  warning: Clock,
  danger: AlertTriangle,
};

export default function PatientNotificationsPage() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [medicalHistory, setMedicalHistory] = useState<MedicalHistory[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (user?.id) {
      loadData();
    }
  }, [user?.id]);

  const loadData = async () => {
    if (!user?.id) return;
    setIsLoading(true);
    try {
      const [appointmentsRes, medicalHistoryRes] = await Promise.all([
        patientApi.getAppointments(user.id),
        patientApi.getMedicalHistories(user.id),
      ]);

      if (appointmentsRes.success && appointmentsRes.data) {
        setAppointments(appointmentsRes.data);
      }
      if (medicalHistoryRes.success && medicalHistoryRes.data) {
        setMedicalHistory(medicalHistoryRes.data);
      }
    } catch (error) {
      toast.error('Failed to load notifications');
    } finally {
      setIsLoading(false);
    }
  };

  const notifications = useMemo<NotificationItem[]>(() => {
    const items: NotificationItem[] = [];

    appointments.forEach((apt) => {
      const baseDescription = `${apt.appointmentType === 'ONLINE' ? 'Virtual' : 'Clinic'} visit with ${
        apt.doctorName
      } on ${format(new Date(apt.appointmentDateTime), 'MMM dd, h:mm a')}`;

      switch (apt.status) {
        case 'PENDING':
          items.push({
            id: `apt-${apt.id}-pending`,
            title: 'Appointment awaiting confirmation',
            description: `${baseDescription}. We'll notify you once your doctor responds.`,
            timestamp: apt.updatedAt || apt.createdAt,
            variant: 'warning',
            action: { label: 'View appointment', href: `/patient/appointments` },
          });
          break;
        case 'CONFIRMED':
          items.push({
            id: `apt-${apt.id}-confirmed`,
            title: 'Appointment confirmed',
            description: `${baseDescription} has been confirmed.`,
            timestamp: apt.updatedAt || apt.createdAt,
            variant: 'success',
            action:
              apt.appointmentType === 'ONLINE' && apt.zoomJoinUrl
                ? { label: 'Join Zoom', href: apt.zoomJoinUrl }
                : { label: 'View details', href: `/patient/appointments` },
          });
          break;
        case 'COMPLETED':
          items.push({
            id: `apt-${apt.id}-completed`,
            title: 'Visit completed',
            description: `Thanks for attending your consultation with ${apt.doctorName}.`,
            timestamp: apt.updatedAt || apt.createdAt,
            variant: 'info',
          });
          break;
        case 'CANCELLED':
        case 'REJECTED':
          items.push({
            id: `apt-${apt.id}-cancelled`,
            title: 'Appointment update',
            description: `${baseDescription} was ${apt.status.toLowerCase()}. Please reschedule if needed.`,
            timestamp: apt.updatedAt || apt.createdAt,
            variant: 'danger',
            action: { label: 'Book again', href: '/patient/doctors' },
          });
          break;
      }
    });

    const soonAppointments = appointments.filter((apt) => {
      const diffHours =
        (new Date(apt.appointmentDateTime).getTime() - Date.now()) / (1000 * 60 * 60);
      return diffHours > 0 && diffHours <= 48 && apt.status === 'CONFIRMED';
    });

    soonAppointments.forEach((apt) => {
      items.push({
        id: `apt-${apt.id}-reminder`,
        title: 'Upcoming visit reminder',
        description: `You have a consultation with ${apt.doctorName} in the next 48 hours.`,
        timestamp: new Date().toISOString(),
        variant: 'info',
        action: { label: 'Review details', href: '/patient/appointments' },
      });
    });

    medicalHistory.slice(0, 5).forEach((record) => {
      items.push({
        id: `history-${record.id}`,
        title: `Record updated: ${record.condition}`,
        description: `Status marked as ${record.status.toLowerCase()} by ${record.doctorName}.`,
        timestamp: record.updatedAt || record.createdAt,
        variant: 'success',
        action: { label: 'View record', href: '/patient/medical-history' },
      });
    });

    if (user?.isVerified === false) {
      items.push({
        id: 'verification-reminder',
        title: 'Complete your verification',
        description: 'Verify your email to unlock full access to HealthLink+ services.',
        timestamp: new Date().toISOString(),
        variant: 'warning',
      });
    }

    return items.sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime(),
    );
  }, [appointments, medicalHistory, user]);

  const handleLogout = () => {
    logout();
    router.replace('/');
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav
        userName={`${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() || 'Patient'}
        userRole="Patient"
        showPortalLinks={false}
        onLogout={handleLogout}
      />

      <div className="flex">
        <Sidebar items={patientSidebarItems} currentPath="/patient/notifications" />

        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <div className="max-w-5xl mx-auto space-y-8">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="text-sm uppercase tracking-wide text-slate-500">Activity</p>
                <h1 className="text-3xl font-semibold text-slate-900">Notifications</h1>
                <p className="text-slate-600">
                  Track appointment updates, record changes, and account alerts.
                </p>
              </div>
              <Button
                onClick={loadData}
                variant="outline"
                className="border-slate-300 text-slate-700"
                disabled={isLoading}
              >
                Refresh feed
              </Button>
            </div>

            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-4">
              {isLoading ? (
                <p className="text-slate-500 text-center py-8">Loading notifications...</p>
              ) : notifications.length === 0 ? (
                <div className="text-center py-10">
                  <Bell className="w-10 h-10 text-slate-300 mx-auto mb-3" />
                  <p className="text-slate-600 font-medium">You're all caught up!</p>
                  <p className="text-sm text-slate-500">We'll let you know when something changes.</p>
                </div>
              ) : (
                notifications.map((notification) => {
                  const Icon = variantIcon[notification.variant];
                  return (
                    <div
                      key={notification.id}
                      className={`flex gap-4 rounded-2xl border p-4 ${variantStyles[notification.variant]}`}
                    >
                      <div className="mt-1">
                        <Icon className="w-6 h-6" />
                      </div>
                      <div className="flex-1">
                        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-2">
                          <div>
                            <p className="font-semibold text-slate-900">{notification.title}</p>
                            <p className="text-sm text-slate-700">{notification.description}</p>
                          </div>
                          <span className="text-xs text-slate-500">
                            {formatDistanceToNow(new Date(notification.timestamp), {
                              addSuffix: true,
                            })}
                          </span>
                        </div>
                        {notification.action && (
                          <a
                            href={notification.action.href}
                            className="inline-flex items-center gap-2 text-sm font-medium text-slate-900 mt-3"
                          >
                            <CalendarIcon className="w-4 h-4" />
                            {notification.action.label}
                          </a>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            <div className="bg-gradient-to-br from-teal-500 to-violet-600 rounded-2xl p-6 text-white shadow-lg">
              <h2 className="text-lg font-semibold mb-2">Stay informed</h2>
              <p className="text-white/80 text-sm mb-4">
                Use the HealthLink+ mobile app for instant push notifications and reminders.
              </p>
              <div className="flex flex-wrap gap-3">
                <Button className="bg-white text-slate-900 hover:bg-white/90">Download iOS</Button>
                <Button variant="outline" className="bg-transparent border-white/40 text-white">
                  Download Android
                </Button>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

