'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { doctorApi, getUserFriendlyError } from '@/lib/api';
import { toast } from 'react-hot-toast';
import Link from 'next/link';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import AuthLayout from '@/components/auth/AuthLayout';

interface SignupForm {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  specialization: string;
  licenseNumber: string;
  yearsOfExperience: number;
}

export default function DoctorSignup() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm<SignupForm>();

  const onSubmit = async (data: SignupForm) => {
    setIsLoading(true);
    try {
      const response = await doctorApi.signup({
        ...data,
        role: 'DOCTOR',
      } as any);

      // Backend returns an envelope; treat truthy response as success
      toast.success('Signup successful. Please check your email for OTP verification.');
      router.push(`/auth/doctor/verify-otp?email=${encodeURIComponent(data.email)}`);
    } catch (error: any) {
      toast.error(error.userMessage || getUserFriendlyError(error, 'Signup failed'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout
      role="DOCTOR"
      title="Create your doctor workspace"
      subtitle="Verify your profile to publish clinics, manage schedules, and consult patients securely."
      footer={
        <>
          Already have an account?{' '}
          <Link href="/auth/doctor/login" className="text-teal-600 font-semibold hover:underline">
            Login
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Email"
          type="email"
          {...register('email', { required: 'Email is required' })}
          error={errors.email?.message}
        />

        <div className="space-y-1">
          <Input
            label="Password"
            type="password"
            autoComplete="new-password"
            {...register('password', {
              required: 'Password is required',
              minLength: { value: 8, message: 'Password must be at least 8 characters' },
              validate: {
                hasUpper: (v) =>
                  /[A-Z]/.test(v || '') || 'Password must include at least one uppercase letter',
                hasLower: (v) =>
                  /[a-z]/.test(v || '') || 'Password must include at least one lowercase letter',
                hasNumber: (v) => /\d/.test(v || '') || 'Password must include at least one number',
                hasSpecial: (v) =>
                  /[^a-zA-Z0-9 ]/.test(v || '') ||
                  'Password must include at least one special character',
              },
            })}
            error={errors.password?.message}
          />
          <p className="text-xs text-slate-500">
            Password must be at least 8 characters and include an uppercase letter, lowercase
            letter, number, and special character.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input
            label="First Name"
            {...register('firstName', { required: 'First name is required' })}
            error={errors.firstName?.message}
          />

          <Input
            label="Last Name"
            {...register('lastName', { required: 'Last name is required' })}
            error={errors.lastName?.message}
          />
        </div>

        <Input
          label="Phone Number"
          type="tel"
          placeholder="03301234567"
          {...register('phoneNumber', {
            required: 'Phone number is required',
            pattern: { value: /^\d{11}$/, message: 'Phone number must be 11 digits (Pakistani format)' },
          })}
          error={errors.phoneNumber?.message}
        />

        <Input
          label="Specialization"
          {...register('specialization', { required: 'Specialization is required' })}
          error={errors.specialization?.message}
        />

        <Input
          label="PMDC / License Number"
          placeholder="e.g. 12345-P"
          {...register('licenseNumber', {
            required: 'License number is required',
            pattern: {
              value: /^\d{5}-P$/,
              message: 'License must be in the format 12345-P (5 digits, dash, capital P)',
            },
          })}
          error={errors.licenseNumber?.message}
        />

        <Input
          label="Years of Experience"
          type="number"
          {...register('yearsOfExperience', {
            required: 'Years of experience is required',
            valueAsNumber: true,
            min: { value: 0, message: 'Must be a positive number' },
          })}
          error={errors.yearsOfExperience?.message}
        />

        <Button
          type="submit"
          isLoading={isLoading}
          className="w-full bg-gradient-to-r from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600"
        >
          Sign Up
        </Button>
      </form>
    </AuthLayout>
  );
}

