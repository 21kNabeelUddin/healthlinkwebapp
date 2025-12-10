'use client';

import { useEffect, useMemo, useState } from 'react';
import { format, formatDistanceToNow } from 'date-fns';
import { Bell, CheckCircle2, Clock, AlertTriangle, Calendar as CalendarIcon } from 'lucide-react';
import { toast } from 'react-hot-toast';

import { useAuth } from '@/contexts/AuthContext';
import { appointmentsApi } from '@/lib/api';
import { Appointment } from '@/types';
import DashboardLayout from '@/components/layout/DashboardLayout';
import { Button } from '@/marketing/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/marketing/ui/card';
import { Badge } from '@/marketing/ui/badge';

type NotificationVariant = 'success' | 'info' | 'warning' | 'danger';

interface NotificationItem {
  id: string;
  title: string;
  description: string;
  timestamp: string;
  variant: NotificationVariant;
  action?: { label: string; href: string };
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

export default function DoctorNotificationsPage() {
  const { user, logout } = useAuth();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (user?.id) {
      loadData();
    }
  }, [user?.id]);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const list = await appointmentsApi.list();
      setAppointments(Array.isArray(list) ? list : []);
    } catch (error) {
      toast.error('Failed to load notifications');
    } finally {
      setIsLoading(false);
    }
  };

  const notifications = useMemo<NotificationItem[]>(() => {
    const items: NotificationItem[] = [];

    appointments.forEach((apt) => {
      const base = `${apt.patientName || 'Patient'} on ${format(new Date(apt.appointmentDateTime), 'MMM dd, h:mm a')}`;
      switch (apt.status) {
        case 'IN_PROGRESS':
          items.push({
            id: `apt-${apt.id}-new`,
            title: 'New appointment scheduled',
            description: `You have a visit with ${base}.`,
            timestamp: apt.updatedAt || apt.createdAt || new Date().toISOString(),
            variant: 'info',
            action: { label: 'View appointment', href: '/doctor/appointments' },
          });
          break;
        case 'COMPLETED':
          items.push({
            id: `apt-${apt.id}-done`,
            title: 'Appointment completed',
            description: `Consultation with ${base} is marked completed.`,
            timestamp: apt.updatedAt || apt.createdAt || new Date().toISOString(),
            variant: 'success',
            action: { label: 'Review notes', href: '/doctor/appointments' },
          });
          break;
        case 'CANCELLED':
          items.push({
            id: `apt-${apt.id}-cancelled`,
            title: 'Appointment cancelled',
            description: `The appointment with ${base} was cancelled.`,
            timestamp: apt.updatedAt || apt.createdAt || new Date().toISOString(),
            variant: 'danger',
            action: { label: 'View calendar', href: '/doctor/appointments' },
          });
          break;
        case 'NO_SHOW':
          items.push({
            id: `apt-${apt.id}-no-show`,
            title: 'Patient no-show recorded',
            description: `${apt.patientName || 'Patient'} did not attend on ${format(new Date(apt.appointmentDateTime), 'MMM dd, h:mm a')}.`,
            timestamp: apt.updatedAt || apt.createdAt || new Date().toISOString(),
            variant: 'warning',
            action: { label: 'Log follow-up', href: '/doctor/appointments' },
          });
          break;
        default:
          break;
      }
    });

    // Sort newest first
    return items.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
  }, [appointments]);

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
        <div className="flex-1 p-8 space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-800">Notifications</h1>
              <p className="text-gray-600">Stay updated on your appointments and patient activity</p>
            </div>
            <Button variant="outline" size="sm" onClick={loadData} disabled={isLoading}>
              Refresh
            </Button>
          </div>

          <div className="grid gap-4">
            {notifications.length === 0 && !isLoading && (
              <Card>
                <CardContent className="p-6 text-center text-gray-500">No notifications yet.</CardContent>
              </Card>
            )}

            {notifications.map((item) => {
              const Icon = variantIcon[item.variant];
              return (
                <div
                  key={item.id}
                  className={`border rounded-lg p-4 flex items-start gap-4 ${variantStyles[item.variant]}`}
                >
                  <div className="mt-1">
                    <Icon className="w-5 h-5" />
                  </div>
                  <div className="flex-1 space-y-1">
                    <div className="flex items-center gap-3">
                      <h3 className="font-semibold text-lg">{item.title}</h3>
                      <Badge variant="outline" className="flex items-center gap-1">
                        <CalendarIcon className="w-3 h-3" />
                        {formatDistanceToNow(new Date(item.timestamp), { addSuffix: true })}
                      </Badge>
                    </div>
                    <p className="text-gray-700">{item.description}</p>
                    {item.action && (
                      <Button asChild size="sm" variant="outline" className="mt-2">
                        <a href={item.action.href}>{item.action.label}</a>
                      </Button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}

