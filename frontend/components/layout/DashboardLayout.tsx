'use client';

import { useEffect } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import Navbar from './Navbar';
import { Sidebar, type SidebarItem } from '@/marketing/layout/Sidebar';
import { patientSidebarItems } from '@/app/patient/sidebar-items';
import { doctorSidebarItems } from '@/app/doctor/sidebar-items';
import { adminSidebarItems } from '@/app/admin/sidebar-items';

interface DashboardLayoutProps {
  children: React.ReactNode;
  requiredUserType?: 'PATIENT' | 'DOCTOR' | 'ADMIN';
  sidebarItems?: SidebarItem[];
  hideSidebar?: boolean;
}

export default function DashboardLayout({
  children,
  requiredUserType,
  sidebarItems,
  hideSidebar,
}: DashboardLayoutProps) {
  const { user, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

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

  // Auto-pick sidebar based on user type if not provided
  const inferredSidebar =
    sidebarItems ??
    (user.userType === 'PATIENT'
      ? patientSidebarItems
      : user.userType === 'DOCTOR'
        ? doctorSidebarItems
        : user.userType === 'ADMIN'
          ? adminSidebarItems
          : undefined);

  const shouldHideSidebar =
    hideSidebar ||
    !inferredSidebar ||
    (pathname && pathname.startsWith('/patient/chatbot')) ||
    (pathname && pathname.startsWith('/staff'));

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="container mx-auto px-4 py-8">
        {shouldHideSidebar ? (
          <main>{children}</main>
        ) : (
          <div className="flex gap-6">
            <Sidebar items={inferredSidebar} currentPath={pathname || ''} />
            <main className="flex-1 min-w-0">{children}</main>
          </div>
        )}
      </div>
    </div>
  );
}

