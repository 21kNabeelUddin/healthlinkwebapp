'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { authApi, getUserFriendlyError } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from 'react-hot-toast';
import Link from 'next/link';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import AuthLayout from '@/components/auth/AuthLayout';

interface LoginForm {
  email: string;
  password: string;
}

export default function PatientLogin() {
  const router = useRouter();
  const { login } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>();

  const onSubmit = async (data: LoginForm) => {
    setIsLoading(true);
    try {
      const response = await authApi.login({
        email: data.email,
        password: data.password,
      });

      if (response.accessToken && response.user) {
        login(response.user, response.accessToken, response.refreshToken);
        toast.success('Login successful');
        router.replace('/patient/dashboard');
      } else {
        toast.error('Login failed: Invalid response');
      }
    } catch (error: any) {
      toast.error(error.userMessage || getUserFriendlyError(error, 'Login failed'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout
      role="PATIENT"
      title="Patient Login"
      subtitle="Access your appointments, medical records, and care team updates."
      footer={
        <>
          Don't have an account?{' '}
          <Link href="/auth/patient/signup" className="text-teal-600 font-semibold hover:underline">
            Sign up
          </Link>
          <span className="mx-2 text-gray-400">|</span>
          <Link href="/auth/patient/forgot-password" className="text-sm text-teal-600 font-semibold hover:underline">
            Forgot password?
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          {...register('email', { required: 'Email is required' })}
          error={errors.email?.message}
        />

        <Input
          label="Password"
          type="password"
          autoComplete="current-password"
          {...register('password', { required: 'Password is required' })}
          error={errors.password?.message}
        />

        <Button
          type="submit"
          isLoading={isLoading}
          className="w-full bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700"
        >
          Login
        </Button>
      </form>
    </AuthLayout>
  );
}

