'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useForm } from 'react-hook-form';
import { facilitiesApi } from '@/lib/api';
import { ClinicRequest } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';

export default function EditClinicPage() {
  const router = useRouter();
  const params = useParams();
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingData, setIsLoadingData] = useState(true);
  const clinicId = params.id as string;

  const { register, handleSubmit, formState: { errors }, setValue } = useForm<ClinicRequest>();

  useEffect(() => {
    if (user?.id) {
      loadClinic();
    }
  }, [user, clinicId]);

  const loadClinic = async () => {
    if (!user?.id) return;
    
    setIsLoadingData(true);
    try {
      const clinics = await facilitiesApi.listForDoctor(user.id.toString());
      const clinic = clinics.find((c: any) => c.id === clinicId);
      if (clinic) {
        setValue('name', clinic.name);
        setValue('address', clinic.address);
        setValue('city', clinic.city);
        setValue('state', clinic.province || clinic.state);
        setValue('zipCode', clinic.zipCode || '');
        setValue('phoneNumber', clinic.phone || '');
        setValue('email', clinic.email || '');
        setValue('description', clinic.description || '');
        setValue('openingTime', clinic.openingHours?.monday?.split('-')[0] || '09:00');
        setValue('closingTime', clinic.openingHours?.monday?.split('-')[1] || '17:00');
      } else {
        toast.error('Clinic not found');
        router.push('/doctor/clinics');
      }
    } catch (error: any) {
      toast.error('Failed to load clinic');
      router.push('/doctor/clinics');
    } finally {
      setIsLoadingData(false);
    }
  };

  const onSubmit = async (data: ClinicRequest) => {
    if (!user?.id) return;

    setIsLoading(true);
    try {
      await facilitiesApi.update(clinicId, {
        ...data,
        facilityType: 'CLINIC',
        province: data.state,
      });

      toast.success('Clinic updated successfully!');
      router.push('/doctor/clinics');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to update clinic');
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoadingData) {
    return (
      <DashboardLayout requiredUserType="DOCTOR">
        <div className="text-center">Loading...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-4xl font-bold text-gray-800 mb-8">Edit Clinic</h1>

        <Card>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Clinic Name *"
              {...register('name', { required: 'Clinic name is required' })}
              error={errors.name?.message}
            />

            <Input
              label="Address *"
              {...register('address', { required: 'Address is required' })}
              error={errors.address?.message}
            />

            <div className="grid grid-cols-2 gap-4">
              <Input
                label="City *"
                {...register('city', { required: 'City is required' })}
                error={errors.city?.message}
              />

              <Input
                label="State *"
                {...register('state', { required: 'State is required' })}
                error={errors.state?.message}
              />
            </div>

            <Input
              label="Zip Code *"
              {...register('zipCode', { required: 'Zip code is required' })}
              error={errors.zipCode?.message}
            />

            <Input
              label="Phone Number *"
              type="tel"
              placeholder="1234567890"
              {...register('phoneNumber', { 
                required: 'Phone number is required',
                pattern: { value: /^\d{11}$/, message: 'Phone number must be 11 digits' }
              })}
              error={errors.phoneNumber?.message}
            />

            <Input
              label="Email *"
              type="email"
              {...register('email', { required: 'Email is required' })}
              error={errors.email?.message}
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

            <div className="grid grid-cols-2 gap-4">
              <Input
                label="Opening Time *"
                type="time"
                {...register('openingTime', { required: 'Opening time is required' })}
                error={errors.openingTime?.message}
              />

              <Input
                label="Closing Time *"
                type="time"
                {...register('closingTime', { required: 'Closing time is required' })}
                error={errors.closingTime?.message}
              />
            </div>

            <div className="flex gap-4">
              <Button type="submit" isLoading={isLoading} className="flex-1">
                Update Clinic
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

