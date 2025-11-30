'use client';

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { format } from 'date-fns';
import { toast } from 'react-hot-toast';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { patientApi } from '@/lib/api';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { patientSidebarItems } from '@/app/patient/sidebar-items';
import { Button } from '@/marketing/ui/button';
import Input from '@/components/ui/Input';
import { Textarea } from '@/marketing/ui/textarea';

interface ProfileFormValues {
  phoneNumber: string;
  address: string;
}

export default function PatientProfilePage() {
  const { user, logout, updateUser } = useAuth();
  const router = useRouter();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ProfileFormValues>({
    defaultValues: {
      phoneNumber: user?.phoneNumber ?? '',
      address: user?.address ?? '',
    },
  });

  useEffect(() => {
    reset({
      phoneNumber: user?.phoneNumber ?? '',
      address: user?.address ?? '',
    });
  }, [user, reset]);

  const onSubmit = async (values: ProfileFormValues) => {
    if (!user?.id) return;
    try {
      const response = await patientApi.updateProfile(user.id, values);
      if (response.success && response.data) {
        updateUser(response.data);
        toast.success(response.message || 'Profile updated successfully');
      } else {
        toast.error(response.message || 'Failed to update profile');
      }
    } catch (error) {
      toast.error('Failed to update profile');
    }
  };

  const handleLogout = () => {
    logout();
    router.replace('/');
  };

  const dateOfBirth = user?.dateOfBirth ? format(new Date(user.dateOfBirth), 'PPP') : '—';

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav
        userName={`${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() || 'Patient'}
        userRole="Patient"
        showPortalLinks={false}
        onLogout={handleLogout}
      />

      <div className="flex">
        <Sidebar items={patientSidebarItems} currentPath="/patient/profile" />

        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <div className="max-w-5xl mx-auto space-y-8">
            <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm text-slate-500 uppercase tracking-wide">Profile</p>
                  <h1 className="text-3xl font-semibold text-slate-900">Personal Information</h1>
                  <p className="text-slate-600">
                    Manage the contact details that help your care team reach you
                  </p>
                </div>
                <div className="bg-gradient-to-r from-teal-500 to-violet-600 text-white rounded-2xl p-4 min-w-[220px]">
                  <p className="text-sm opacity-80">Verification Status</p>
                  <p className="text-lg font-semibold">
                    {user?.isVerified ? 'Verified' : 'Pending verification'}
                  </p>
                  <p className="text-xs opacity-80 mt-1">
                    Member since {user?.createdAt ? format(new Date(user.createdAt), 'MMM yyyy') : '—'}
                  </p>
                </div>
              </div>
            </div>

            <div className="grid lg:grid-cols-3 gap-6">
              <div className="lg:col-span-1 space-y-4">
                <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
                  <h2 className="text-lg font-semibold text-slate-900 mb-4">Identity</h2>
                  <div className="space-y-3 text-sm text-slate-600">
                    <div>
                      <p className="text-xs uppercase text-slate-400">Full Name</p>
                      <p className="text-base text-slate-900">
                        {user ? `${user.firstName} ${user.lastName}` : '—'}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs uppercase text-slate-400">Email</p>
                      <p className="text-base text-slate-900 break-all">{user?.email ?? '—'}</p>
                    </div>
                    <div>
                      <p className="text-xs uppercase text-slate-400">Date of Birth</p>
                      <p className="text-base text-slate-900">{dateOfBirth}</p>
                    </div>
                  </div>
                </div>

                <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
                  <h2 className="text-lg font-semibold text-slate-900 mb-4">Account Preferences</h2>
                  <div className="space-y-3 text-sm text-slate-600">
                    <div className="flex items-center justify-between">
                      <span>Two-factor status</span>
                      <span className="px-2 py-1 text-xs rounded-full bg-slate-100 text-slate-600">
                        Email OTP
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span>Communication</span>
                      <span className="text-slate-900 font-medium">
                        {user?.isVerified ? 'Enabled' : 'Awaiting verification'}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="lg:col-span-2 bg-white rounded-2xl p-6 border border-slate-200 shadow-sm">
                <h2 className="text-xl font-semibold text-slate-900 mb-6">Contact Details</h2>
                <form className="space-y-5" onSubmit={handleSubmit(onSubmit)}>
                  <Input
                    label="Phone Number"
                    placeholder="03XXXXXXXXX"
                    {...register('phoneNumber', {
                      required: 'Phone number is required',
                      pattern: {
                        value: /^\d{11}$/,
                        message: 'Phone number must be 11 digits',
                      },
                    })}
                    error={errors.phoneNumber?.message}
                  />

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
                    <Textarea
                      rows={4}
                      placeholder="House, street, city"
                      {...register('address', { required: 'Address is required', maxLength: 255 })}
                      className={errors.address ? 'border-red-500' : ''}
                    />
                    {errors.address && (
                      <p className="mt-1 text-sm text-red-600">{errors.address.message}</p>
                    )}
                  </div>

                  <div className="flex justify-end gap-3">
                    <Button
                      type="button"
                      variant="ghost"
                      onClick={() =>
                        reset({
                          phoneNumber: user?.phoneNumber ?? '',
                          address: user?.address ?? '',
                        })
                      }
                    >
                      Reset
                    </Button>
                    <Button
                      type="submit"
                      className="bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700"
                      disabled={isSubmitting}
                    >
                      {isSubmitting ? 'Saving...' : 'Save Changes'}
                    </Button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

