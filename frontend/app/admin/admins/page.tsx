'use client';

import { useState, useEffect } from 'react';
import { adminApi } from '@/lib/api';
import { User } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';

export default function AdminAdminsPage() {
  const [admins, setAdmins] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadAdmins();
  }, []);

  const loadAdmins = async () => {
    setIsLoading(true);
    try {
      const response = await adminApi.getAllAdmins();
      if (response.success && response.data) {
        setAdmins(response.data);
      }
    } catch (error: any) {
      toast.error('Failed to load admins');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (adminId: number) => {
    if (!confirm('Are you sure you want to delete this admin?')) return;

    try {
      const response = await adminApi.deleteAdmin(adminId);
      if (response.success) {
        toast.success('Admin deleted successfully');
        loadAdmins();
      } else {
        toast.error(response.message);
      }
    } catch (error: any) {
      toast.error('Failed to delete admin');
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="ADMIN">
        <div className="text-center">Loading...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="ADMIN">
      <div className="mb-8">
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Admin Management</h1>
        <p className="text-gray-600">Manage all administrators</p>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b">
                <th className="text-left p-3">ID</th>
                <th className="text-left p-3">Name</th>
                <th className="text-left p-3">Email</th>
                <th className="text-left p-3">Phone</th>
                <th className="text-left p-3">Verified</th>
                <th className="text-left p-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {admins.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center p-4 text-gray-500">
                    No admins found
                  </td>
                </tr>
              ) : (
                admins.map((admin) => (
                  <tr key={admin.id} className="border-b">
                    <td className="p-3">{admin.id}</td>
                    <td className="p-3">{admin.firstName} {admin.lastName}</td>
                    <td className="p-3">{admin.email}</td>
                    <td className="p-3">{admin.phoneNumber}</td>
                    <td className="p-3">
                      <span className={`px-2 py-1 rounded text-xs ${
                        admin.isVerified ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                      }`}>
                        {admin.isVerified ? 'Yes' : 'No'}
                      </span>
                    </td>
                    <td className="p-3">
                      <Button
                        variant="danger"
                        onClick={() => handleDelete(admin.id)}
                      >
                        Delete
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </DashboardLayout>
  );
}

