'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { medicalRecordsApi } from '@/lib/api';
import { MedicalHistory } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Link from 'next/link';
import { format } from 'date-fns';

export default function MedicalHistoryPage() {
  const { user } = useAuth();
  const [histories, setHistories] = useState<MedicalHistory[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (user?.id) {
      loadHistories();
    }
  }, [user, statusFilter]);

  const loadHistories = async () => {
    if (!user?.id) return;
    
    setIsLoading(true);
    try {
      const data = await medicalRecordsApi.listForPatient(user.id.toString());
      // Filter by status if needed
      const filtered = statusFilter 
        ? data.filter((record: any) => record.status === statusFilter)
        : data;
      setHistories(filtered || []);
    } catch (error: any) {
      toast.error('Failed to load medical history');
      console.error('Medical history load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (historyId: string) => {
    if (!confirm('Are you sure you want to delete this medical history?')) return;

    try {
      await medicalRecordsApi.delete(historyId);
      toast.success('Medical history deleted');
      loadHistories();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to delete medical history');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-red-100 text-red-800';
      case 'RESOLVED':
        return 'bg-green-100 text-green-800';
      case 'CHRONIC':
        return 'bg-yellow-100 text-yellow-800';
      case 'UNDER_TREATMENT':
        return 'bg-blue-100 text-blue-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="PATIENT">
        <div className="text-center">Loading...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="mb-8 flex justify-between items-center">
        <div>
          <h1 className="text-4xl font-bold text-gray-800 mb-2">Medical History</h1>
          <p className="text-gray-600">Manage your medical records</p>
        </div>
        <Link href="/patient/medical-history/new">
          <Button>Add New Record</Button>
        </Link>
      </div>

      <div className="mb-6">
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-4 py-2 border rounded-lg"
        >
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="RESOLVED">Resolved</option>
          <option value="CHRONIC">Chronic</option>
          <option value="UNDER_TREATMENT">Under Treatment</option>
        </select>
      </div>

      <div className="space-y-4">
        {histories.length === 0 ? (
          <Card>
            <div className="text-center py-8 text-gray-500">
              No medical history records found
            </div>
          </Card>
        ) : (
          histories.map((history) => (
            <Card key={history.id}>
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="text-xl font-semibold text-gray-800">
                      {history.condition}
                    </h3>
                    <span className={`px-2 py-1 rounded text-xs ${getStatusColor(history.status)}`}>
                      {history.status}
                    </span>
                  </div>

                  <div className="space-y-1 text-gray-600">
                    <p><strong>Diagnosis Date:</strong> {format(new Date(history.diagnosisDate), 'MMM dd, yyyy')}</p>
                    <p><strong>Description:</strong> {history.description}</p>
                    <p><strong>Treatment:</strong> {history.treatment}</p>
                    <p><strong>Medications:</strong> {history.medications}</p>
                    <p><strong>Doctor:</strong> {history.doctorName}</p>
                    <p><strong>Hospital:</strong> {history.hospitalName}</p>
                  </div>
                </div>

                <div className="flex flex-col gap-2 ml-4">
                  <Link href={`/patient/medical-history/${history.id}/edit`}>
                    <Button variant="outline" className="w-full">Edit</Button>
                  </Link>
                  <Button
                    variant="danger"
                    className="w-full"
                    onClick={() => handleDelete(history.id)}
                  >
                    Delete
                  </Button>
                </div>
              </div>
            </Card>
          ))
        )}
      </div>
    </DashboardLayout>
  );
}

