'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { authApi } from '@/lib/api';
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
  dateOfBirth: string;
  address: string;
}

export default function PatientSignup() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const { register, handleSubmit, formState: { errors }, setError } = useForm<SignupForm>();

  const onSubmit = async (data: SignupForm) => {
    setIsLoading(true);
    try {
      const response = await authApi.register({
        email: data.email,
        password: data.password,
        firstName: data.firstName,
        lastName: data.lastName,
        phoneNumber: data.phoneNumber,
        dateOfBirth: data.dateOfBirth ? `${data.dateOfBirth}T00:00:00` : undefined,
        address: data.address,
        role: 'PATIENT',
      });

      // For patients, backend does NOT return tokens until email is verified.
      // A successful response with a user object means registration succeeded.
      if (response?.user) {
        await authApi.sendOtp(data.email);
        toast.success('Registration successful! We\'ve sent a verification code to your email.');
        router.push(`/auth/patient/verify-otp?email=${encodeURIComponent(data.email)}`);
      } else {
        toast.error('Registration failed');
      }
    } catch (error: any) {
      const status = error?.response?.status;
      const backendMessage: string | undefined =
        error?.response?.data?.message || error?.response?.data?.error;

      if (status === 409) {
        const message =
          backendMessage ||
          'An account with this email already exists. Please log in or use a different email.';
        // Show message next to the email field as well
        setError('email', {
          type: 'conflict',
          message,
        });
        toast.error(message);
      } else if (status === 400) {
        toast.error(backendMessage || 'Please check the form for validation errors.');
      } else {
        toast.error(backendMessage || error.message || 'Signup failed');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout
      role="PATIENT"
      title="Create your patient account"
      subtitle="Set up your profile to book appointments, manage records, and receive instant updates."
      footer={
        <>
          Already have an account?{' '}
          <Link href="/auth/patient/login" className="text-teal-600 font-semibold hover:underline">
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
            {...register('password', {
              required: 'Password is required',
              minLength: {
                value: 8,
                message: 'Password must be at least 8 characters long',
              },
              pattern: {
                value: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).+$/,
                message:
                  'Password must include at least one uppercase letter, one lowercase letter, one number, and one special character.',
              },
            })}
            error={errors.password?.message}
          />
          {!errors.password && (
            <p className="text-xs text-gray-500">
              Password must be at least 8 characters and include an uppercase letter, lowercase letter, number, and special character.
            </p>
          )}
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
          label="Date of Birth"
          type="date"
          {...register('dateOfBirth', { required: 'Date of birth is required' })}
          error={errors.dateOfBirth?.message}
        />

        <Input
          label="Address"
          {...register('address', { required: 'Address is required' })}
          error={errors.address?.message}
        />

        <Button
          type="submit"
          isLoading={isLoading}
          className="w-full bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700"
        >
          Sign Up
        </Button>
      </form>
    </AuthLayout>
  );
}

