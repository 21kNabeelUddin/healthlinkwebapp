'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function AdminAppointmentsPage() {
  const router = useRouter();
  
  useEffect(() => {
    // Redirect to enhanced appointments page
    router.replace('/admin/appointments/enhanced');
  }, [router]);

  return null;
}

