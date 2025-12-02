'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { format } from 'date-fns';
import { toast } from 'react-hot-toast';
import { Calendar, FileText, Bell, Plus, Video, Clock, CheckCircle2 } from 'lucide-react';

import { useAuth } from '@/contexts/AuthContext';
import { appointmentsApi, medicalRecordsApi, analyticsApi } from '@/lib/api';
import { Appointment, MedicalHistory } from '@/types';

import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { StatsCard } from '@/marketing/dashboard/StatsCard';
import { AppointmentCard } from '@/marketing/dashboard/AppointmentCard';
import { Button } from '@/marketing/ui/button';
import { patientSidebarItems } from '@/app/patient/sidebar-items';

export default function PatientDashboard() {
  const router = useRouter();
  const { user, logout } = useAuth();
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
      const [appointmentsData, medicalHistoryData] = await Promise.all([
        appointmentsApi.list(),
        medicalRecordsApi.listForPatient(user.id.toString()),
      ]);

      // Ensure we always have arrays, even if API returns unexpected format
      setAppointments(Array.isArray(appointmentsData) ? appointmentsData : []);
      setMedicalHistory(Array.isArray(medicalHistoryData) ? medicalHistoryData : []);
    } catch (error: any) {
      toast.error('Failed to load dashboard data');
      console.error('Dashboard load error:', error);
      // Set empty arrays on error to prevent further errors
      setAppointments([]);
      setMedicalHistory([]);
    } finally {
      setIsLoading(false);
    }
  };

  const upcomingAppointments = useMemo(
    () =>
      Array.isArray(appointments)
        ? appointments
            .slice()
            .sort(
              (a, b) =>
                new Date(a.appointmentDateTime).getTime() -
                new Date(b.appointmentDateTime).getTime(),
            )
        : [],
    [appointments],
  );

  const onlineAppointments = appointments.filter((apt) => apt.appointmentType === 'ONLINE').length;
  const confirmedAppointments = appointments.filter((apt) => apt.status === 'CONFIRMED').length;

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

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white flex items-center justify-center text-slate-600">
        Loading dashboard...
      </div>
    );
  }

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
        <Sidebar items={patientSidebarItems} currentPath="/patient/dashboard" />

        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <div className="max-w-7xl mx-auto space-y-8">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h1 className="text-3xl text-slate-900 mb-2">
                  Welcome back, {user?.firstName ?? 'Patient'}
                </h1>
                <p className="text-slate-600">Here's your health overview for today</p>
              </div>
              <div className="flex gap-3">
                <Link href="/patient/appointments/book">
                  <Button className="bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700">
                    <Plus className="w-4 h-4 mr-2" />
                    Book Appointment
                  </Button>
                </Link>
                <Link href="/patient/medical-history">
                  <Button variant="outline">View Records</Button>
                </Link>
              </div>
            </div>

            <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
              <StatsCard
                icon={Calendar}
                label="Upcoming Appointments"
                value={upcomingAppointments.length.toString()}
                gradient="from-blue-500 to-cyan-500"
              />
              <StatsCard
                icon={Video}
                label="Video Consultations"
                value={onlineAppointments.toString()}
                gradient="from-teal-500 to-emerald-500"
              />
              <StatsCard
                icon={FileText}
                label="Medical Records"
                value={medicalHistory.length.toString()}
                gradient="from-violet-500 to-purple-500"
              />
              <StatsCard
                icon={Clock}
                label="Confirmed Visits"
                value={confirmedAppointments.toString()}
                gradient="from-pink-500 to-rose-500"
              />
            </div>

            <div className="grid lg:grid-cols-3 gap-8">
              <div className="lg:col-span-2 space-y-6">
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Upcoming Appointments</h2>
                    <Link href="/patient/appointments">
                      <Button variant="ghost" size="sm">
                        View All
                      </Button>
                    </Link>
                  </div>

                  {upcomingAppointments.length === 0 ? (
                    <p className="text-slate-500 text-center py-6">No appointments scheduled.</p>
                  ) : (
                    <div className="space-y-4">
                      {upcomingAppointments.slice(0, 3).map((apt) => (
                        <AppointmentCard
                          key={apt.id}
                          doctorName={apt.doctorName}
                          specialty={apt.reason}
                          date={formatDate(apt.appointmentDateTime, 'MMM dd, yyyy')}
                          time={formatDate(apt.appointmentDateTime, 'h:mm a')}
                          type={apt.appointmentType === 'ONLINE' ? 'online' : 'onsite'}
                          clinicName={apt.clinicName}
                          status={mapStatus(apt.status)}
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

                <div
                  id="medical-history"
                  className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg"
                >
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Medical History</h2>
                    <Link href="/patient/medical-history">
                      <Button variant="ghost" size="sm">
                        <Plus className="w-4 h-4 mr-1" />
                        Add Record
                      </Button>
                    </Link>
                  </div>

                  {medicalHistory.length === 0 ? (
                    <p className="text-slate-500">No medical history records yet.</p>
                  ) : (
                    <div className="space-y-4">
                      {medicalHistory.slice(0, 4).map((record) => (
                        <div key={record.id} className="flex items-start gap-4">
                          <div className="w-10 h-10 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center flex-shrink-0">
                            <FileText className="w-5 h-5 text-white" />
                          </div>
                          <div className="flex-1">
                            <div className="flex items-start justify-between mb-1">
                              <div>
                                <p className="text-slate-900">{record.condition}</p>
                                <p className="text-sm text-slate-600">{record.doctorName}</p>
                              </div>
                              <span className="text-xs text-slate-500">
                                {formatDate(record.diagnosisDate, 'MMM dd, yyyy')}
                              </span>
                            </div>
                            <div className="flex items-center gap-2 mt-2">
                              <CheckCircle2 className="w-4 h-4 text-green-600" />
                              <span className="text-xs text-green-600">{record.status}</span>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <div className="space-y-6" id="notifications">
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <h3 className="text-lg text-slate-900 mb-4">Notifications</h3>
                  <div className="space-y-4">
                    <div className="flex items-start gap-3 p-3 bg-green-50 rounded-lg border border-green-200">
                      <CheckCircle2 className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                      <div>
                        <p className="text-sm text-slate-900 mb-1">OTP Verified</p>
                        <p className="text-xs text-slate-600">
                          Your account {user?.isVerified ? 'is verified' : 'still needs verification'}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-start gap-3 p-3 bg-blue-50 rounded-lg border border-blue-200">
                      <Bell className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                      <div>
                        <p className="text-sm text-slate-900 mb-1">Appointments</p>
                        <p className="text-xs text-slate-600">
                          {upcomingAppointments.length} upcoming appointment
                          {upcomingAppointments.length === 1 ? '' : 's'}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="bg-gradient-to-br from-teal-500 to-violet-600 rounded-2xl p-6 text-white shadow-lg">
                  <h3 className="text-lg mb-4">Quick Actions</h3>

                  <div className="space-y-3">
                    <Link href="/patient/appointments/book">
                      <button className="w-full bg-white/20 backdrop-blur-sm hover:bg-white/30 rounded-lg p-3 text-left transition flex items-center gap-3">
                        <Plus className="w-5 h-5" />
                        <span className="text-sm">Book New Appointment</span>
                      </button>
                    </Link>
                    <Link href="/patient/medical-history">
                      <button className="w-full bg-white/20 backdrop-blur-sm hover:bg-white/30 rounded-lg p-3 text-left transition flex items-center gap-3">
                        <FileText className="w-5 h-5" />
                        <span className="text-sm">Upload Medical Record</span>
                      </button>
                    </Link>
                    <Link href="/patient/appointments">
                      <button className="w-full bg-white/20 backdrop-blur-sm hover:bg-white/30 rounded-lg p-3 text-left transition flex items-center gap-3">
                        <Calendar className="w-5 h-5" />
                        <span className="text-sm">View All Appointments</span>
                      </button>
                    </Link>
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

