'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useForm } from 'react-hook-form';
import { facilitiesApi } from '@/lib/api';
import { ClinicRequest } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import { Building2, MapPin, Clock, Phone, Mail, DollarSign, FileText, CheckCircle2 } from 'lucide-react';

export default function NewClinicPage() {
  const router = useRouter();
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [servicesOffered, setServicesOffered] = useState<{ online: boolean; onsite: boolean }>({
    online: false,
    onsite: true, // Default to onsite
  });
  const { register, handleSubmit, formState: { errors }, setValue } = useForm<ClinicRequest>();

  const onSubmit = async (data: ClinicRequest) => {
    if (!user?.id) return;

    // Build servicesOffered string
    const services: string[] = [];
    if (servicesOffered.online) services.push('ONLINE');
    if (servicesOffered.onsite) services.push('ONSITE');
    
    if (services.length === 0) {
      toast.error('Please select at least one service type (Online or On-site)');
      return;
    }

    const servicesOfferedStr = services.join(',');

    setIsLoading(true);
    try {
      await facilitiesApi.createForDoctor(user.id.toString(), {
        ...data,
        servicesOffered: servicesOfferedStr,
      });

      toast.success('Clinic created successfully!');
      router.push('/doctor/clinics');
    } catch (error: any) {
      if (error.response?.status === 401) {
        toast.error('Your session has expired. Please log in again to create a clinic.');
        setTimeout(() => router.push('/auth/doctor/login'), 800);
      } else {
        toast.error(error.response?.data?.message || 'Failed to create clinic');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          {/* Header */}
          <div className="mb-6">
            <div className="flex items-center gap-3 mb-2">
              <div className="w-12 h-12 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center">
                <Building2 className="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 className="text-3xl font-bold text-slate-900">Create New Clinic</h1>
                <p className="text-slate-600 text-sm mt-1">Add a new clinic location to your practice</p>
              </div>
            </div>
          </div>

          <Card className="shadow-lg">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Clinic Information Section */}
              <div className="space-y-5">
                <div className="flex items-center gap-2 pb-3 border-b border-slate-200">
                  <Building2 className="w-5 h-5 text-teal-600" />
                  <h2 className="text-lg font-semibold text-slate-900">Clinic Information</h2>
                </div>

                <Input
                  label="Clinic Name *"
                  {...register('name', { required: 'Clinic name is required' })}
                  error={errors.name?.message}
                  placeholder="e.g., Downtown Medical Center"
                />

                <Input
                  label="Address *"
                  {...register('address', { required: 'Address is required' })}
                  error={errors.address?.message}
                  placeholder="Street address"
                />

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <Input
                    label="Town *"
                    {...register('town', { required: 'Town is required' })}
                    error={errors.town?.message}
                    placeholder="e.g., Malir Cantt"
                  />
                  <Input
                    label="City *"
                    {...register('city', { required: 'City is required' })}
                    error={errors.city?.message}
                    placeholder="e.g., Karachi"
                  />
                  <Input
                    label="State *"
                    {...register('state', { required: 'State is required' })}
                    error={errors.state?.message}
                    placeholder="e.g., Sindh"
                  />
                </div>

                <Input
                  label="Zip Code *"
                  {...register('zipCode', { required: 'Zip code is required' })}
                  error={errors.zipCode?.message}
                  placeholder="e.g., 75000"
                />
              </div>

              {/* Contact Information Section */}
              <div className="space-y-5 pt-4 border-t border-slate-200">
                <div className="flex items-center gap-2 pb-3">
                  <Phone className="w-5 h-5 text-teal-600" />
                  <h2 className="text-lg font-semibold text-slate-900">Contact Information</h2>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <Input
                    label="Phone Number *"
                    type="tel"
                    placeholder="03001234567"
                    {...register('phoneNumber', { 
                      required: 'Phone number is required',
                      pattern: { value: /^[0-9]{11}$/, message: 'Phone number must be 11 digits' }
                    })}
                    error={errors.phoneNumber?.message}
                  />

                  <Input
                    label="Email *"
                    type="email"
                    placeholder="clinic@example.com"
                    {...register('email', { required: 'Email is required' })}
                    error={errors.email?.message}
                  />
                </div>
              </div>

              {/* Services & Pricing Section */}
              <div className="space-y-5 pt-4 border-t border-slate-200">
                <div className="flex items-center gap-2 pb-3">
                  <CheckCircle2 className="w-5 h-5 text-teal-600" />
                  <h2 className="text-lg font-semibold text-slate-900">Services & Pricing</h2>
                </div>

                {/* Services Offered */}
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-3">
                    Services Offered *
                  </label>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <label className="flex items-center gap-3 p-4 border-2 rounded-lg cursor-pointer transition-all hover:border-teal-400 hover:bg-teal-50"
                      style={{ borderColor: servicesOffered.online ? '#14b8a6' : '#e2e8f0' }}
                    >
                      <input
                        type="checkbox"
                        checked={servicesOffered.online}
                        onChange={(e) => setServicesOffered({ ...servicesOffered, online: e.target.checked })}
                        className="w-5 h-5 text-teal-600 rounded focus:ring-teal-500"
                      />
                      <div className="flex-1">
                        <div className="font-semibold text-slate-900">Online Consultation</div>
                        <div className="text-sm text-slate-600">Video call appointments</div>
                      </div>
                    </label>
                    <label className="flex items-center gap-3 p-4 border-2 rounded-lg cursor-pointer transition-all hover:border-teal-400 hover:bg-teal-50"
                      style={{ borderColor: servicesOffered.onsite ? '#14b8a6' : '#e2e8f0' }}
                    >
                      <input
                        type="checkbox"
                        checked={servicesOffered.onsite}
                        onChange={(e) => setServicesOffered({ ...servicesOffered, onsite: e.target.checked })}
                        className="w-5 h-5 text-teal-600 rounded focus:ring-teal-500"
                      />
                      <div className="flex-1">
                        <div className="font-semibold text-slate-900">On-site Visit</div>
                        <div className="text-sm text-slate-600">In-person appointments</div>
                      </div>
                    </label>
                  </div>
                  {!servicesOffered.online && !servicesOffered.onsite && (
                    <p className="mt-2 text-sm text-red-600">Please select at least one service type</p>
                  )}
                </div>

                <Input
                  label="Consultation Fee (PKR) *"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                  {...register('consultationFee', {
                    required: 'Consultation fee is required',
                    min: { value: 0, message: 'Fee must be positive' },
                  })}
                  error={errors.consultationFee?.message}
                />
              </div>

              {/* Operating Hours Section */}
              <div className="space-y-5 pt-4 border-t border-slate-200">
                <div className="flex items-center gap-2 pb-3">
                  <Clock className="w-5 h-5 text-teal-600" />
                  <h2 className="text-lg font-semibold text-slate-900">Operating Hours</h2>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
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
              </div>

              {/* Description Section */}
              <div className="space-y-5 pt-4 border-t border-slate-200">
                <div className="flex items-center gap-2 pb-3">
                  <FileText className="w-5 h-5 text-teal-600" />
                  <h2 className="text-lg font-semibold text-slate-900">Description</h2>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Clinic Description *
                  </label>
                  <textarea
                    {...register('description', { required: 'Description is required' })}
                    className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 resize-none"
                    rows={4}
                    placeholder="Describe your clinic, specialties, and services offered..."
                  />
                  {errors.description && (
                    <p className="mt-1 text-sm text-red-600">{errors.description.message}</p>
                  )}
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex gap-4 pt-6 border-t border-slate-200">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => router.back()}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  isLoading={isLoading}
                  className="flex-1 bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700 text-white"
                >
                  Create Clinic
                </Button>
              </div>
            </form>
          </Card>
        </div>
      </div>
    </DashboardLayout>
  );
}
