'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useForm } from 'react-hook-form';
import { appointmentsApi, facilitiesApi } from '@/lib/api';
import { patientApi } from '@/lib/api';
import { AppointmentRequest, Doctor, Clinic } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';

export default function BookAppointmentPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedDoctorId, setSelectedDoctorId] = useState<number | null>(
    searchParams.get('doctorId') ? parseInt(searchParams.get('doctorId')!) : null
  );

  const { register, handleSubmit, watch, formState: { errors } } = useForm<AppointmentRequest & { doctorId: number }>();

  const appointmentType = watch('appointmentType');

  useEffect(() => {
    loadDoctors();
  }, []);

  useEffect(() => {
    if (selectedDoctorId) {
      loadClinics(selectedDoctorId.toString());
    }
  }, [selectedDoctorId]);

  const loadDoctors = async () => {
    try {
      const response = await patientApi.getDoctors();
      if (response.success && response.data) {
        setDoctors(response.data);
      }
    } catch (error: any) {
      toast.error('Failed to load doctors');
    }
  };

  const loadClinics = async (doctorId: string) => {
    try {
      const data = await facilitiesApi.listForDoctor(doctorId);
      setClinics(data || []);
    } catch (error: any) {
      // Doctor might not have clinics yet, that's okay
    }
  };

  const onSubmit = async (data: AppointmentRequest & { doctorId: number }) => {
    if (!user?.id) return;

    setIsLoading(true);
    try {
      const appointmentData = {
        patientId: user.id.toString(),
        doctorId: data.doctorId.toString(),
        facilityId: data.clinicId?.toString(),
        scheduledAt: data.appointmentDateTime,
        consultationType: data.appointmentType === 'ONLINE' ? 'VIDEO_CALL' : 'IN_PERSON',
        chiefComplaint: data.reason,
        notes: data.notes,
      };

      await appointmentsApi.create(appointmentData);
      toast.success('Appointment booked successfully!');
      router.push('/patient/appointments');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to book appointment');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-4xl font-bold text-gray-800 mb-8">Book Appointment</h1>

        <Card>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Select Doctor *
              </label>
              <select
                {...register('doctorId', { required: 'Doctor is required', valueAsNumber: true })}
                onChange={(e) => setSelectedDoctorId(parseInt(e.target.value))}
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value="">Select a doctor</option>
                {doctors.map((doctor) => (
                  <option key={doctor.id} value={doctor.id}>
                    {doctor.firstName} {doctor.lastName} - {doctor.specialization}
                  </option>
                ))}
              </select>
              {errors.doctorId && (
                <p className="mt-1 text-sm text-red-600">{errors.doctorId.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Appointment Type *
              </label>
              <select
                {...register('appointmentType', { required: 'Appointment type is required' })}
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value="">Select type</option>
                <option value="ONLINE">Online (Zoom)</option>
                <option value="ONSITE">Onsite</option>
              </select>
              {errors.appointmentType && (
                <p className="mt-1 text-sm text-red-600">{errors.appointmentType.message}</p>
              )}
            </div>

            {appointmentType === 'ONSITE' && selectedDoctorId && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Select Clinic
                </label>
                <select
                  {...register('clinicId', { valueAsNumber: true })}
                  className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option value="">Select a clinic (optional)</option>
                  {clinics.map((clinic) => (
                    <option key={clinic.id} value={clinic.id}>
                      {clinic.name} - {clinic.address}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <Input
              label="Date & Time *"
              type="datetime-local"
              {...register('appointmentDateTime', { required: 'Date and time is required' })}
              error={errors.appointmentDateTime?.message}
            />

            <Input
              label="Reason *"
              {...register('reason', { required: 'Reason is required' })}
              error={errors.reason?.message}
            />

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Notes
              </label>
              <textarea
                {...register('notes')}
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                rows={3}
              />
            </div>

            <div className="flex gap-4">
              <Button type="submit" isLoading={isLoading} className="flex-1">
                Book Appointment
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => router.back()}
                className="flex-1"
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

