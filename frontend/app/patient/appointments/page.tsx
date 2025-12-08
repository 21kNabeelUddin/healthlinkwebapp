'use client';

import { useState, useEffect, useRef, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { appointmentsApi, reviewsApi } from '@/lib/api';
import { Appointment } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Link from 'next/link';
import { format } from 'date-fns';
import { Star, Calendar, Clock, MapPin, Video, AlertCircle, CheckCircle2 } from 'lucide-react';
import { Badge } from '@/marketing/ui/badge';
import Input from '@/components/ui/Input';

import { ActiveAppointmentPrescriptionMonitor } from '@/components/prescription/ActiveAppointmentPrescriptionMonitor';


export default function AppointmentsPage() {
  const router = useRouter();
  const { user } = useAuth();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [isLoading, setIsLoading] = useState(true);
  const [reviewedAppointments, setReviewedAppointments] = useState<Set<string>>(new Set());
  const hasCheckedReviews = useRef(false);

  const checkReviews = async (appts: Appointment[]) => {
    if (!user?.id || hasCheckedReviews.current) return;
    hasCheckedReviews.current = true;
    
    try {
      const myReviews = await reviewsApi.getMine();
      const reviewedIds = new Set(myReviews.map((r: any) => r.appointmentId?.toString()));
      setReviewedAppointments(reviewedIds);
      
      // Check for newly completed appointments that need review
      const completedAppointments = appts.filter(
        apt => apt.status === 'COMPLETED' && !reviewedIds.has(apt.id.toString())
      );
      
      if (completedAppointments.length > 0) {
        // Show notification and auto-redirect to first completed appointment review
        const firstCompleted = completedAppointments[0];
        setTimeout(() => {
          toast.success('Your appointment has been completed. Please rate your experience.', {
            duration: 5000,
            action: {
              label: 'Rate Now',
              onClick: () => router.push(`/patient/appointments/${firstCompleted.id}/review`),
            },
          });
          // Auto-redirect after 5 seconds if user doesn't click
          setTimeout(() => {
            router.push(`/patient/appointments/${firstCompleted.id}/review`);
          }, 5000);
        }, 1000);
      }
    } catch (error) {
      console.error('Failed to check reviews:', error);
      hasCheckedReviews.current = false; // Reset on error
    }
  };

  useEffect(() => {
    if (user?.id) {
      loadAppointments();
    }
  }, [user, statusFilter]);

  useEffect(() => {
    if (appointments.length > 0 && user?.id) {
      checkReviews(appointments);
    }
  }, [appointments.length, user?.id]);

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      const data = await appointmentsApi.list(statusFilter || undefined);
      setAppointments(data || []);
    } catch (error: any) {
      toast.error('Failed to load appointments');
      console.error('Appointments load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = async (appointmentId: number | string) => {
    if (!confirm('Are you sure you want to cancel this appointment?')) return;

    try {
      await appointmentsApi.cancel(String(appointmentId));
      toast.success('Appointment cancelled');
      loadAppointments();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to cancel appointment');
    }
  };

  const getStatusClasses = (status: string) => {
    switch (status) {
      case 'PENDING_PAYMENT':
        return 'bg-amber-100 text-amber-800 border border-amber-200';
      case 'CONFIRMED':
        return 'bg-green-100 text-green-800 border border-green-200';
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800 border border-blue-200';
      case 'COMPLETED':
        return 'bg-teal-100 text-teal-800 border border-teal-200';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800 border border-red-200';
      case 'NO_SHOW':
        return 'bg-slate-100 text-slate-700 border border-slate-200';
      default:
        return 'bg-slate-100 text-slate-700 border border-slate-200';
    }
  };

  const getStatusLabel = (status: string) =>
    status
      .split('_')
      .map((p) => p.charAt(0) + p.slice(1).toLowerCase())
      .join(' ');

  const filteredAppointments = useMemo(() => {
    const term = searchTerm.toLowerCase().trim();
    return appointments
      .filter((apt) => {
        const matchesStatus = !statusFilter || apt.status === statusFilter;
        const matchesSearch =
          !term ||
          (apt.doctorName || '').toLowerCase().includes(term) ||
          (apt.clinicName || '').toLowerCase().includes(term) ||
          (apt.reason || '').toLowerCase().includes(term);
        return matchesStatus && matchesSearch;
      })
      .sort(
        (a, b) =>
          new Date(a.appointmentDateTime).getTime() -
          new Date(b.appointmentDateTime).getTime()
      );
  }, [appointments, statusFilter, searchTerm]);

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="PATIENT">
        <div className="text-center py-12 text-slate-600">Loading appointments...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="PATIENT">

      {/* Monitor active appointments for prescriptions */}
      <ActiveAppointmentPrescriptionMonitor 
        appointments={filteredAppointments} 
        autoRedirect={true}
        redirectDelay={5000}
      />
      

      <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
          {/* Header */}
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <p className="text-sm text-slate-500 uppercase tracking-wide">Appointments</p>
              <h1 className="text-4xl font-bold text-slate-900">My Appointments</h1>
              <p className="text-slate-600">View and manage your upcoming and past visits</p>
            </div>
            <Link href="/patient/doctors">
              <Button className="bg-gradient-to-r from-teal-500 to-violet-600 text-white">
                Book New Appointment
              </Button>
            </Link>
          </div>

          {/* Filters */}
          <Card className="p-4 shadow-sm">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">Status</label>
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500 bg-white"
                >
                  <option value="">All Statuses</option>
                  <option value="PENDING_PAYMENT">Pending Payment</option>
                  <option value="CONFIRMED">Confirmed</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="CANCELLED">Cancelled</option>
                  <option value="NO_SHOW">No Show</option>
                </select>
              </div>
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-slate-700 mb-2">Search</label>
                <Input
                  placeholder="Search by doctor, clinic, or reason"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>
          </Card>

          {/* Lists */}
          <div className="space-y-6">
            {filteredAppointments.length === 0 ? (
              <Card className="p-10 text-center">
                <div className="mx-auto mb-3 w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center">
                  <Calendar className="w-6 h-6 text-slate-500" />
                </div>
                <h3 className="text-xl font-semibold text-slate-900 mb-1">No appointments found</h3>
                <p className="text-slate-600">Try adjusting filters or book a new appointment.</p>
              </Card>
            ) : (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                {filteredAppointments.map((appointment) => (
                  <Card key={appointment.id} className="overflow-hidden shadow-sm hover:shadow-md transition-shadow">
                    <div className="bg-gradient-to-r from-teal-50 to-violet-50 border-b border-slate-200 p-4 flex items-center justify-between">
                      <div className="flex items-start gap-3">
                        <div className="w-10 h-10 rounded-lg bg-white border border-slate-200 flex items-center justify-center">
                          <Calendar className="w-5 h-5 text-teal-600" />
                        </div>
                        <div>
                          <h3 className="text-lg font-semibold text-slate-900">{appointment.doctorName}</h3>
                          <div className="flex flex-wrap items-center gap-2 mt-1">
                            <Badge
                              variant="outline"
                              className={`${getStatusClasses(appointment.status)} text-xs`}
                            >
                              {getStatusLabel(appointment.status)}
                            </Badge>
                            <Badge variant="secondary" className="text-xs">
                              {appointment.appointmentType}
                            </Badge>
                          </div>
                        </div>
                      </div>
                      {appointment.appointmentType === 'ONLINE' && appointment.zoomJoinUrl && appointment.status === 'CONFIRMED' && (
                        <a
                          href={appointment.zoomJoinUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-2 px-3 py-2 bg-gradient-to-r from-teal-500 to-violet-600 text-white rounded-lg text-sm"
                        >
                          <Video className="w-4 h-4" />
                          Join Zoom
                        </a>
                      )}
                    </div>

                    <div className="p-4 space-y-3">
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm text-slate-700">
                        <div className="flex items-center gap-2">
                          <Clock className="w-4 h-4 text-slate-400" />
                          <div>
                            <p className="text-xs uppercase text-slate-400">Date & Time</p>
                            <p className="font-semibold text-slate-900">
                              {appointment.appointmentDateTime && !isNaN(new Date(appointment.appointmentDateTime).getTime())
                                ? format(new Date(appointment.appointmentDateTime), 'MMM dd, yyyy h:mm a')
                                : 'Date not available'}
                            </p>
                          </div>
                        </div>
                        {appointment.clinicName && (
                          <div className="flex items-center gap-2">
                            <MapPin className="w-4 h-4 text-rose-500" />
                            <div>
                              <p className="text-xs uppercase text-slate-400">Clinic</p>
                              <p className="font-semibold text-slate-900">{appointment.clinicName}</p>
                            </div>
                          </div>
                        )}
                      </div>

                      {appointment.reason && (
                        <div className="text-sm text-slate-700">
                          <p className="font-semibold text-slate-900">Reason</p>
                          <p className="text-slate-700">{appointment.reason}</p>
                        </div>
                      )}
                      {appointment.notes && (
                        <div className="text-sm text-slate-600">
                          <p className="font-semibold text-slate-900">Notes</p>
                          <p>{appointment.notes}</p>
                        </div>
                      )}

                      <div className="flex flex-wrap gap-2 pt-2 border-t border-slate-200">
                        {appointment.status === 'PENDING_PAYMENT' && (
                          <div className="inline-flex items-center gap-2 px-3 py-2 bg-amber-50 text-amber-700 rounded-lg text-sm">
                            <AlertCircle className="w-4 h-4" />
                            Payment pending
                          </div>
                        )}
                        {appointment.status === 'IN_PROGRESS' && (
                          <div className="inline-flex items-center gap-2 px-3 py-2 bg-blue-50 text-blue-700 rounded-lg text-sm">
                            <CheckCircle2 className="w-4 h-4" />
                            In progress
                          </div>
                        )}
                      </div>

                      <div className="flex flex-col sm:flex-row gap-2">
                        {appointment.status === 'PENDING_PAYMENT' && (
                          <Button
                            variant="secondary"
                            className="w-full"
                            onClick={() => handleCancel(appointment.id)}
                          >
                            Cancel
                          </Button>
                        )}

                        {appointment.appointmentType === 'ONLINE' && appointment.zoomJoinUrl && appointment.status === 'CONFIRMED' && (
                          <a
                            href={appointment.zoomJoinUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="w-full"
                          >
                            <Button className="w-full">Join Zoom</Button>
                          </a>
                        )}

                        {appointment.status === 'COMPLETED' && (
                          <Link href={`/patient/prescriptions?appointmentId=${appointment.id}`} className="w-full">
                            <Button variant="outline" className="w-full">View Prescription</Button>
                          </Link>
                        )}

                        {appointment.status === 'COMPLETED' && !reviewedAppointments.has(appointment.id.toString()) && (
                          <Link href={`/patient/appointments/${appointment.id}/review`} className="w-full">
                            <Button className="w-full bg-gradient-to-r from-yellow-500 to-orange-500 hover:from-yellow-600 hover:to-orange-600">
                              <Star className="w-4 h-4 mr-2" />
                              Rate Appointment
                            </Button>
                          </Link>
                        )}
                      </div>
                    </div>
                  </Card>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
