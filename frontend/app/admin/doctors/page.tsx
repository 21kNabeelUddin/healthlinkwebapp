'use client';

import { useState, useEffect } from 'react';
import { adminApi } from '@/lib/api';
import { User } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';

export default function AdminDoctorsPage() {
  const [doctors, setDoctors] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadDoctors();
  }, []);

  const loadDoctors = async () => {
    setIsLoading(true);
    try {
      const data = await adminApi.getAllDoctors();
      setDoctors(data || []);
    } catch (error: any) {
      toast.error('Failed to load doctors');
      console.error('Doctors load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (doctorId: string) => {
    if (!confirm('Are you sure you want to delete this doctor?')) return;

    try {
      await adminApi.deleteDoctor(doctorId);
      toast.success('Doctor deleted successfully');
      loadDoctors();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to delete doctor');
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
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Doctor Management</h1>
        <p className="text-gray-600">Manage all registered doctors</p>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b">
                <th className="text-left p-3">ID</th>
                <th className="text-left p-3">Name</th>
                <th className="text-left p-3">Email</th>
                <th className="text-left p-3">Specialization</th>
                <th className="text-left p-3">Verified</th>
                <th className="text-left p-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {doctors.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center p-4 text-gray-500">
                    No doctors found
                  </td>
                </tr>
              ) : (
                doctors.map((doctor) => (
                  <tr key={doctor.id} className="border-b">
                    <td className="p-3">{doctor.id}</td>
                    <td className="p-3">{doctor.firstName} {doctor.lastName}</td>
                    <td className="p-3">{doctor.email}</td>
                    <td className="p-3">{doctor.specialization || 'N/A'}</td>
                    <td className="p-3">
                      <span className={`px-2 py-1 rounded text-xs ${
                        doctor.isVerified ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                      }`}>
                        {doctor.isVerified ? 'Yes' : 'No'}
                      </span>
                    </td>
                    <td className="p-3">
                      <Button
                        variant="danger"
                        onClick={() => handleDelete(doctor.id.toString())}
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

