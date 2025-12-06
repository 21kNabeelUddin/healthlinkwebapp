'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useForm } from 'react-hook-form';
import { doctorApi, facilitiesApi } from '@/lib/api';
import { Clinic, CreateEmergencyPatientRequest, CreateEmergencyPatientAndAppointmentRequest } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';

interface EmergencyPatientForm {
  patientName: string;
  email: string;
  phoneNumber?: string;
  createAppointment: boolean;
  facilityId?: string;
  appointmentTime?: string;
  reasonForVisit?: string;
}

export default function NewEmergencyPatientPage() {
  const router = useRouter();
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [createdPatient, setCreatedPatient] = useState<{
    email: string;
    patientName: string;
  } | null>(null);
  const { register, handleSubmit, watch, formState: { errors } } = useForm<EmergencyPatientForm>({
    defaultValues: {
      createAppointment: false,
    },
  });

  const createAppointment = watch('createAppointment');

  useEffect(() => {
    if (user?.id) {
      loadClinics();
    }
  }, [user?.id]);

  const loadClinics = async () => {
    if (!user?.id) return;
    try {
      const data = await facilitiesApi.listForDoctor(user.id.toString());
      setClinics(data || []);
    } catch (error: any) {
      console.error('Failed to load clinics:', error);
    }
  };

  const onSubmit = async (data: EmergencyPatientForm) => {
    if (!user?.id) return;

    setIsLoading(true);
    try {
      if (data.createAppointment && data.facilityId && data.appointmentTime && data.reasonForVisit) {
        // Create patient + appointment in one call
        const request: CreateEmergencyPatientAndAppointmentRequest = {
          patientName: data.patientName,
          email: data.email,
          phoneNumber: data.phoneNumber || undefined,
          appointmentRequest: {
            doctorId: user.id.toString(),
            facilityId: data.facilityId,
            appointmentTime: data.appointmentTime,
            reasonForVisit: data.reasonForVisit,
            isEmergency: true,
          },
        };

        const response = await doctorApi.createEmergencyPatientAndAppointment(user.id.toString(), request);
        setCreatedPatient({
          email: response.patient.email,
          patientName: response.patient.patientName,
        });
        toast.success('Emergency patient and appointment created successfully!');
      } else {
        // Create patient only
        const request: CreateEmergencyPatientRequest = {
          patientName: data.patientName,
          email: data.email,
          phoneNumber: data.phoneNumber || undefined,
        };

        const response = await doctorApi.createEmergencyPatient(user.id.toString(), request);
        setCreatedPatient({
          email: response.email,
          patientName: response.patientName,
        });
        toast.success('Emergency patient created successfully!');
      }
    } catch (error: any) {
      if (error.response?.status === 401) {
        toast.error('Your session has expired. Please log in again.');
        setTimeout(() => router.push('/auth/doctor/login'), 800);
      } else {
        toast.error(error.response?.data?.message || 'Failed to create emergency patient');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateAnother = () => {
    setCreatedPatient(null);
    // Reset form would require react-hook-form reset, but for simplicity we'll just clear the state
  };

  if (createdPatient) {
    return (
      <DashboardLayout requiredUserType="DOCTOR">
        <div className="max-w-2xl mx-auto">
          <h1 className="text-4xl font-bold text-gray-800 mb-8">Emergency Patient Created</h1>

          <Card>
            <div className="space-y-6">
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <p className="text-green-800 font-semibold mb-2">✓ Patient account created successfully</p>
                <p className="text-sm text-green-700">
                  A welcome email has been sent to the patient with instructions on how to reset their password and access their account.
                </p>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Patient Name</label>
                  <p className="text-gray-900 font-semibold">{createdPatient.patientName}</p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                  <p className="text-gray-900 font-mono text-sm bg-gray-50 p-2 rounded border">
                    {createdPatient.email}
                  </p>
                </div>

                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <p className="text-sm text-blue-800">
                    <strong>Note:</strong> The patient should check their email for instructions on how to reset their password and log in to their account.
                  </p>
                </div>
              </div>

              <div className="flex gap-3 pt-4 border-t">
                <Button onClick={handleCreateAnother} variant="outline">
                  Create Another Patient
                </Button>
                <Button onClick={() => router.push('/doctor/appointments')} variant="primary">
                  View Appointments
                </Button>
                <Button onClick={() => router.push('/doctor/dashboard')} variant="outline">
                  Back to Dashboard
                </Button>
              </div>
            </div>
          </Card>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Create Emergency Patient</h1>
        <p className="text-gray-600 mb-8">
          Create a patient account instantly for emergency cases. The patient will receive an email with instructions to reset their password.
        </p>

        <Card>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div className="space-y-4">
              <Input
                label="Patient Name *"
                {...register('patientName', { required: 'Patient name is required' })}
                error={errors.patientName?.message}
                placeholder="Enter patient's full name"
              />

              <Input
                label="Email *"
                type="email"
                {...register('email', { 
                  required: 'Email is required',
                  pattern: {
                    value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                    message: 'Invalid email address'
                  }
                })}
                error={errors.email?.message}
                placeholder="patient@example.com"
              />

              <Input
                label="Phone Number"
                {...register('phoneNumber')}
                error={errors.phoneNumber?.message}
                placeholder="+923001234567 (optional)"
              />

              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="createAppointment"
                  {...register('createAppointment')}
                  className="w-4 h-4 text-primary-600 rounded"
                />
                <label htmlFor="createAppointment" className="text-sm font-medium text-gray-700">
                  Also create an emergency appointment
                </label>
              </div>
            </div>

            {createAppointment && (
              <div className="space-y-4 pt-4 border-t border-gray-200">
                <h3 className="text-lg font-semibold text-gray-800">Appointment Details</h3>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Clinic/Facility *
                  </label>
                  <select
                    {...register('facilityId', {
                      required: createAppointment ? 'Please select a clinic' : false,
                    })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                  >
                    <option value="">Select a clinic</option>
                    {clinics
                      .filter((c) => c.active)
                      .map((clinic) => (
                        <option key={clinic.id} value={clinic.id.toString()}>
                          {clinic.name} - {clinic.address}
                        </option>
                      ))}
                  </select>
                  {errors.facilityId && (
                    <p className="text-red-600 text-sm mt-1">{errors.facilityId.message}</p>
                  )}
                </div>

                <Input
                  label="Appointment Time *"
                  type="datetime-local"
                  {...register('appointmentTime', {
                    required: createAppointment ? 'Appointment time is required' : false,
                  })}
                  error={errors.appointmentTime?.message}
                />

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Reason for Visit *
                  </label>
                  <textarea
                    {...register('reasonForVisit', {
                      required: createAppointment ? 'Reason for visit is required' : false,
                    })}
                    rows={3}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                    placeholder="Describe the emergency or reason for visit"
                  />
                  {errors.reasonForVisit && (
                    <p className="text-red-600 text-sm mt-1">{errors.reasonForVisit.message}</p>
                  )}
                </div>

                <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                  <p className="text-sm text-blue-800">
                    <strong>Note:</strong> Emergency appointments bypass availability checks and can be scheduled
                    immediately or within 5 minutes.
                  </p>
                </div>
              </div>
            )}

            <div className="flex gap-3 pt-4">
              <Button type="submit" variant="primary" disabled={isLoading} className="flex-1">
                {isLoading ? 'Creating...' : 'Create Emergency Patient'}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => router.push('/doctor/dashboard')}
              >
                Cancel
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </DashboardLayout>
  );
}

