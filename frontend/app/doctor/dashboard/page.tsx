'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { format, isToday } from 'date-fns';
import { toast } from 'react-hot-toast';
import {
  LayoutDashboard,
  Calendar,
  Users,
  Building2,
  Settings,
  Clock,
  Video,
  ClipboardList,
  TrendingUp,
  AlertCircle,
} from 'lucide-react';

import { useAuth } from '@/contexts/AuthContext';
import { appointmentsApi, facilitiesApi, analyticsApi } from '@/lib/api';
import { Appointment, Clinic } from '@/types';

import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { StatsCard } from '@/marketing/dashboard/StatsCard';
import { AppointmentCard } from '@/marketing/dashboard/AppointmentCard';
import { Button } from '@/marketing/ui/button';
import { Badge } from '@/marketing/ui/badge';

export default function DoctorDashboard() {
  const router = useRouter();
  const { user, logout } = useAuth();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [clinics, setClinics] = useState<Clinic[]>([]);
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
      const [appointmentsData, clinicsData] = await Promise.all([
        appointmentsApi.list(),
        facilitiesApi.listForDoctor(user.id.toString()),
      ]);

      setAppointments(Array.isArray(appointmentsData) ? appointmentsData : []);
      setClinics(Array.isArray(clinicsData) ? clinicsData : []);
    } catch (error: any) {
      toast.error('Failed to load dashboard data');
      console.error('Dashboard load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAppointmentAction = async (
    appointmentId: string,
    action: 'confirm' | 'reject' | 'complete',
  ) => {
    try {
      switch (action) {
        case 'confirm':
          // In new backend, appointments are auto-confirmed after payment
          await appointmentsApi.getById(appointmentId);
          toast.success('Appointment confirmed');
          break;
        case 'reject':
          await appointmentsApi.cancel(appointmentId, 'Rejected by doctor');
          toast.success('Appointment rejected');
          break;
        case 'complete':
          await appointmentsApi.complete(appointmentId);
          toast.success('Appointment completed');
          break;
      }
      loadData();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to perform action');
    }
  };

  const sidebarItems = [
    { icon: LayoutDashboard, label: 'Dashboard', href: '/doctor/dashboard' },
    { icon: Calendar, label: 'Appointments', href: '/doctor/appointments', badge: appointments.length },
    { icon: Users, label: 'Patients', href: '/doctor/appointments' },
    { icon: Building2, label: 'Clinics', href: '/doctor/clinics' },
    { icon: ClipboardList, label: 'Tasks', href: '/doctor/dashboard#tasks' },
    { icon: Settings, label: 'Settings', href: '/doctor/profile' },
  ];

  const todaysAppointments = useMemo(
    () =>
      Array.isArray(appointments)
        ? appointments.filter((apt) => isToday(new Date(apt.appointmentDateTime)))
        : [],
    [appointments],
  );
  const pendingAppointments = Array.isArray(appointments)
    ? appointments.filter((apt) => apt.status === 'PENDING')
    : [];
  const activeClinics = Array.isArray(clinics)
    ? clinics.filter((clinic) => clinic.active)
    : [];
  const todaysVideoConsults = todaysAppointments.filter((apt) => apt.appointmentType === 'ONLINE')
    .length;

  const patientSummaries = useMemo(() => {
    const map = new Map<number, { name: string; lastVisit: string; condition: string }>();
    appointments.forEach((apt) => {
      map.set(apt.patientId, {
        name: apt.patientName,
        lastVisit: format(new Date(apt.appointmentDateTime), 'MMM dd, yyyy'),
        condition: apt.reason,
      });
    });
    return Array.from(map.values()).slice(0, 4);
  }, [appointments]);

  const tasks = pendingAppointments.slice(0, 4).map((apt) => ({
    title: `Confirm ${apt.patientName} (${format(new Date(apt.appointmentDateTime), 'p')})`,
    priority: 'high' as const,
    onComplete: () => handleAppointmentAction(apt.id, 'confirm'),
  }));

  const mapStatus = (
    status: Appointment['status'],
  ): 'pending' | 'confirmed' | 'completed' | 'cancelled' => {
    switch (status) {
      case 'PENDING':
        return 'pending';
      case 'COMPLETED':
        return 'completed';
      case 'CANCELLED':
      case 'REJECTED':
        return 'cancelled';
      default:
        return 'confirmed';
    }
  };

  const formatDate = (dateString: string, pattern: string) =>
    format(new Date(dateString), pattern);

  const handleLogout = () => {
    logout();
    router.replace('/');
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white flex items-center justify-center text-slate-600">
        Loading dashboard...
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav
        userName={`Dr. ${user?.lastName ?? user?.firstName ?? ''}`.trim() || 'Doctor'}
        userRole="Doctor"
        showPortalLinks={false}
        onLogout={handleLogout}
      />

      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/doctor/dashboard" />

        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <div className="max-w-7xl mx-auto space-y-8">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h1 className="text-3xl text-slate-900 mb-2">
                  Good {new Date().getHours() < 12 ? 'morning' : 'afternoon'}, Dr.{' '}
                  {user?.lastName ?? ''}
                </h1>
                <p className="text-slate-600">
                  You have {todaysAppointments.length} appointment
                  {todaysAppointments.length === 1 ? '' : 's'} today
                </p>
              </div>
              <div className="flex gap-3">
                <Link href="/doctor/emergency/new">
                  <Button variant="default" className="bg-red-600 hover:bg-red-700 text-white">
                    <AlertCircle className="w-4 h-4 mr-2" />
                    Emergency Patient
                  </Button>
                </Link>
                <Link href="/doctor/appointments">
                  <Button variant="outline">
                    <Calendar className="w-4 h-4 mr-2" />
                    View Calendar
                  </Button>
                </Link>
              </div>
            </div>

            <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
              <StatsCard
                icon={Calendar}
                label="Today's Appointments"
                value={todaysAppointments.length.toString()}
                gradient="from-blue-500 to-cyan-500"
              />
              <StatsCard
                icon={Clock}
                label="Pending Confirmations"
                value={pendingAppointments.length.toString()}
                gradient="from-orange-500 to-amber-500"
              />
              <StatsCard
                icon={Building2}
                label="Active Clinics"
                value={activeClinics.length.toString()}
                gradient="from-teal-500 to-emerald-500"
              />
              <StatsCard
                icon={Video}
                label="Video Consults Today"
                value={todaysVideoConsults.toString()}
                gradient="from-violet-500 to-purple-500"
              />
            </div>

            <div className="grid lg:grid-cols-3 gap-8">
              <div className="lg:col-span-2 space-y-6">
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Appointment Queue</h2>
                    <Link href="/doctor/appointments">
                      <Button variant="ghost" size="sm">
                        View All
                      </Button>
                    </Link>
                  </div>

                  {appointments.length === 0 ? (
                    <p className="text-slate-500 text-center py-6">No appointments yet.</p>
                  ) : (
                    <div className="space-y-4">
                      {appointments.slice(0, 5).map((apt) => (
                        <AppointmentCard
                          key={apt.id}
                          patientName={apt.patientName}
                          specialty={apt.reason}
                          date={formatDate(apt.appointmentDateTime, 'MMM dd, yyyy')}
                          time={formatDate(apt.appointmentDateTime, 'h:mm a')}
                          type={apt.appointmentType === 'ONLINE' ? 'online' : 'onsite'}
                          clinicName={apt.clinicName}
                          status={mapStatus(apt.status)}
                          showActions={apt.status === 'PENDING'}
                          onConfirm={() => handleAppointmentAction(apt.id.toString(), 'confirm')}
                          onReject={() => handleAppointmentAction(apt.id.toString(), 'reject')}
                          onJoinZoom={
                            apt.appointmentType === 'ONLINE' && apt.zoomJoinUrl
                              ? () => window.open(apt.zoomJoinUrl as string, '_blank')
                              : undefined
                          }
                        />
                      ))}
                    </div>
                  )}
                </div>

                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Clinic Management</h2>
                    <Link href="/doctor/clinics">
                      <Button variant="ghost" size="sm">
                        Manage
                      </Button>
                    </Link>
                  </div>

                  {clinics.length === 0 ? (
                    <p className="text-slate-500">No clinics configured yet.</p>
                  ) : (
                    <div className="space-y-4">
                      {clinics.map((clinic) => (
                        <div
                          key={clinic.id}
                          className="flex items-start justify-between p-4 bg-white rounded-xl border border-slate-200"
                        >
                          <div className="flex items-start gap-3">
                            <div className="w-12 h-12 bg-gradient-to-br from-teal-500 to-emerald-500 rounded-xl flex items-center justify-center">
                              <Building2 className="w-6 h-6 text-white" />
                            </div>
                            <div>
                              <div className="flex items-center gap-2 mb-1">
                                <p className="text-slate-900">{clinic.name}</p>
                                <Badge
                                  variant="default"
                                  className={`text-xs ${
                                    clinic.active ? '' : 'bg-slate-200 text-slate-600'
                                  }`}
                                >
                                  {clinic.active ? 'Active' : 'Inactive'}
                                </Badge>
                              </div>
                              <p className="text-sm text-slate-600 mb-1">{clinic.address}</p>
                              <p className="text-xs text-slate-500">
                                Opens {clinic.openingTime} • Closes {clinic.closingTime}
                              </p>
                            </div>
                          </div>
                          <Link href={`/doctor/clinics/${clinic.id}/edit`}>
                            <Button size="sm" variant="outline">
                              Edit
                            </Button>
                          </Link>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Recent Patients</h2>
                    <Link href="/doctor/appointments">
                      <Button variant="ghost" size="sm">
                        View All
                      </Button>
                    </Link>
                  </div>

                  {patientSummaries.length === 0 ? (
                    <p className="text-slate-500">No patients yet.</p>
                  ) : (
                    <div className="space-y-3">
                      {patientSummaries.map((patient) => (
                        <div
                          key={patient.name}
                          className="flex items-center justify-between p-4 bg-white rounded-xl border border-slate-200 hover:shadow-md transition"
                        >
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-gradient-to-br from-violet-500 to-purple-500 rounded-full flex items-center justify-center">
                              <span className="text-white text-sm">{patient.name[0]}</span>
                            </div>
                            <div>
                              <p className="text-slate-900 text-sm">{patient.name}</p>
                              <p className="text-xs text-slate-600">{patient.condition}</p>
                            </div>
                          </div>
                          <p className="text-xs text-slate-500">{patient.lastVisit}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <div className="space-y-6" id="tasks">
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <h3 className="text-lg text-slate-900 mb-4">Today's Tasks</h3>
                  {tasks.length === 0 ? (
                    <p className="text-slate-500">No pending tasks.</p>
                  ) : (
                    <div className="space-y-3">
                      {tasks.map((task, index) => (
                        <div key={index} className="flex items-start gap-3">
                          <input type="checkbox" className="mt-1 w-4 h-4 text-teal-600 rounded" />
                          <div className="flex-1">
                            <p className="text-sm text-slate-900 mb-1">{task.title}</p>
                            <Badge variant="destructive" className="text-xs">
                              High
                            </Badge>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                  <Link href="/doctor/appointments">
                    <Button variant="ghost" size="sm" className="w-full mt-4">
                      View All Tasks
                    </Button>
                  </Link>
                </div>

                <div className="bg-gradient-to-br from-teal-500 to-violet-600 rounded-2xl p-6 text-white shadow-lg">
                  <div className="flex items-center gap-2 mb-4">
                    <TrendingUp className="w-5 h-5" />
                    <h3 className="text-lg">This Month</h3>
                  </div>

                  <div className="space-y-4">
                    <div>
                      <p className="text-white/80 text-sm mb-1">Patients Seen</p>
                      <p className="text-3xl">{appointments.length}</p>
                    </div>
                    <div>
                      <p className="text-white/80 text-sm mb-1">Pending Actions</p>
                      <p className="text-3xl">{pendingAppointments.length}</p>
                    </div>
                    <div>
                      <p className="text-white/80 text-sm mb-1">Clinics Managed</p>
                      <p className="text-3xl">{clinics.length}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

