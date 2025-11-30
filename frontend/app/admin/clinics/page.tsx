'use client';

import { useState, useEffect } from 'react';
import { adminApi } from '@/lib/api';
import { Clinic } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';

export default function AdminClinicsPage() {
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadClinics();
  }, []);

  const loadClinics = async () => {
    setIsLoading(true);
    try {
      const response = await adminApi.getAllClinics();
      if (response.success && response.data) {
        setClinics(response.data);
      }
    } catch (error: any) {
      toast.error('Failed to load clinics');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (clinicId: number) => {
    if (!confirm('Are you sure you want to delete this clinic?')) return;

    try {
      const response = await adminApi.deleteClinic(clinicId);
      if (response.success) {
        toast.success('Clinic deleted successfully');
        loadClinics();
      } else {
        toast.error(response.message);
      }
    } catch (error: any) {
      toast.error('Failed to delete clinic');
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
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Clinic Management</h1>
        <p className="text-gray-600">Manage all clinics</p>
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        {clinics.length === 0 ? (
          <div className="col-span-full text-center py-8 text-gray-500">
            No clinics found
          </div>
        ) : (
          clinics.map((clinic) => (
            <Card key={clinic.id}>
              <div className="space-y-3">
                <div>
                  <h3 className="text-xl font-semibold text-gray-800">{clinic.name}</h3>
                  <span className={`inline-block mt-2 px-2 py-1 rounded text-xs ${
                    clinic.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                  }`}>
                    {clinic.isActive ? 'Active' : 'Inactive'}
                  </span>
                </div>

                <div className="space-y-1 text-sm text-gray-600">
                  <p>{clinic.address}</p>
                  <p>{clinic.city}, {clinic.state} {clinic.zipCode}</p>
                  <p>Doctor: {clinic.doctorName}</p>
                </div>

                <Button
                  variant="danger"
                  className="w-full"
                  onClick={() => handleDelete(clinic.id)}
                >
                  Delete
                </Button>
              </div>
            </Card>
          ))
        )}
      </div>
    </DashboardLayout>
  );
}

