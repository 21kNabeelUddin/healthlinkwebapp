'use client';

import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { useRouter } from 'next/navigation';
import Button from '@/components/ui/Button';
import { Activity } from 'lucide-react';

export default function Navbar() {
  const { user, logout, isAuthenticated } = useAuth();
  const router = useRouter();

  const handleLogout = () => {
    logout();
    router.push('/');
  };

  const getDashboardPath = () => {
    if (!user) return '/';
    switch (user.userType) {
      case 'PATIENT':
        return '/patient/dashboard';
      case 'DOCTOR':
        return '/doctor/dashboard';
      case 'ADMIN':
        return '/admin/dashboard';
      default:
        return '/';
    }
  };

  return (
    <nav className="bg-white shadow-md">
      <div className="container mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          <Link
            href={isAuthenticated ? getDashboardPath() : '/'}
            className="flex items-center gap-3 text-2xl font-bold text-primary-600"
          >
            <span className="w-10 h-10 rounded-xl bg-gradient-to-br from-teal-500 to-violet-600 flex items-center justify-center">
              <Activity className="w-5 h-5 text-white" />
            </span>
            <span>HealthLink+</span>
          </Link>

          <div className="flex items-center gap-4">
            {isAuthenticated && user ? (
              <>
                <span className="text-gray-700">
                  {user.firstName} {user.lastName} ({user.userType})
                </span>
                <Button variant="outline" onClick={handleLogout}>
                  Logout
                </Button>
              </>
            ) : (
              <Link href="/">
                <Button variant="outline">Home</Button>
              </Link>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}

