'use client';

import { useState, useEffect } from 'react';
import { adminApi } from '@/lib/api';
import { MedicalHistory } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { format } from 'date-fns';

export default function AdminMedicalHistoriesPage() {
  const [histories, setHistories] = useState<MedicalHistory[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadHistories();
  }, [statusFilter]);

  const loadHistories = async () => {
    setIsLoading(true);
    try {
      const response = await adminApi.getAllMedicalHistories(undefined, statusFilter || undefined);
      if (response.success && response.data) {
        setHistories(response.data);
      }
    } catch (error: any) {
      toast.error('Failed to load medical histories');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (historyId: number) => {
    if (!confirm('Are you sure you want to delete this medical history?')) return;

    try {
      const response = await adminApi.deleteMedicalHistory(historyId);
      if (response.success) {
        toast.success('Medical history deleted successfully');
        loadHistories();
      } else {
        toast.error(response.message);
      }
    } catch (error: any) {
      toast.error('Failed to delete medical history');
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
      <DashboardLayout requiredUserType="ADMIN">
        <div className="text-center">Loading...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="ADMIN">
      <div className="mb-8">
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Medical History Management</h1>
        <p className="text-gray-600">View all medical history records</p>
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
              No medical histories found
            </div>
          </Card>
        ) : (
          histories.map((history) => (
            <Card key={history.id}>
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="text-xl font-semibold text-gray-800">
                      {history.condition} - {history.patientName}
                    </h3>
                    <span className={`px-2 py-1 rounded text-xs ${getStatusColor(history.status)}`}>
                      {history.status}
                    </span>
                  </div>

                  <div className="space-y-1 text-gray-600">
                    <p><strong>Diagnosis Date:</strong> {format(new Date(history.diagnosisDate), 'MMM dd, yyyy')}</p>
                    <p><strong>Description:</strong> {history.description}</p>
                    <p><strong>Doctor:</strong> {history.doctorName}</p>
                    <p><strong>Hospital:</strong> {history.hospitalName}</p>
                  </div>
                </div>

                <Button
                  variant="danger"
                  onClick={() => handleDelete(history.id)}
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

