'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useForm } from 'react-hook-form';
import { medicalRecordsApi } from '@/lib/api';
import { MedicalHistoryRequest } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';

export default function NewMedicalHistoryPage() {
  const router = useRouter();
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm<MedicalHistoryRequest>();

  const onSubmit = async (data: MedicalHistoryRequest) => {
    if (!user?.id) return;

    setIsLoading(true);
    try {
      await medicalRecordsApi.create({
        patientId: user.id.toString(),
        title: data.condition,
        summary: data.description,
        details: `${data.treatment}\n\nMedications: ${data.medications}\nDoctor: ${data.doctorName}\nHospital: ${data.hospitalName}`,
        recordDate: data.diagnosisDate,
        recordType: 'CONSULTATION',
      });

      toast.success('Medical history added successfully!');
      router.push('/patient/medical-history');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to add medical history');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-4xl font-bold text-gray-800 mb-8">Add Medical History</h1>

        <Card>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Condition *"
              {...register('condition', { required: 'Condition is required' })}
              error={errors.condition?.message}
            />

            <Input
              label="Diagnosis Date *"
              type="date"
              {...register('diagnosisDate', { required: 'Diagnosis date is required' })}
              error={errors.diagnosisDate?.message}
            />

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description *
              </label>
              <textarea
                {...register('description', { required: 'Description is required' })}
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                rows={3}
              />
              {errors.description && (
                <p className="mt-1 text-sm text-red-600">{errors.description.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Treatment *
              </label>
              <textarea
                {...register('treatment', { required: 'Treatment is required' })}
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                rows={3}
              />
              {errors.treatment && (
                <p className="mt-1 text-sm text-red-600">{errors.treatment.message}</p>
              )}
            </div>

            <Input
              label="Medications *"
              {...register('medications', { required: 'Medications is required' })}
              error={errors.medications?.message}
            />

            <Input
              label="Doctor Name *"
              {...register('doctorName', { required: 'Doctor name is required' })}
              error={errors.doctorName?.message}
            />

            <Input
              label="Hospital Name *"
              {...register('hospitalName', { required: 'Hospital name is required' })}
              error={errors.hospitalName?.message}
            />

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Status *
              </label>
              <select
                {...register('status', { required: 'Status is required' })}
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value="">Select status</option>
                <option value="ACTIVE">Active</option>
                <option value="RESOLVED">Resolved</option>
                <option value="CHRONIC">Chronic</option>
                <option value="UNDER_TREATMENT">Under Treatment</option>
              </select>
              {errors.status && (
                <p className="mt-1 text-sm text-red-600">{errors.status.message}</p>
              )}
            </div>

            <div className="flex gap-4">
              <Button type="submit" isLoading={isLoading} className="flex-1">
                Add Medical History
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

