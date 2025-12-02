'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { doctorApi, getUserFriendlyError } from '@/lib/api';
import { toast } from 'react-hot-toast';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';

interface OtpForm {
  otp: string;
}

export default function DoctorVerifyOtp() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isLoading, setIsLoading] = useState(false);
  const email = searchParams.get('email') || '';

  const { register, handleSubmit, formState: { errors } } = useForm<OtpForm>();

  useEffect(() => {
    if (!email) {
      router.push('/auth/doctor/signup');
    }
  }, [email, router]);

  const onSubmit = async (data: OtpForm) => {
    setIsLoading(true);
    try {
      const message = await doctorApi.verifyOtp({
        email,
        otp: data.otp,
        userType: 'DOCTOR',
      });

      // Backend returns a plain success string (e.g. "Email verified successfully")
      toast.success(message || 'Email verified successfully');
      router.push('/auth/doctor/login');
    } catch (error: any) {
      toast.error(error.userMessage || getUserFriendlyError(error, 'OTP verification failed'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-xl p-8 w-full max-w-md">
        <h1 className="text-3xl font-bold text-center mb-6 text-gray-800">
          Verify OTP
        </h1>
        <p className="text-center text-gray-600 mb-6">
          Please enter the OTP sent to <strong>{email}</strong>
        </p>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input
            label="OTP"
            type="text"
            placeholder="123456"
            maxLength={6}
            {...register('otp', { 
              required: 'OTP is required',
              pattern: { value: /^\d{6}$/, message: 'OTP must be 6 digits' }
            })}
            error={errors.otp?.message}
          />

          <Button type="submit" isLoading={isLoading} className="w-full">
            Verify OTP
          </Button>
        </form>
      </div>
    </div>
  );
}

