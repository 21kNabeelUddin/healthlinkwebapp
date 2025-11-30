'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import Navbar from './Navbar';

interface DashboardLayoutProps {
  children: React.ReactNode;
  requiredUserType?: 'PATIENT' | 'DOCTOR' | 'ADMIN';
}

export default function DashboardLayout({ children, requiredUserType }: DashboardLayoutProps) {
  const { user, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading) {
      if (!isAuthenticated || !user) {
        router.push('/');
        return;
      }
      if (requiredUserType && user.userType !== requiredUserType) {
        router.push('/');
        return;
      }
    }
  }, [isAuthenticated, isLoading, user, requiredUserType, router]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-xl">Loading...</div>
      </div>
    );
  }

  if (!isAuthenticated || !user || (requiredUserType && user.userType !== requiredUserType)) {
    return null; // Will redirect via useEffect
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        {children}
      </main>
    </div>
  );
}

