'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { authApi } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from 'react-hot-toast';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import AuthLayout from '@/components/auth/AuthLayout';

interface LoginForm {
  email: string;
  password: string;
}

export default function AdminLogin() {
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
        role: 'ADMIN',
      });

      if (response.accessToken && response.user) {
        login(response.user, response.accessToken, response.refreshToken);
        toast.success('Login successful');
        router.replace('/admin/dashboard');
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
      role="ADMIN"
      title="Admin Login"
      subtitle="Authenticate to access analytics, manage users, and keep the platform secure."
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Email"
          type="email"
          name="email"
          autoComplete="email"
          {...register('email', { required: 'Email is required' })}
          error={errors.email?.message}
        />

        <Input
          label="Password"
          type="password"
          name="password"
          autoComplete="current-password"
          {...register('password', { required: 'Password is required' })}
          error={errors.password?.message}
        />

        <Button
          type="submit"
          isLoading={isLoading}
          className="w-full bg-gradient-to-r from-slate-900 to-slate-700 hover:from-slate-800 hover:to-slate-600"
        >
          Login
        </Button>
      </form>
    </AuthLayout>
  );
}

