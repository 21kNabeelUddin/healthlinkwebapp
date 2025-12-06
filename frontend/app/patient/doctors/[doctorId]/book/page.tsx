'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { useForm } from 'react-hook-form';
import { appointmentsApi, facilitiesApi, patientApi } from '@/lib/api';
import { AppointmentRequest, Doctor, Clinic } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import { Badge } from '@/marketing/ui/badge';
import {
  ArrowLeft,
  Stethoscope,
  MapPin,
  Clock,
  Calendar,
  Video,
  Building2,
  Star,
  Phone,
  Mail,
} from 'lucide-react';

interface BookingFormData {
  appointmentType: 'ONLINE' | 'ONSITE';
  clinicId?: string | number; // Can be UUID string or number
  appointmentDateTime: string;
  reason: string;
  notes?: string;
}

export default function BookAppointmentPage() {
  const router = useRouter();
  const params = useParams();
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const doctorId = params?.doctorId as string;
  const clinicIdFromQuery = searchParams?.get('clinicId');

  const [doctor, setDoctor] = useState<Doctor | null>(null);
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [selectedClinic, setSelectedClinic] = useState<Clinic | null>(null);
  const [isLoadingDoctor, setIsLoadingDoctor] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<BookingFormData>({
    defaultValues: {
      appointmentType: 'ONSITE',
      clinicId: clinicIdFromQuery || undefined,
    },
  });

  const appointmentType = watch('appointmentType');
  const selectedClinicId = watch('clinicId');

  useEffect(() => {
    if (doctorId) {
      loadDoctor();
      loadClinics();
    }
  }, [doctorId]);

  // Clear clinicId when switching to ONLINE appointment type
  useEffect(() => {
    if (appointmentType === 'ONLINE') {
      setValue('clinicId', undefined);
      setSelectedClinic(null);
    }
  }, [appointmentType, setValue]);

  const loadDoctor = async () => {
    setIsLoadingDoctor(true);
    try {
      const response = await patientApi.getDoctors();
      let doctorsList: Doctor[] = [];
      if (Array.isArray(response)) {
        doctorsList = response;
      } else if (response && typeof response === 'object' && 'data' in response) {
        const data = (response as any).data;
        if (Array.isArray(data)) {
          doctorsList = data;
        }
      }
      const foundDoctor = doctorsList.find((d) => d.id.toString() === doctorId);
      if (foundDoctor) {
        setDoctor(foundDoctor);
      } else {
        toast.error('Doctor not found');
        router.push('/patient/doctors');
      }
    } catch (error: any) {
      toast.error('Failed to load doctor information');
      router.push('/patient/doctors');
    } finally {
      setIsLoadingDoctor(false);
    }
  };

  const loadClinics = async () => {
    try {
      const data = await facilitiesApi.listForDoctor(doctorId);
      const activeClinics = data.filter((c) => c.active);
      setClinics(activeClinics || []);
      
      // If clinicId is in query params, set it as selected
      if (clinicIdFromQuery) {
        const clinic = activeClinics.find((c) => c.id.toString() === clinicIdFromQuery);
        if (clinic) {
          setSelectedClinic(clinic);
          setValue('clinicId', clinic.id, { shouldValidate: true });
        }
      }
    } catch (error: any) {
      // Doctor might not have clinics yet, that's okay
      setClinics([]);
    }
  };

  useEffect(() => {
    if (selectedClinicId && clinics.length > 0) {
      const clinic = clinics.find((c) => c.id === selectedClinicId);
      setSelectedClinic(clinic || null);
    }
  }, [selectedClinicId, clinics]);

  const onSubmit = async (data: BookingFormData) => {
    if (!user?.id || !doctorId) return;

    // Determine facilityId based on appointment type
    let facilityId: string | undefined;
    
    if (data.appointmentType === 'ONSITE') {
      // For on-site visits, clinic selection is required
      if (!data.clinicId) {
        toast.error('Please select a clinic for on-site visits');
        return;
      }
      
      // Validate clinicId is valid (not empty string, not NaN)
      const clinicIdStr = String(data.clinicId).trim();
      if (!clinicIdStr || clinicIdStr === 'NaN' || clinicIdStr === 'undefined' || clinicIdStr === '') {
        toast.error('Please select a clinic for on-site visits');
        return;
      }
      
      // Find the clinic to ensure it exists
      const selectedClinicObj = clinics.find((c) => c.id.toString() === clinicIdStr);
      if (!selectedClinicObj) {
        toast.error('Selected clinic not found. Please select a clinic again.');
        return;
      }
      
      // Convert clinic ID to string (backend expects UUID string format)
      facilityId = String(selectedClinicObj.id);
    } else {
      // For online appointments, use the first available clinic
      // (Backend requires facilityId even for online appointments)
      if (clinics.length === 0) {
        toast.error('This doctor has no clinics available. Please contact the doctor directly.');
        return;
      }
      facilityId = String(clinics[0].id);
    }

    // Final validation that facilityId is valid
    if (!facilityId || facilityId === 'NaN' || facilityId === 'undefined' || facilityId.trim() === '') {
      toast.error('Invalid clinic selection. Please try again.');
      return;
    }

    // Validate appointment time is in the future
    if (!data.appointmentDateTime) {
      toast.error('Please select a date and time for your appointment');
      return;
    }

    const appointmentDate = new Date(data.appointmentDateTime);
    const now = new Date();
    
    // Add 1 minute buffer to account for time selection
    if (appointmentDate <= now) {
      toast.error('Appointment time must be in the future. Please select a future date and time.');
      return;
    }

    setIsSubmitting(true);
    try {
      // Map to backend CreateAppointmentRequest format
      // Backend expects: doctorId (UUID string), facilityId (UUID string), appointmentTime (ISO datetime string), reasonForVisit (String), isEmergency (Boolean), type (String)
      const appointmentData: any = {
        doctorId: doctorId, // Already a UUID string from URL params
        facilityId: facilityId, // Converted to string above
        appointmentTime: data.appointmentDateTime, // ISO datetime string
        reasonForVisit: data.reason,
        isEmergency: false,
        type: data.appointmentType, // "ONLINE" or "ONSITE"
      };
      
      // Include notes if provided (but backend will prepend appointment type to it)
      if (data.notes) {
        appointmentData.notes = data.notes;
      }

      await appointmentsApi.create(appointmentData);
      toast.success('Appointment booked successfully!');
      router.push('/patient/appointments');
    } catch (error: any) {
      // Extract error message from backend response
      let errorMessage = 'Failed to book appointment';
      
      if (error.response?.data) {
        // Try different possible error message fields
        errorMessage = error.response.data.message || 
                      error.response.data.error || 
                      error.response.data.detail ||
                      (typeof error.response.data === 'string' ? error.response.data : errorMessage);
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      // Show user-friendly error message
      toast.error(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoadingDoctor) {
    return (
      <DashboardLayout requiredUserType="PATIENT">
        <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
          <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="text-center py-20">
              <div className="inline-block animate-spin rounded-full h-12 w-12 border-4 border-teal-500 border-t-transparent"></div>
              <p className="mt-4 text-slate-600">Loading doctor information...</p>
            </div>
          </div>
        </div>
      </DashboardLayout>
    );
  }

  if (!doctor) {
    return null;
  }

  const rating = doctor.averageRating || 0;
  const reviewCount = doctor.totalReviews || 0;
  const primaryClinic = clinics[0];
  const consultationFee = primaryClinic?.consultationFee || doctor.consultationFee || 0;

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* Back Button */}
          <Link
            href={`/patient/doctors/${doctorId}`}
            className="inline-flex items-center gap-2 text-teal-600 hover:text-teal-700 text-sm font-medium mb-6"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Doctor Profile
          </Link>

          {/* Doctor Info Card */}
          <Card className="mb-6 bg-gradient-to-br from-teal-50 to-violet-50 border-teal-200">
            <div className="flex flex-col md:flex-row gap-6">
              {/* Avatar */}
              <div className="w-24 h-24 bg-gradient-to-br from-teal-500 to-violet-600 rounded-2xl flex items-center justify-center flex-shrink-0">
                <Stethoscope className="w-12 h-12 text-white" />
              </div>

              {/* Doctor Details */}
              <div className="flex-1">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h1 className="text-3xl font-bold text-slate-900 mb-2">
                      Dr. {doctor.firstName} {doctor.lastName}
                    </h1>
                    <Badge variant="secondary" className="text-sm mb-3">
                      {doctor.specialization}
                    </Badge>
                    {rating > 0 && (
                      <div className="flex items-center gap-2 mb-2">
                        <div className="flex items-center gap-1">
                          <Star className="w-5 h-5 fill-yellow-400 text-yellow-400" />
                          <span className="font-semibold text-slate-900">{rating.toFixed(1)}</span>
                        </div>
                        {reviewCount > 0 && (
                          <span className="text-slate-600 text-sm">({reviewCount} reviews)</span>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                {/* Contact Info */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm text-slate-600">
                  {doctor.phoneNumber && (
                    <div className="flex items-center gap-2">
                      <Phone className="w-4 h-4 text-teal-600" />
                      <span>{doctor.phoneNumber}</span>
                    </div>
                  )}
                  {doctor.email && (
                    <div className="flex items-center gap-2">
                      <Mail className="w-4 h-4 text-teal-600" />
                      <span>{doctor.email}</span>
                    </div>
                  )}
                  {doctor.yearsOfExperience > 0 && (
                    <div className="flex items-center gap-2">
                      <Stethoscope className="w-4 h-4 text-teal-600" />
                      <span>{doctor.yearsOfExperience} years of experience</span>
                    </div>
                  )}
                </div>

                {/* Selected Clinic Info (if clinic is pre-selected) */}
                {selectedClinic && (
                  <div className="mt-4 pt-4 border-t border-slate-200">
                    <div className="flex items-start gap-2 mb-2">
                      <Building2 className="w-5 h-5 text-teal-600 mt-0.5" />
                      <div className="flex-1">
                        <h3 className="font-semibold text-slate-900 mb-1">{selectedClinic.name}</h3>
                        <div className="flex items-start gap-2 text-sm text-slate-600 mb-2">
                          <MapPin className="w-4 h-4 mt-0.5 text-teal-600" />
                          <span>
                            {selectedClinic.address}, {selectedClinic.city}
                          </span>
                        </div>
                        <div className="flex items-center gap-2 text-sm text-slate-600">
                          <Clock className="w-4 h-4 text-teal-600" />
                          <span>
                            Opens {selectedClinic.openingTime} • Closes {selectedClinic.closingTime}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* Consultation Fee */}
                {consultationFee > 0 && (
                  <div className="mt-4 bg-gradient-to-br from-blue-500 to-cyan-500 text-white p-4 rounded-lg inline-block">
                    <div className="text-xs opacity-90 mb-1">Consultation Fee</div>
                    <div className="text-2xl font-bold">PKR {consultationFee.toLocaleString()}</div>
                  </div>
                )}
              </div>
            </div>
          </Card>

          {/* Booking Form */}
          <Card>
            <div className="mb-6">
              <h2 className="text-2xl font-bold text-slate-900 mb-2">Book Appointment</h2>
              <p className="text-slate-600">Fill in the details below to schedule your appointment</p>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Appointment Type */}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-3">
                  Appointment Type *
                </label>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <label
                    className={`relative flex items-center p-4 border-2 rounded-xl cursor-pointer transition-all ${
                      appointmentType === 'ONSITE'
                        ? 'border-teal-500 bg-teal-50'
                        : 'border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <input
                      type="radio"
                      value="ONSITE"
                      {...register('appointmentType', { required: 'Appointment type is required' })}
                      className="sr-only"
                    />
                    <div className="flex items-center gap-3">
                      <Building2
                        className={`w-6 h-6 ${
                          appointmentType === 'ONSITE' ? 'text-teal-600' : 'text-slate-400'
                        }`}
                      />
                      <div>
                        <div className="font-semibold text-slate-900">On-site Visit</div>
                        <div className="text-sm text-slate-600">Visit the clinic in person</div>
                      </div>
                    </div>
                  </label>

                  <label
                    className={`relative flex items-center p-4 border-2 rounded-xl cursor-pointer transition-all ${
                      appointmentType === 'ONLINE'
                        ? 'border-teal-500 bg-teal-50'
                        : 'border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <input
                      type="radio"
                      value="ONLINE"
                      {...register('appointmentType', { required: 'Appointment type is required' })}
                      className="sr-only"
                    />
                    <div className="flex items-center gap-3">
                      <Video
                        className={`w-6 h-6 ${
                          appointmentType === 'ONLINE' ? 'text-teal-600' : 'text-slate-400'
                        }`}
                      />
                      <div>
                        <div className="font-semibold text-slate-900">Online Consultation</div>
                        <div className="text-sm text-slate-600">Video call via Zoom</div>
                      </div>
                    </div>
                  </label>
                </div>
                {errors.appointmentType && (
                  <p className="mt-2 text-sm text-red-600">{errors.appointmentType.message}</p>
                )}
              </div>

              {/* Clinic Selection (required for on-site) */}
              {appointmentType === 'ONSITE' && (
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Select Clinic *
                  </label>
                  {clinics.length === 0 ? (
                    <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                      <p className="text-sm text-yellow-800">
                        This doctor has no active clinics. Please select "Online Consultation" or contact the doctor directly.
                      </p>
                    </div>
                  ) : (
                    <>
                      <select
                        {...register('clinicId', {
                          required: 'Clinic is required for on-site visits',
                          validate: (value) => {
                            if (!value || String(value).trim() === '') {
                              return 'Clinic is required for on-site visits';
                            }
                            return true;
                          },
                        })}
                        value={selectedClinicId ? String(selectedClinicId) : ''}
                        onChange={(e) => {
                          const clinicIdValue = e.target.value;
                          setValue('clinicId', clinicIdValue, { shouldValidate: true });
                          const clinic = clinics.find((c) => c.id.toString() === clinicIdValue);
                          setSelectedClinic(clinic || null);
                        }}
                        className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                      >
                        <option value="">Select a clinic</option>
                        {clinics.map((clinic) => (
                          <option key={clinic.id} value={String(clinic.id)}>
                            {clinic.name} - {clinic.address}, {clinic.city}
                            {clinic.consultationFee && ` (PKR ${clinic.consultationFee.toLocaleString()})`}
                          </option>
                        ))}
                      </select>
                      {errors.clinicId && (
                        <p className="mt-2 text-sm text-red-600">{errors.clinicId.message}</p>
                      )}
                      
                      {/* Show Selected Clinic Hours */}
                      {selectedClinic && (
                        <div className="mt-3 p-4 bg-teal-50 border border-teal-200 rounded-lg">
                          <div className="flex items-center gap-2 mb-3">
                            <Building2 className="w-5 h-5 text-teal-600" />
                            <h4 className="font-semibold text-slate-900">{selectedClinic.name}</h4>
                          </div>
                          <div className="space-y-2 text-sm text-slate-700">
                            <div className="flex items-center gap-2">
                              <MapPin className="w-4 h-4 text-teal-600" />
                              <span>{selectedClinic.address}, {selectedClinic.city}</span>
                            </div>
                            <div className="flex items-center gap-2">
                              <Clock className="w-4 h-4 text-teal-600" />
                              <span>
                                <strong>Operating Hours:</strong> Opens {selectedClinic.openingTime} • Closes {selectedClinic.closingTime}
                              </span>
                            </div>
                            {selectedClinic.consultationFee && selectedClinic.consultationFee > 0 && (
                              <div className="flex items-center gap-2">
                                <span className="font-semibold">Consultation Fee:</span>
                                <span className="text-teal-600 font-bold">PKR {selectedClinic.consultationFee.toLocaleString()}</span>
                              </div>
                            )}
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}

              {/* Date & Time */}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  <Calendar className="w-4 h-4 inline mr-2" />
                  Date & Time *
                </label>
                <Input
                  type="datetime-local"
                  {...register('appointmentDateTime', { 
                    required: 'Date and time is required',
                    validate: (value) => {
                      if (!value) return 'Date and time is required';
                      const selectedDate = new Date(value);
                      const now = new Date();
                      // Allow appointments at least 1 minute in the future
                      if (selectedDate <= now) {
                        return 'Appointment time must be in the future';
                      }
                      return true;
                    }
                  })}
                  min={new Date(Date.now() + 60000).toISOString().slice(0, 16)} // Minimum 1 minute from now
                  error={errors.appointmentDateTime?.message}
                  className="w-full"
                />
                <p className="mt-2 text-xs text-slate-500">
                  Please select a future date and time for your appointment
                </p>
              </div>

              {/* Reason for Appointment */}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Reason for Appointment *
                </label>
                <textarea
                  {...register('reason', {
                    required: 'Reason is required',
                    minLength: {
                      value: 10,
                      message: 'Please provide at least 10 characters',
                    },
                  })}
                  rows={4}
                  placeholder="Describe the reason for your visit..."
                  className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 resize-none"
                />
                {errors.reason && (
                  <p className="mt-2 text-sm text-red-600">{errors.reason.message}</p>
                )}
              </div>

              {/* Notes */}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Additional Notes (Optional)
                </label>
                <textarea
                  {...register('notes')}
                  rows={3}
                  placeholder="Any additional information you'd like to share..."
                  className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 resize-none"
                />
              </div>

              {/* Submit Buttons */}
              <div className="flex gap-4 pt-4 border-t border-slate-200">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => router.push('/patient/doctors')}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button type="submit" isLoading={isSubmitting} className="flex-1 bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700">
                  Book Appointment
                </Button>
              </div>
            </form>
          </Card>
        </div>
      </div>
    </DashboardLayout>
  );
}

