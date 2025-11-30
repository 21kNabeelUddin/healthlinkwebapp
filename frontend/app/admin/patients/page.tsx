'use client';

import { useState, useEffect } from 'react';
import { adminApi } from '@/lib/api';
import { User } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';

export default function AdminPatientsPage() {
  const [patients, setPatients] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadPatients();
  }, []);

  const loadPatients = async () => {
    setIsLoading(true);
    try {
      const data = await adminApi.getAllPatients();
      setPatients(data || []);
    } catch (error: any) {
      toast.error('Failed to load patients');
      console.error('Patients load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (patientId: string) => {
    if (!confirm('Are you sure you want to delete this patient?')) return;

    try {
      await adminApi.deletePatient(patientId);
      toast.success('Patient deleted successfully');
      loadPatients();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to delete patient');
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
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Patient Management</h1>
        <p className="text-gray-600">Manage all registered patients</p>
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
              {patients.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center p-4 text-gray-500">
                    No patients found
                  </td>
                </tr>
              ) : (
                patients.map((patient) => (
                  <tr key={patient.id} className="border-b">
                    <td className="p-3">{patient.id}</td>
                    <td className="p-3">{patient.firstName} {patient.lastName}</td>
                    <td className="p-3">{patient.email}</td>
                    <td className="p-3">{patient.phoneNumber}</td>
                    <td className="p-3">
                      <span className={`px-2 py-1 rounded text-xs ${
                        patient.isVerified ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                      }`}>
                        {patient.isVerified ? 'Yes' : 'No'}
                      </span>
                    </td>
                    <td className="p-3">
                      <Button
                        variant="danger"
                        onClick={() => handleDelete(patient.id.toString())}
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

