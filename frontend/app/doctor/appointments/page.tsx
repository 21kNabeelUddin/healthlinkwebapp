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
import Input from '@/components/ui/Input';
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
  FileText,
  ExternalLink,
} from 'lucide-react';
import Link from 'next/link';
import { prescriptionsApi } from '@/lib/api';

interface ClinicWithAppointments extends Clinic {
  appointments: Appointment[];
}

export default function DoctorAppointmentsPage() {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [selectedClinicFilter, setSelectedClinicFilter] = useState<string>('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [prescriptionMap, setPrescriptionMap] = useState<Record<string, boolean>>({}); // appointmentId -> hasPrescription

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
      
      // Check which appointments have prescriptions
      const prescriptionChecks: Record<string, boolean> = {};
      for (const apt of data || []) {
        try {
          const prescription = await prescriptionsApi.getByAppointmentId(apt.id.toString());
          prescriptionChecks[apt.id.toString()] = !!prescription;
        } catch (error) {
          prescriptionChecks[apt.id.toString()] = false;
        }
      }
      setPrescriptionMap(prescriptionChecks);
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

  // Apply search filter for patient name/email
  const filteredAppointmentsBySearch = useMemo(() => {
    const term = searchTerm.toLowerCase().trim();
    if (!term) return appointments;
    return appointments.filter((apt) => {
      const name = (apt.patientName || '').toLowerCase();
      const email = (apt.patientEmail || '').toLowerCase();
      const idStr = apt.patientId ? String(apt.patientId) : '';
      return (
        name.includes(term) ||
        email.includes(term) ||
        idStr.includes(term)
      );
    });
  }, [appointments, searchTerm]);

  // Group appointments by clinic (sorted by soonest appointment first)
  const clinicsWithAppointments = useMemo(() => {
    const sortedAppointments = [...filteredAppointmentsBySearch].sort(
      (a, b) =>
        new Date(a.appointmentDateTime).getTime() -
        new Date(b.appointmentDateTime).getTime()
    );
    const clinicMap = new Map<string, ClinicWithAppointments>();

    // Initialize all clinics - use string keys for consistent comparison
    clinics.forEach((clinic) => {
      clinicMap.set(String(clinic.id), {
        ...clinic,
        appointments: [],
      });
    });

    // Group appointments by clinic - both ONLINE and ONSITE appointments go under their clinic
    sortedAppointments.forEach((appointment) => {
      // If appointment has a clinicId, add it to that clinic (regardless of ONLINE/ONSITE type)
      if (appointment.clinicId) {
        const appointmentClinicId = String(appointment.clinicId);
        const clinic = clinicMap.get(appointmentClinicId);
        
        if (clinic) {
          clinic.appointments.push(appointment);
        } else {
          // Clinic not found in our list - create a temporary entry for it
          // Use 0 as temporary ID since appointment.clinicId is a string (UUID) but Clinic.id is number
          clinicMap.set(appointmentClinicId, {
            id: 0, // Temporary ID for clinics not in our list
            name: appointment.clinicName || 'Unknown Clinic',
            address: appointment.clinicAddress || '',
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
            appointments: [appointment],
          } as ClinicWithAppointments);
        }
      } else {
        // No clinic ID - create an "Unassigned" section for these
        if (!clinicMap.has('unassigned')) {
          clinicMap.set('unassigned', {
            id: 'unassigned' as any,
            name: 'Unassigned Appointments',
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
        }
        clinicMap.get('unassigned')!.appointments.push(appointment);
      }
    });

    // Filter by selected clinic
    let result = Array.from(clinicMap.values());
    if (selectedClinicFilter !== 'all') {
      result = result.filter((c) => String(c.id) === selectedClinicFilter);
    }

    // Only show clinics with appointments
    result = result.filter((c) => c.appointments.length > 0);

    return result.sort((a, b) => {
      // Sort by appointment count (descending)
      return b.appointments.length - a.appointments.length;
    });
  }, [filteredAppointmentsBySearch, clinics, selectedClinicFilter, user?.id]);

  // Helper function to check if meeting can be started (5 minutes before start time)
  const canStartMeeting = (appointmentDateTime: string): boolean => {
    const appointmentTime = new Date(appointmentDateTime);
    const now = new Date();
    const fiveMinutesBefore = new Date(appointmentTime.getTime() - 5 * 60 * 1000);
    return now >= fiveMinutesBefore;
  };

  // Helper function to check if appointment time has started (for prescription creation)
  const hasAppointmentStarted = (appointmentDateTime: string): boolean => {
    const appointmentTime = new Date(appointmentDateTime);
    const now = new Date();
    return now >= appointmentTime;
  };

  // Helper function to check if appointment is active (can be worked on)
  const isActiveAppointment = (status: string): boolean => {
    return status === 'PENDING_PAYMENT' || status === 'CONFIRMED' || status === 'IN_PROGRESS';
  };

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
          // Show confirmation dialog first
          const apt = appointments.find(a => a.id.toString() === appointmentId);
          if (!apt) {
            toast.error('Appointment not found');
            return;
          }

          // Check if prescription exists before completing
          const hasPrescription = prescriptionMap[appointmentId];
          if (!hasPrescription) {
            // Try to fetch prescription one more time
            try {
              const prescription = await prescriptionsApi.getByAppointmentId(appointmentId);
              if (!prescription) {
                toast.error(
                  (t) => (
                    <div className="flex flex-col gap-2 max-w-md">
                      <p className="font-semibold text-base">Prescription Required</p>
                      <p className="text-sm text-slate-700">
                        You must create a prescription before concluding this appointment. Please fill out the prescription form first.
                      </p>
                      <div className="flex gap-2 mt-2">
                        <button
                          onClick={() => {
                            toast.dismiss(t.id);
                            window.location.href = `/doctor/prescriptions/new?appointmentId=${appointmentId}&patientId=${apt.patientId}`;
                          }}
                          className="px-4 py-2 bg-teal-600 text-white rounded-lg text-sm font-medium hover:bg-teal-700 transition-colors"
                        >
                          Create Prescription Now
                        </button>
                        <button
                          onClick={() => toast.dismiss(t.id)}
                          className="px-4 py-2 bg-slate-200 text-slate-700 rounded-lg text-sm font-medium hover:bg-slate-300 transition-colors"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  ),
                  { 
                    duration: 10000,
                    icon: '⚠️',
                  }
                );
                return;
              }
            } catch (error) {
              toast.error(
                (t) => (
                  <div className="flex flex-col gap-2 max-w-md">
                    <p className="font-semibold text-base">Prescription Required</p>
                    <p className="text-sm text-slate-700">
                      You must create a prescription before concluding this appointment. Please fill out the prescription form first.
                    </p>
                    <div className="flex gap-2 mt-2">
                      <button
                        onClick={() => {
                          toast.dismiss(t.id);
                          window.location.href = `/doctor/prescriptions/new?appointmentId=${appointmentId}&patientId=${apt.patientId}`;
                        }}
                        className="px-4 py-2 bg-teal-600 text-white rounded-lg text-sm font-medium hover:bg-teal-700 transition-colors"
                      >
                        Create Prescription Now
                      </button>
                      <button
                        onClick={() => toast.dismiss(t.id)}
                        className="px-4 py-2 bg-slate-200 text-slate-700 rounded-lg text-sm font-medium hover:bg-slate-300 transition-colors"
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                ),
                { 
                  duration: 10000,
                  icon: '⚠️',
                }
              );
              return;
            }
          }

          // Show confirmation dialog
          const confirmed = window.confirm(
            `Are you sure you want to conclude this appointment?\n\n` +
            `Patient: ${apt.patientName}\n` +
            `Date: ${format(new Date(apt.appointmentDateTime), 'MMM dd, yyyy h:mm a')}\n\n` +
            `This action cannot be undone.`
          );

          if (!confirmed) {
            return;
          }

          await appointmentsApi.complete(appointmentId);
          toast.success('Appointment completed successfully');
          loadAppointments(); // Refresh the list
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
      case 'PENDING_PAYMENT':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'COMPLETED':
        return 'bg-teal-100 text-teal-800 border-teal-200';
      case 'NO_SHOW':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
      case 'IN_PROGRESS':
        return <CheckCircle2 className="w-4 h-4" />;
      case 'PENDING_PAYMENT':
        return <AlertCircle className="w-4 h-4" />;
      case 'CANCELLED':
      case 'NO_SHOW':
        return <XCircle className="w-4 h-4" />;
      case 'COMPLETED':
        return <CheckCircle2 className="w-4 h-4" />;
      default:
        return <AlertCircle className="w-4 h-4" />;
    }
  };

  const getStatusLabel = (status: string) => {
    return status
      .split('_')
      .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
      .join(' ');
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
          <Card className="mb-6">
            <div className="p-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="md:col-span-1">
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Search
                  </label>
                  <div className="relative">
                    <Input
                      type="text"
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      placeholder="Search patient name, email, or ID"
                      className="pl-3 pr-3 py-2"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Status
                  </label>
                  <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
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

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Clinic
                  </label>
                  <select
                    value={selectedClinicFilter}
                    onChange={(e) => setSelectedClinicFilter(e.target.value)}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                  >
                    <option value="all">All Clinics ({clinics.length})</option>
                    {clinics.map((clinic) => {
                      const count = appointments.filter((apt) => String(apt.clinicId) === String(clinic.id)).length;
                      return (
                        <option key={clinic.id} value={clinic.id.toString()}>
                          {clinic.name} ({count})
                        </option>
                      );
                    })}
                  </select>
                </div>
              </div>
            </div>
          </Card>

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
                          <Building2 className="w-6 h-6 text-white" />
                        </div>
                        <div className="flex-1">
                          <div className="flex items-center gap-3 mb-2">
                            <h2 className="text-2xl font-bold text-slate-900">{clinic.name}</h2>
                            <Badge variant="default" className="bg-teal-500 text-white">
                              {clinic.appointments.length} appointment{clinic.appointments.length !== 1 ? 's' : ''}
                            </Badge>
                          </div>
                          {clinic.address && (
                            <div className="space-y-1 text-sm text-slate-600">
                              <div className="flex items-center gap-2">
                                <MapPin className="w-4 h-4 text-red-500" />
                                <span>{clinic.address}{clinic.city ? `, ${clinic.city}` : ''}</span>
                              </div>
                              {clinic.openingTime && clinic.closingTime && (
                                <div className="flex items-center gap-2">
                                  <Clock className="w-4 h-4 text-slate-500" />
                                  <span>Opens {clinic.openingTime} • Closes {clinic.closingTime}</span>
                                </div>
                              )}
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
                                      {appointment.patientName || `Patient ID: ${appointment.patientId}`}
                                    </h3>
                                  </div>
                                  <Badge
                                    variant="outline"
                                    className={`${getStatusColor(appointment.status)} border`}
                                  >
                                    {getStatusLabel(appointment.status)}
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

                                {/* Highlighted Date & Time */}
                                <div className="mb-4 p-3 bg-gradient-to-r from-teal-50 to-violet-50 rounded-lg border border-teal-200">
                                  <div className="flex items-center gap-2">
                                    <Calendar className="w-5 h-5 text-teal-600" />
                                    <div>
                                      <p className="text-xs font-medium text-slate-500 uppercase tracking-wide">Date & Time</p>
                                      <p className="text-lg font-bold text-slate-900">
                                        {format(new Date(appointment.appointmentDateTime), 'MMM dd, yyyy')}
                                      </p>
                                      <p className="text-base font-semibold text-teal-700">
                                        {format(new Date(appointment.appointmentDateTime), 'h:mm a')}
                                      </p>
                                    </div>
                                  </div>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm text-slate-600 mb-3">
                                  <div className="flex items-center gap-2">
                                    <User className="w-4 h-4 text-slate-400" />
                                    <span>
                                      <strong>Patient:</strong>{' '}
                                      {appointment.patientName || `Patient ID: ${appointment.patientId}`}
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

                                {/* Zoom Meeting Button - Show for ONLINE appointments */}
                                {appointment.appointmentType === 'ONLINE' && isActiveAppointment(appointment.status) && (
                                  <div className="mt-3">
                                    {appointment.zoomStartUrl ? (
                                      canStartMeeting(appointment.appointmentDateTime) ? (
                                        <a
                                          href={appointment.zoomStartUrl}
                                          target="_blank"
                                          rel="noopener noreferrer"
                                          className="inline-flex items-center gap-2 bg-gradient-to-r from-teal-500 to-violet-600 text-white px-4 py-2 rounded-lg font-medium hover:from-teal-600 hover:to-violet-700 transition-all shadow-md hover:shadow-lg"
                                        >
                                          <Video className="w-5 h-5" />
                                          Start Zoom Meeting
                                          <ExternalLink className="w-4 h-4" />
                                        </a>
                                      ) : (
                                        <div className="inline-flex items-center gap-2 bg-slate-200 text-slate-600 px-4 py-2 rounded-lg font-medium cursor-not-allowed">
                                          <Clock className="w-5 h-5" />
                                          <span>Meeting available 5 minutes before start time ({format(new Date(appointment.appointmentDateTime), 'MMM dd, h:mm a')})</span>
                                        </div>
                                      )
                                    ) : (
                                      <div className="inline-flex items-center gap-2 bg-amber-100 text-amber-800 px-4 py-2 rounded-lg font-medium border border-amber-300">
                                        <AlertCircle className="w-5 h-5" />
                                        <span>Zoom meeting link will be available soon</span>
                                      </div>
                                    )}
                                  </div>
                                )}
                                
                                {/* Prescription Status */}
                                {prescriptionMap[appointment.id.toString()] ? (
                                  <div className="mt-2 flex items-center gap-2 text-sm text-green-600">
                                    <CheckCircle2 className="w-4 h-4" />
                                    <span>Prescription created</span>
                                  </div>
                                ) : isActiveAppointment(appointment.status) && (
                                  <div className="mt-2 flex items-center gap-2 text-sm text-amber-600">
                                    <AlertCircle className="w-4 h-4" />
                                    <span>Prescription required</span>
                                  </div>
                                )}
                              </div>

                              {/* Actions */}
                              <div className="flex flex-col gap-2 ml-4">
                                {/* Show for PENDING_PAYMENT, CONFIRMED, and IN_PROGRESS statuses */}
                                {isActiveAppointment(appointment.status) && (
                                  <>
                                    {hasAppointmentStarted(appointment.appointmentDateTime) ? (
                                      <Link href={`/doctor/prescriptions/new?appointmentId=${appointment.id}&patientId=${appointment.patientId}`}>
                                        <Button
                                          variant="primary"
                                          className="w-full text-sm px-3 py-1.5"
                                        >
                                          <FileText className="w-4 h-4 mr-2" />
                                          {prescriptionMap[appointment.id.toString()] ? 'Edit Prescription' : 'Create Prescription'}
                                        </Button>
                                      </Link>
                                    ) : (
                                      <Button
                                        variant="secondary"
                                        className="w-full text-sm px-3 py-1.5 cursor-not-allowed"
                                        disabled
                                        title={`Prescription can only be created after the appointment starts (${format(new Date(appointment.appointmentDateTime), 'MMM dd, h:mm a')})`}
                                      >
                                        <FileText className="w-4 h-4 mr-2" />
                                        Create Prescription
                                        <span className="ml-2 text-xs">(Available after appointment starts)</span>
                                      </Button>
                                    )}
                                    <Button
                                      variant="primary"
                                      onClick={() => handleAppointmentAction(appointment.id.toString(), 'complete')}
                                      className="w-full text-sm px-3 py-1.5"
                                    >
                                      <CheckCircle2 className="w-4 h-4 mr-2" />
                                      Conclude Appointment
                                    </Button>
                                  </>
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
