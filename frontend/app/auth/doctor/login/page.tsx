'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { authApi } from '@/lib/api';
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

export default function DoctorLogin() {
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
        role: 'DOCTOR',
      });

      if (response.accessToken && response.user) {
        login(response.user, response.accessToken, response.refreshToken);
        toast.success('Login successful');
        router.replace('/doctor/dashboard');
      } else {
        toast.error('Login failed: Invalid response');
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || error.message || 'Login failed');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout
      role="DOCTOR"
      title="Doctor Login"
      subtitle="Manage your clinics, appointments, and patient insights in one secure space."
      footer={
        <>
          Don't have an account?{' '}
          <Link href="/auth/doctor/signup" className="text-teal-600 font-semibold hover:underline">
            Sign up
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

        <Input
          label="Password"
          type="password"
          {...register('password', { required: 'Password is required' })}
          error={errors.password?.message}
        />

        <Button
          type="submit"
          isLoading={isLoading}
          className="w-full bg-gradient-to-r from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600"
        >
          Login
        </Button>
      </form>
    </AuthLayout>
  );
}

