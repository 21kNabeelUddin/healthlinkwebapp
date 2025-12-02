'use client';

import { useAuth } from '@/contexts/AuthContext';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';

export default function DoctorProfilePage() {
  const { user } = useAuth();

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="max-w-3xl mx-auto space-y-6">
        <h1 className="text-3xl font-bold text-gray-800">Doctor Profile</h1>
        <p className="text-gray-600">
          View your basic profile information. Profile editing will be added in a later phase.
        </p>

        <Card>
          <div className="space-y-4">
            <div>
              <h2 className="text-xl font-semibold text-gray-800">Basic Information</h2>
              <p className="mt-2 text-sm text-gray-600">
                These details are used across your clinics and appointments.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-500">Full Name</p>
                <p className="font-medium text-gray-900">
                  {user ? `${user.firstName} ${user.lastName}` : '—'}
                </p>
              </div>
              <div>
                <p className="text-gray-500">Email</p>
                <p className="font-medium text-gray-900">{user?.email ?? '—'}</p>
              </div>
              <div>
                <p className="text-gray-500">Phone Number</p>
                <p className="font-medium text-gray-900">{user?.phoneNumber ?? '—'}</p>
              </div>
              <div>
                <p className="text-gray-500">Role</p>
                <p className="font-medium text-gray-900">{user?.userType ?? '—'}</p>
              </div>
            </div>
          </div>
        </Card>

        <Card>
          <div className="space-y-4">
            <div>
              <h2 className="text-xl font-semibold text-gray-800">Account Status</h2>
              <p className="mt-2 text-sm text-gray-600">
                Email verification and approval status control your ability to log in and accept appointments.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-500">Email Verified</p>
                <p className="font-medium text-gray-900">
                  {user?.isVerified ? 'Yes' : 'No'}
                </p>
              </div>
              <div>
                <p className="text-gray-500">Created At</p>
                <p className="font-medium text-gray-900">
                  {user?.createdAt ? new Date(user.createdAt).toLocaleString() : '—'}
                </p>
              </div>
            </div>

            <div className="pt-2">
              <Button disabled className="opacity-60 cursor-not-allowed">
                Edit Profile (coming soon)
              </Button>
            </div>
          </div>
        </Card>
      </div>
    </DashboardLayout>
  );
}


