'use client';

import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { appointmentsApi, facilitiesApi } from '@/lib/api';
import { Appointment, Clinic } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { Badge } from '@/marketing/ui/badge';
import { format } from 'date-fns';
import {
  Building2,
  Calendar,
  Clock,
  MapPin,
  User,
  Video,
  Phone,
  CheckCircle2,
  XCircle,
  AlertCircle,
} from 'lucide-react';

interface ClinicWithAppointments extends Clinic {
  appointments: Appointment[];
}

export default function DoctorAppointmentsPage() {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [selectedClinicFilter, setSelectedClinicFilter] = useState<string>('all');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (user?.id) {
      loadAppointments();
      loadClinics();
    }
  }, [user, statusFilter]);

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

  const loadClinics = async () => {
    if (!user?.id) return;
    try {
      const data = await facilitiesApi.listForDoctor(user.id.toString());
      const activeClinics = data.filter((c) => c.active);
      // Ensure clinic IDs are strings (UUIDs from backend)
      const clinicsWithStringIds = activeClinics.map((c: any) => ({
        ...c,
        id: String(c.id), // Convert to string for consistent comparison
      }));
      setClinics(clinicsWithStringIds || []);
    } catch (error: any) {
      console.error('Failed to load clinics:', error);
    }
  };

  // Group appointments by clinic
  const clinicsWithAppointments = useMemo(() => {
    const clinicMap = new Map<string, ClinicWithAppointments>();

    // Initialize all clinics - use string keys for consistent comparison
    clinics.forEach((clinic) => {
      clinicMap.set(String(clinic.id), {
        ...clinic,
        appointments: [],
      });
    });

    // Add "Online/No Clinic" category for online appointments or appointments without clinic
    clinicMap.set('online', {
      id: 0,
      name: 'Online Consultations',
      address: '',
      city: '',
      state: '',
      zipCode: '',
      phoneNumber: '',
      email: '',
      description: '',
      openingTime: '',
      closingTime: '',
      active: true,
      doctorId: user?.id || 0,
      doctorName: '',
      createdAt: '',
      updatedAt: '',
      appointments: [],
    } as ClinicWithAppointments);

    // Group appointments by clinic first, then show both ONLINE and ONSITE under each clinic
    appointments.forEach((appointment) => {
      // If appointment has a clinicId, add it to that clinic (regardless of ONLINE/ONSITE)
      if (appointment.clinicId) {
        // Convert both to strings for consistent comparison (handles UUID strings and numeric IDs)
        const appointmentClinicId = String(appointment.clinicId);
        const clinic = clinicMap.get(appointmentClinicId);
        
        if (clinic) {
          clinic.appointments.push(appointment);
        } else {
          // Clinic not found in our list, add to online as fallback
          const onlineClinic = clinicMap.get('online');
          if (onlineClinic) {
            onlineClinic.appointments.push(appointment);
          }
        }
      } else {
        // No clinic ID - these are truly online-only appointments without a clinic
        // Add to "Online Consultations" section
        const onlineClinic = clinicMap.get('online');
        if (onlineClinic) {
          onlineClinic.appointments.push(appointment);
        }
      }
    });

    // Filter by selected clinic
    let result = Array.from(clinicMap.values());
    if (selectedClinicFilter !== 'all') {
      if (selectedClinicFilter === 'online') {
        result = result.filter((c) => c.id === 0);
      } else {
        result = result.filter((c) => String(c.id) === selectedClinicFilter);
      }
    }

    // Only show clinics with appointments or all clinics if filter is set
    if (selectedClinicFilter === 'all') {
      result = result.filter((c) => c.appointments.length > 0 || clinics.some((cl) => String(cl.id) === String(c.id)));
    }

    return result.sort((a, b) => {
      // Sort by appointment count (descending)
      return b.appointments.length - a.appointments.length;
    });
  }, [appointments, clinics, selectedClinicFilter, user?.id]);

  const handleAppointmentAction = async (appointmentId: string, action: 'confirm' | 'reject' | 'complete') => {
    try {
      switch (action) {
        case 'confirm':
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
      loadAppointments();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to perform action');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'COMPLETED':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'REJECTED':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
        return <CheckCircle2 className="w-4 h-4" />;
      case 'PENDING':
        return <AlertCircle className="w-4 h-4" />;
      case 'CANCELLED':
      case 'REJECTED':
        return <XCircle className="w-4 h-4" />;
      case 'COMPLETED':
        return <CheckCircle2 className="w-4 h-4" />;
      default:
        return <AlertCircle className="w-4 h-4" />;
    }
  };

  const totalAppointments = appointments.length;

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="DOCTOR">
        <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
          <div className="text-center py-20">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-4 border-teal-500 border-t-transparent"></div>
            <p className="mt-4 text-slate-600">Loading appointments...</p>
          </div>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* Header */}
          <div className="mb-8">
            <h1 className="text-4xl font-bold text-slate-900 mb-2">Appointments</h1>
            <p className="text-slate-600">Manage your appointments by clinic</p>
          </div>

          {/* Filters */}
          <div className="mb-6 flex flex-wrap gap-4">
            <div className="flex-1 min-w-[200px]">
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Filter by Status
              </label>
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              >
                <option value="">All Statuses</option>
                <option value="PENDING">Pending</option>
                <option value="CONFIRMED">Confirmed</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELLED">Cancelled</option>
                <option value="REJECTED">Rejected</option>
              </select>
            </div>

            <div className="flex-1 min-w-[200px]">
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Filter by Clinic
              </label>
              <select
                value={selectedClinicFilter}
                onChange={(e) => setSelectedClinicFilter(e.target.value)}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              >
                <option value="all">All Clinics ({totalAppointments})</option>
                <option value="online">Online Consultations</option>
                {clinics.map((clinic) => {
                  const count = appointments.filter((apt) => apt.clinicId === clinic.id).length;
                  return (
                    <option key={clinic.id} value={clinic.id.toString()}>
                      {clinic.name} ({count})
                    </option>
                  );
                })}
              </select>
            </div>
          </div>

          {/* Clinics with Appointments */}
          {clinicsWithAppointments.length === 0 ? (
            <Card>
              <div className="text-center py-12">
                <Calendar className="w-16 h-16 text-slate-300 mx-auto mb-4" />
                <h3 className="text-xl font-semibold text-slate-900 mb-2">No appointments found</h3>
                <p className="text-slate-600">
                  {statusFilter || selectedClinicFilter !== 'all'
                    ? 'Try adjusting your filters'
                    : 'You have no appointments yet'}
                </p>
              </div>
            </Card>
          ) : (
            <div className="space-y-6">
              {clinicsWithAppointments.map((clinic) => (
                <Card key={clinic.id} className="overflow-hidden">
                  {/* Clinic Header */}
                  <div className="bg-gradient-to-r from-teal-50 to-violet-50 border-b border-slate-200 p-6">
                    <div className="flex items-start justify-between">
                      <div className="flex items-start gap-4">
                        <div className="w-12 h-12 bg-gradient-to-br from-teal-500 to-violet-600 rounded-lg flex items-center justify-center flex-shrink-0">
                          {clinic.id === 0 ? (
                            <Video className="w-6 h-6 text-white" />
                          ) : (
                            <Building2 className="w-6 h-6 text-white" />
                          )}
                        </div>
                        <div className="flex-1">
                          <div className="flex items-center gap-3 mb-2">
                            <h2 className="text-2xl font-bold text-slate-900">{clinic.name}</h2>
                            <Badge variant="default" className="bg-teal-500 text-white">
                              {clinic.appointments.length} appointment{clinic.appointments.length !== 1 ? 's' : ''}
                            </Badge>
                          </div>
                          {clinic.id !== 0 && (
                            <div className="space-y-1 text-sm text-slate-600">
                              <div className="flex items-center gap-2">
                                <MapPin className="w-4 h-4 text-red-500" />
                                <span>{clinic.address}, {clinic.city}</span>
                              </div>
                              <div className="flex items-center gap-2">
                                <Clock className="w-4 h-4 text-slate-500" />
                                <span>Opens {clinic.openingTime} • Closes {clinic.closingTime}</span>
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Appointments List */}
                  <div className="p-6">
                    {clinic.appointments.length === 0 ? (
                      <div className="text-center py-8 text-slate-500">
                        No appointments for this clinic
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {clinic.appointments.map((appointment) => (
                          <div
                            key={appointment.id}
                            className="border border-slate-200 rounded-lg p-4 hover:shadow-md transition-all"
                          >
                            <div className="flex items-start justify-between">
                              <div className="flex-1">
                                <div className="flex items-center gap-3 mb-3">
                                  <div className="flex items-center gap-2">
                                    {getStatusIcon(appointment.status)}
                                    <h3 className="text-lg font-semibold text-slate-900">
                                      {appointment.patientName}
                                    </h3>
                                  </div>
                                  <Badge
                                    variant="outline"
                                    className={`${getStatusColor(appointment.status)} border`}
                                  >
                                    {appointment.status}
                                  </Badge>
                                  <Badge variant="secondary" className="text-xs">
                                    {appointment.appointmentType}
                                  </Badge>
                                  {appointment.isEmergency && (
                                    <Badge variant="destructive" className="text-xs">
                                      Emergency
                                    </Badge>
                                  )}
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm text-slate-600 mb-3">
                                  <div className="flex items-center gap-2">
                                    <Calendar className="w-4 h-4 text-slate-400" />
                                    <span>
                                      <strong>Date & Time:</strong>{' '}
                                      {format(new Date(appointment.appointmentDateTime), 'MMM dd, yyyy h:mm a')}
                                    </span>
                                  </div>
                                  <div className="flex items-center gap-2">
                                    <User className="w-4 h-4 text-slate-400" />
                                    <span>
                                      <strong>Patient:</strong> {appointment.patientName}
                                    </span>
                                  </div>
                                  {appointment.patientEmail && (
                                    <div className="flex items-center gap-2">
                                      <Phone className="w-4 h-4 text-slate-400" />
                                      <span>{appointment.patientEmail}</span>
                                    </div>
                                  )}
                                  {appointment.clinicName && appointment.id !== 0 && (
                                    <div className="flex items-center gap-2">
                                      <Building2 className="w-4 h-4 text-slate-400" />
                                      <span>{appointment.clinicName}</span>
                                    </div>
                                  )}
                                </div>

                                {appointment.reason && (
                                  <div className="mb-2">
                                    <p className="text-sm text-slate-700">
                                      <strong>Reason:</strong> {appointment.reason}
                                    </p>
                                  </div>
                                )}

                                {appointment.notes && (
                                  <div className="mb-2">
                                    <p className="text-sm text-slate-600">
                                      <strong>Notes:</strong> {appointment.notes}
                                    </p>
                                  </div>
                                )}

                                {appointment.appointmentType === 'ONLINE' && appointment.zoomJoinUrl && (
                                  <div className="mt-3">
                                    <a
                                      href={appointment.zoomJoinUrl}
                                      target="_blank"
                                      rel="noopener noreferrer"
                                      className="inline-flex items-center gap-2 text-teal-600 hover:text-teal-700 text-sm font-medium"
                                    >
                                      <Video className="w-4 h-4" />
                                      Join Zoom Meeting
                                    </a>
                                  </div>
                                )}
                              </div>

                              {/* Actions */}
                              <div className="flex flex-col gap-2 ml-4">
                                {appointment.status === 'PENDING' && (
                                  <>
                                    <Button
                                      variant="primary"
                                      size="sm"
                                      onClick={() => handleAppointmentAction(appointment.id.toString(), 'confirm')}
                                    >
                                      Confirm
                                    </Button>
                                    <Button
                                      variant="danger"
                                      size="sm"
                                      onClick={() => handleAppointmentAction(appointment.id.toString(), 'reject')}
                                    >
                                      Reject
                                    </Button>
                                  </>
                                )}
                                {appointment.status === 'CONFIRMED' && (
                                  <Button
                                    variant="primary"
                                    size="sm"
                                    onClick={() => handleAppointmentAction(appointment.id.toString(), 'complete')}
                                  >
                                    Mark Complete
                                  </Button>
                                )}
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
