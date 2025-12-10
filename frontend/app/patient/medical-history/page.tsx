'use client';

import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { medicalRecordsApi } from '@/lib/api';
import { MedicalHistory } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Link from 'next/link';
import { format } from 'date-fns';
import { Badge } from '@/marketing/ui/badge';
import { Calendar, Building2, User, Activity } from 'lucide-react';

export default function MedicalHistoryPage() {
  const { user } = useAuth();
  const [histories, setHistories] = useState<MedicalHistory[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (user?.id) {
      loadHistories();
    }
  }, [user]);

  const loadHistories = async () => {
    if (!user?.id) return;
    
    setIsLoading(true);
    try {
      const data = await medicalRecordsApi.listForPatient(user.id.toString());
      // Don't filter here - let filteredHistories useMemo handle it
      setHistories(data || []);
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

  const filteredHistories = useMemo(() => {
    return histories.filter((record) => {
      if (statusFilter) {
        // If filtering and record has no status, exclude it
        if (!record.status) return false;
        if (record.status !== statusFilter) return false;
      }
      return true;
    });
  }, [histories, statusFilter]);

  const getStatusClasses = (status?: string) => {
    if (!status) {
      return 'bg-slate-100 text-slate-700 border-slate-200';
    }
    switch (status) {
      case 'ACTIVE':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'RESOLVED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'CHRONIC':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'UNDER_TREATMENT':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  const formatDate = (dateValue: string | Date | null | undefined): string => {
    if (!dateValue) return 'N/A';
    try {
      const date = new Date(dateValue);
      if (isNaN(date.getTime())) return 'N/A';
      return format(date, 'MMM dd, yyyy');
    } catch (error) {
      return 'N/A';
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="PATIENT">
        <div className="text-center py-12 text-slate-600">Loading medical history...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <p className="text-sm text-slate-500 uppercase tracking-wide">Medical History</p>
              <h1 className="text-4xl font-bold text-slate-900">My Medical Records</h1>
              <p className="text-slate-600">Review your conditions, treatments, and notes</p>
            </div>
            <Link href="/patient/medical-history/new">
              <Button className="bg-gradient-to-r from-teal-500 to-violet-600 text-white">
                Add New Record
              </Button>
            </Link>
          </div>

          <Card className="p-4 shadow-sm">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">Status</label>
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500 bg-white"
                >
                  <option value="">All Statuses</option>
                  <option value="ACTIVE">Active</option>
                  <option value="RESOLVED">Resolved</option>
                  <option value="CHRONIC">Chronic</option>
                  <option value="UNDER_TREATMENT">Under Treatment</option>
                </select>
              </div>
            </div>
          </Card>

          <div className="space-y-4">
            {filteredHistories.length === 0 ? (
              <Card className="p-10 text-center">
                <div className="mx-auto mb-3 w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center">
                  <Activity className="w-6 h-6 text-slate-500" />
                </div>
                <h3 className="text-xl font-semibold text-slate-900 mb-1">No medical history found</h3>
                <p className="text-slate-600">Add your records to keep them organized here.</p>
              </Card>
            ) : (
              <div className="space-y-4">
                {filteredHistories.map((history) => (
                  <Card key={history.id} className="p-5 shadow-sm hover:shadow-md transition-shadow">
                    <div className="flex flex-col gap-3">
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="text-xl font-semibold text-slate-900">{history.condition || 'Untitled Record'}</h3>
                        {history.status && (
                          <Badge
                            variant="outline"
                            className={`${getStatusClasses(history.status)} text-xs`}
                          >
                            {history.status.replace('_', ' ')}
                          </Badge>
                        )}
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 text-sm text-slate-700">
                        <div className="flex items-center gap-2">
                          <Calendar className="w-4 h-4 text-slate-400" />
                          <div>
                            <p className="text-xs uppercase text-slate-400">Diagnosis Date</p>
                            <p className="font-semibold text-slate-900">
                              {formatDate(history.diagnosisDate)}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <User className="w-4 h-4 text-slate-400" />
                          <div>
                            <p className="text-xs uppercase text-slate-400">Doctor</p>
                            <p className="font-semibold text-slate-900">{history.doctorName || 'N/A'}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <Building2 className="w-4 h-4 text-slate-400" />
                          <div>
                            <p className="text-xs uppercase text-slate-400">Hospital</p>
                            <p className="font-semibold text-slate-900">{history.hospitalName || 'N/A'}</p>
                          </div>
                        </div>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm text-slate-700">
                        <div>
                          <p className="font-semibold text-slate-900">Description</p>
                          <p>{history.description || 'No description provided'}</p>
                        </div>
                        <div>
                          <p className="font-semibold text-slate-900">Treatment</p>
                          <p>{history.treatment || 'No treatment information'}</p>
                        </div>
                        <div>
                          <p className="font-semibold text-slate-900">Medications</p>
                          <p>{history.medications || 'No medications listed'}</p>
                        </div>
                      </div>

                      <div className="flex flex-wrap gap-2 pt-3 border-t border-slate-200">
                        <Link href={`/patient/medical-history/${history.id}/edit`} className="w-full sm:w-auto">
                          <Button variant="outline" className="w-full">Edit</Button>
                        </Link>
                        <Button
                          variant="danger"
                          className="w-full sm:w-auto"
                          onClick={() => handleDelete(history.id)}
                        >
                          Delete
                        </Button>
                      </div>
                    </div>
                  </Card>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}

