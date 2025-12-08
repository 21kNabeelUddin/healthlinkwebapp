'use client';

import { useMemo, useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { facilitiesApi } from '@/lib/api';
import { Clinic } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Link from 'next/link';
import Input from '@/components/ui/Input';
import { Badge } from '@/marketing/ui/badge';
import { MapPin, Phone, Mail, Clock, DollarSign, Building2, Plus, Power } from 'lucide-react';

export default function ClinicsPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [stateFilter, setStateFilter] = useState<string>('all');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [searchTerm, setSearchTerm] = useState<string>('');

  useEffect(() => {
    if (user?.id) {
      loadClinics();
    }
  }, [user]);

  const loadClinics = async () => {
    if (!user?.id) return;
    
    setIsLoading(true);
    try {
      const data = await facilitiesApi.listForDoctor(user.id.toString());
      setClinics(data || []);
    } catch (error: any) {
      toast.error('Failed to load clinics');
      console.error('Clinics load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleToggleStatus = async (clinic: Clinic) => {
    try {
      if (clinic.active) {
        await facilitiesApi.deactivate(clinic.id.toString());
      } else {
        await facilitiesApi.activate(clinic.id.toString());
      }
      toast.success(`Clinic ${clinic.active ? 'deactivated' : 'activated'} successfully`);
      loadClinics();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to update clinic status');
    }
  };

  const handleDelete = async (clinicId: string) => {
    if (!confirm('Are you sure you want to delete this clinic?')) return;

    try {
      await facilitiesApi.deactivate(clinicId);
      toast.success('Clinic deleted');
      loadClinics();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to delete clinic');
    }
  };

  const filteredClinics = useMemo(() => {
    return clinics.filter((clinic) => {
      const matchesState = stateFilter === 'all' || (clinic.state && clinic.state.toLowerCase() === stateFilter);
      const matchesStatus =
        statusFilter === 'all' || (statusFilter === 'active' ? clinic.active : !clinic.active);
      const term = searchTerm.toLowerCase().trim();
      const matchesSearch =
        !term ||
        clinic.name.toLowerCase().includes(term) ||
        (clinic.city || '').toLowerCase().includes(term) ||
        (clinic.address || '').toLowerCase().includes(term);
      return matchesState && matchesStatus && matchesSearch;
    });
  }, [clinics, stateFilter, statusFilter, searchTerm]);

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="DOCTOR">
        <div className="text-center py-12 text-slate-600">Loading clinics...</div>
      </DashboardLayout>
    );
  }

  const uniqueStates = Array.from(
    new Set(
      clinics
        .map((c) => c.state?.toLowerCase())
        .filter(Boolean)
    )
  );

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
          {/* Header */}
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <p className="text-sm text-slate-500 uppercase tracking-wide">Clinics</p>
              <h1 className="text-4xl font-bold text-slate-900">My Clinics</h1>
              <p className="text-slate-600">Manage your clinic locations and services</p>
            </div>
            <Link href="/doctor/clinics/new">
              <Button className="inline-flex items-center gap-2 bg-gradient-to-r from-teal-500 to-violet-600 text-white">
                <Plus className="w-4 h-4" />
                Add Clinic
              </Button>
            </Link>
          </div>

          {/* Filters */}
          <Card className="p-4">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="md:col-span-2">
                <Input
                  label="Search"
                  placeholder="Search by clinic name, city, or address"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="bg-white"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">State</label>
                <select
                  value={stateFilter}
                  onChange={(e) => setStateFilter(e.target.value)}
                  className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500 bg-white"
                >
                  <option value="all">All States</option>
                  {uniqueStates.map((state) => (
                    <option key={state} value={state!}>
                      {state}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">Status</label>
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500 bg-white"
                >
                  <option value="all">All Statuses</option>
                  <option value="active">Active</option>
                  <option value="inactive">Deactivated</option>
                </select>
              </div>
            </div>
          </Card>

          {/* Clinics List */}
          {filteredClinics.length === 0 ? (
            <Card className="p-10 text-center">
              <div className="mx-auto mb-3 w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center">
                <Building2 className="w-6 h-6 text-slate-500" />
              </div>
              <h3 className="text-xl font-semibold text-slate-900 mb-1">No clinics found</h3>
              <p className="text-slate-600 mb-4">Create your first clinic to start accepting appointments.</p>
              <Link href="/doctor/clinics/new">
                <Button className="inline-flex items-center gap-2 bg-gradient-to-r from-teal-500 to-violet-600 text-white">
                  <Plus className="w-4 h-4" />
                  Add Clinic
                </Button>
              </Link>
            </Card>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
              {filteredClinics.map((clinic) => (
                <Card key={clinic.id} className="overflow-hidden shadow-sm hover:shadow-md transition-shadow">
                  <div className="bg-gradient-to-r from-teal-50 to-violet-50 border-b border-slate-200 p-4 flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-3 mb-2">
                        <div className="w-10 h-10 rounded-lg bg-white border border-slate-200 flex items-center justify-center">
                          <Building2 className="w-5 h-5 text-teal-600" />
                        </div>
                        <div>
                          <h3 className="text-xl font-semibold text-slate-900">{clinic.name}</h3>
                          <div className="flex items-center gap-2 mt-1">
                            <Badge variant="outline" className={clinic.active ? 'bg-green-100 text-green-700 border-green-200' : 'bg-slate-100 text-slate-700 border-slate-200'}>
                              {clinic.active ? 'Active' : 'Deactivated'}
                            </Badge>
                            {clinic.consultationFee !== undefined && (
                              <Badge variant="outline" className="bg-white text-slate-700 border-slate-200">
                                <DollarSign className="w-3 h-3 inline mr-1" />
                                {clinic.consultationFee}
                              </Badge>
                            )}
                          </div>
                        </div>
                      </div>
                      {clinic.address && (
                        <div className="flex items-center gap-2 text-sm text-slate-700">
                          <MapPin className="w-4 h-4 text-rose-500" />
                          <span>{clinic.address}{clinic.city ? `, ${clinic.city}` : ''}{clinic.state ? `, ${clinic.state}` : ''}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="p-4 space-y-3 text-sm text-slate-700">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      <div className="flex items-center gap-2">
                        <Phone className="w-4 h-4 text-slate-400" />
                        <span>{clinic.phoneNumber || '—'}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Mail className="w-4 h-4 text-slate-400" />
                        <span className="break-all">{clinic.email || '—'}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Clock className="w-4 h-4 text-slate-400" />
                        <span>{clinic.openingTime && clinic.closingTime ? `${clinic.openingTime} - ${clinic.closingTime}` : 'Hours not set'}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Power className={`w-4 h-4 ${clinic.active ? 'text-green-500' : 'text-slate-400'}`} />
                        <span>{clinic.active ? 'Accepting appointments' : 'Inactive'}</span>
                      </div>
                    </div>
                  </div>

                  <div className="p-4 flex flex-col sm:flex-row gap-2 border-t border-slate-200 bg-white">
                    <Link href={`/doctor/clinics/${clinic.id.toString()}/edit`} className="w-full">
                      <Button variant="outline" className="w-full">Edit</Button>
                    </Link>
                    <Button
                      variant="secondary"
                      className="w-full"
                      onClick={() => handleToggleStatus(clinic)}
                    >
                      {clinic.active ? 'Deactivate' : 'Activate'}
                    </Button>
                    <Button
                      variant="danger"
                      className="w-full"
                      onClick={() => handleDelete(clinic.id.toString())}
                    >
                      Delete
                    </Button>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}

