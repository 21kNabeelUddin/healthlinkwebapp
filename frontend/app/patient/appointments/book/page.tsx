'use client';

import { useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import DashboardLayout from '@/components/layout/DashboardLayout';

export default function BookAppointmentPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  useEffect(() => {
    // Redirect to doctors page, or to specific doctor booking if doctorId is provided
    const doctorId = searchParams.get('doctorId');
    if (doctorId) {
      router.replace(`/patient/doctors/${doctorId}/book`);
    } else {
      router.replace('/patient/doctors');
    }
  }, [router, searchParams]);

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-4 border-teal-500 border-t-transparent mb-4"></div>
          <p className="text-slate-600">Redirecting...</p>
        </div>
      </div>
    </DashboardLayout>
  );
}

