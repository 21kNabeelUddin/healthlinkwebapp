'use client';

import { useState, useEffect, useMemo } from 'react';
import { adminApi } from '@/lib/api';
import { User } from '@/types';
import { toast } from 'react-hot-toast';
import { format, parseISO } from 'date-fns';
import {
  Stethoscope, Search, Filter, Download, CheckCircle2, XCircle, 
  Calendar, TrendingUp, DollarSign, Award, AlertTriangle, Eye, 
  Settings, Ban, UserCheck, Clock, Star, Mail, Phone, Building2
} from 'lucide-react';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { adminSidebarItems } from '@/app/admin/sidebar-items';
import { Button } from '@/marketing/ui/button';
import { Input } from '@/marketing/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/marketing/ui/select';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/marketing/ui/card';
import { Badge } from '@/marketing/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/marketing/ui/tabs';
import { Switch } from '@/marketing/ui/switch';
import { Label } from '@/marketing/ui/label';
import { facilitiesApi, adminApi as adminApiFull } from '@/lib/api';
import { Clinic, Appointment } from '@/types';

interface DoctorStats {
  total: number;
  verified: number;
  pending: number;
  suspended: number;
  averageRating: number;
  totalRevenue: number;
}

export default function AdminDoctorsPage() {
  const [doctors, setDoctors] = useState<User[]>([]);
  const [filteredDoctors, setFilteredDoctors] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [verificationFilter, setVerificationFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedDoctor, setSelectedDoctor] = useState<User | null>(null);
  const [doctorAppointments, setDoctorAppointments] = useState<Appointment[]>([]);
  const [doctorClinics, setDoctorClinics] = useState<Clinic[]>([]);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [autoApproveEnabled, setAutoApproveEnabled] = useState(false);
  const [autoApproveRules, setAutoApproveRules] = useState({
    minYearsExperience: 2,
    requirePMDC: true,
    requireSpecialization: true,
  });

  useEffect(() => {
    loadDoctors();
  }, []);

  useEffect(() => {
    filterDoctors();
  }, [doctors, searchQuery, verificationFilter, statusFilter]);

  const loadDoctors = async () => {
    setIsLoading(true);
    try {
      const data = await adminApi.getAllDoctors();
      // Ensure data is always an array
      const doctorsArray = Array.isArray(data) ? data : [];
      const normalized = doctorsArray.map((d) => {
        const normalizedId = d.id?.toString();
        const isVerified =
          (d as any).isEmailVerified ??
          (d as any).emailVerified ??
          (d as any).is_email_verified ??
          d.isVerified ??
          false;

        const approvalStatus =
          (d as any).approvalStatus ??
          (d as any).approval_status ??
          d.approvalStatus ??
          'PENDING';

        const isActive =
          (d as any).isActive ??
          (d as any).active ??
          (d as any).is_active ??
          d.isActive ??
          true;

        const specialization =
          d.specialization ??
          (d as any).speciality ??
          (d as any).specializationName ??
          'N/A';

        const pmdcId =
          d.pmdcId ??
          (d as any).pmdc_id ??
          d.licenseNumber ??
          (d as any).license_number ??
          'N/A';

        const yearsOfExperience =
          d.yearsOfExperience ??
          (d as any).experienceYears ??
          (d as any).years_of_experience ??
          0;

        return {
          ...d,
          id: normalizedId,
          isVerified,
          approvalStatus,
          isActive,
          specialization,
          pmdcId,
          yearsOfExperience,
        };
      });
      setDoctors(normalized);
    } catch (error: any) {
      toast.error('Failed to load doctors');
      console.error('Doctors load error:', error);
      setDoctors([]);
    } finally {
      setIsLoading(false);
    }
  };

  const filterDoctors = () => {
    let filtered = [...doctors];

    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(d =>
        d.firstName?.toLowerCase().includes(query) ||
        d.lastName?.toLowerCase().includes(query) ||
        d.email?.toLowerCase().includes(query) ||
        d.specialization?.toLowerCase().includes(query)
      );
    }

    if (verificationFilter !== 'ALL') {
      filtered = filtered.filter(d => {
        if (verificationFilter === 'VERIFIED') return d.isVerified;
        if (verificationFilter === 'PENDING') return !d.isVerified && d.approvalStatus === 'PENDING';
        if (verificationFilter === 'REJECTED') return d.approvalStatus === 'REJECTED';
        return true;
      });
    }

    if (statusFilter !== 'ALL') {
      filtered = filtered.filter(d => {
        if (statusFilter === 'ACTIVE') return d.isActive;
        if (statusFilter === 'SUSPENDED') return !d.isActive;
        return true;
      });
    }

    setFilteredDoctors(filtered);
  };

  const stats: DoctorStats = useMemo(() => {
    const total = doctors.length;
    const verified = doctors.filter(d => d.isVerified).length;
    const pending = doctors.filter(d => !d.isVerified && d.approvalStatus === 'PENDING').length;
    const suspended = doctors.filter(d => !d.isActive).length;
    const averageRating = doctors.reduce((sum, d) => sum + (d.averageRating || 0), 0) / total || 0;
    const totalRevenue = doctors.reduce((sum, d) => sum + (d.totalRevenue || 0), 0);

    return { total, verified, pending, suspended, averageRating, totalRevenue };
  }, [doctors]);

  const handleVerify = async (doctorId: string) => {
    try {
      await adminApi.approveUser(doctorId);
      toast.success('Doctor verified successfully');
      loadDoctors();
    } catch (error: any) {
      toast.error('Failed to verify doctor');
    }
  };

  const handleSuspend = async (doctorId: string) => {
    try {
      await adminApi.suspendUser(doctorId);
      toast.success('Doctor suspended successfully');
      loadDoctors();
    } catch (error: any) {
      toast.error('Failed to suspend doctor');
    }
  };

  const handleDelete = async (doctorId: string) => {
    if (!confirm('Are you sure you want to delete this doctor?')) return;
    try {
      await adminApi.deleteDoctor(doctorId);
      toast.success('Doctor deleted successfully');
      loadDoctors();
    } catch (error: any) {
      toast.error('Failed to delete doctor');
    }
  };

  const exportToCSV = () => {
    const headers = ['ID', 'Name', 'Email', 'Specialization', 'PMDC ID', 'Verified', 'Status', 'Rating', 'Experience'];
    const rows = filteredDoctors.map(d => [
      d.id,
      `${d.firstName} ${d.lastName}`,
      d.email,
      d.specialization || 'N/A',
      d.pmdcId || 'N/A',
      d.isVerified ? 'Yes' : 'No',
      d.isActive ? 'Active' : 'Suspended',
      d.averageRating?.toFixed(1) || '0.0',
      d.yearsOfExperience || 0,
    ]);

    const csv = [headers, ...rows].map(row => row.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `doctors-${format(new Date(), 'yyyy-MM-dd')}.csv`;
    a.click();
    toast.success('Doctors exported to CSV');
  };

  const loadDoctorDetails = async (doctor: User) => {
    setIsLoadingDetails(true);
    try {
      const [allAppointments, clinics] = await Promise.all([
        adminApiFull.getAllAppointments().catch(() => []),
        facilitiesApi.listForDoctor(doctor.id).catch(() => []),
      ]);
      const appointments = Array.isArray(allAppointments)
        ? allAppointments.filter((a: Appointment) => a.doctorId?.toString() === doctor.id?.toString())
        : [];
      const clinicsArr = Array.isArray(clinics) ? clinics : [];
      setDoctorAppointments(appointments);
      setDoctorClinics(clinicsArr);
    } catch (e) {
      setDoctorAppointments([]);
      setDoctorClinics([]);
    } finally {
      setIsLoadingDetails(false);
    }
  };

  useEffect(() => {
    if (selectedDoctor?.id) {
      loadDoctorDetails(selectedDoctor);
    } else {
      setDoctorAppointments([]);
      setDoctorClinics([]);
    }
  }, [selectedDoctor]);

  if (selectedDoctor) {
    const d = selectedDoctor;
    return (
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
        <TopNav userName="Admin" userRole="Admin" showPortalLinks={false} onLogout={() => {}} />
        <div className="flex">
          <Sidebar items={adminSidebarItems} currentPath="/admin/doctors" />
          <main className="flex-1 p-4 sm:px-6 lg:px-8">
            <div className="max-w-6xl mx-auto space-y-6">
              <Button variant="outline" onClick={() => setSelectedDoctor(null)}>
                ← Back to Doctors
              </Button>

              <Card className="p-6 shadow-sm">
                <div className="grid md:grid-cols-2 gap-6">
                  <div className="space-y-3">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 text-white flex items-center justify-center text-lg font-semibold">
                        {d.firstName?.[0]}
                      </div>
                      <div>
                        <h3 className="text-xl font-semibold text-slate-900">
                          {d.firstName} {d.lastName}
                        </h3>
                        <div className="flex gap-2 flex-wrap text-xs mt-1">
                          <Badge variant="outline">DOCTOR</Badge>
                          <Badge variant={d.isVerified ? 'default' : 'secondary'}>
                            {d.isVerified ? 'Verified' : 'Unverified'}
                          </Badge>
                          <Badge variant={d.isActive ? 'default' : 'destructive'}>
                            {d.isActive ? 'Active' : 'Suspended'}
                          </Badge>
                          <Badge variant="outline">{d.approvalStatus || 'PENDING'}</Badge>
                        </div>
                      </div>
                    </div>
                    <div className="text-sm text-slate-700 space-y-2">
                      <div className="flex items-center gap-2"><Mail size={14} className="text-slate-400" /> {d.email}</div>
                      <div className="flex items-center gap-2"><Phone size={14} className="text-slate-400" /> {d.phoneNumber || 'N/A'}</div>
                    </div>
                  </div>
                  <div className="text-sm text-slate-700 space-y-2">
                    <div><span className="text-xs uppercase text-slate-400">Specialization</span><div className="text-slate-900">{d.specialization || 'N/A'}</div></div>
                    <div><span className="text-xs uppercase text-slate-400">PMDC</span><div className="text-slate-900">{d.pmdcId || 'N/A'}</div></div>
                    <div><span className="text-xs uppercase text-slate-400">Experience</span><div className="text-slate-900">{d.yearsOfExperience ?? 0} years</div></div>
                  </div>
                </div>
              </Card>

              <Tabs defaultValue="profile">
                <TabsList>
                  <TabsTrigger value="profile">Profile</TabsTrigger>
                  <TabsTrigger value="clinics">Clinics</TabsTrigger>
                  <TabsTrigger value="appointments">Appointments</TabsTrigger>
                  <TabsTrigger value="audit">Audit Trail</TabsTrigger>
                </TabsList>

                <TabsContent value="profile">
                  <Card className="p-5">
                    <h4 className="font-semibold mb-2">Account Information</h4>
                    <div className="text-sm text-slate-700 space-y-1">
                      <div>Email: {d.email}</div>
                      <div>Phone: {d.phoneNumber || 'N/A'}</div>
                      <div>Status: {d.isVerified ? 'Verified' : 'Unverified'}</div>
                      <div>Account Status: {d.isActive ? 'Active' : 'Suspended'}</div>
                      <div>Approval Status: {d.approvalStatus || 'PENDING'}</div>
                      <div>Joined: {d.createdAt ? format(parseISO(d.createdAt), 'MMM dd, yyyy') : 'N/A'}</div>
                    </div>
                    <h4 className="font-semibold mt-4 mb-2">Doctor Information</h4>
                    <div className="text-sm text-slate-700 space-y-1">
                      <div>Specialization: {d.specialization || 'N/A'}</div>
                      <div>PMDC ID: {d.pmdcId || 'N/A'}</div>
                      <div>Years of Experience: {d.yearsOfExperience ?? 0}</div>
                    </div>
                  </Card>
                </TabsContent>

                <TabsContent value="clinics">
                  <Card className="p-5">
                    <div className="flex items-center gap-2 mb-3">
                      <Building2 className="w-4 h-4 text-teal-600" />
                      <h4 className="font-semibold">Clinics</h4>
                    </div>
                    {isLoadingDetails ? (
                      <p className="text-sm text-slate-600">Loading clinics...</p>
                    ) : doctorClinics.length === 0 ? (
                      <p className="text-sm text-slate-600">No clinics found.</p>
                    ) : (
                      <div className="space-y-3 text-sm">
                        {doctorClinics.map((c) => (
                          <div key={c.id} className="border rounded-lg p-3">
                            <div className="flex justify-between">
                              <div>
                                <div className="font-semibold">{c.name}</div>
                                <div className="text-slate-600">{c.address}</div>
                              </div>
                              <Badge variant={c.active ? 'default' : 'secondary'}>
                                {c.active ? 'Active' : 'Inactive'}
                              </Badge>
                            </div>
                            <div className="text-slate-600 mt-1">{c.city}, {c.state}</div>
                          </div>
                        ))}
                      </div>
                    )}
                  </Card>
                </TabsContent>

                <TabsContent value="appointments">
                  <Card className="p-5">
                    <h4 className="font-semibold mb-2">Appointments</h4>
                    {isLoadingDetails ? (
                      <p className="text-sm text-slate-600">Loading appointments...</p>
                    ) : doctorAppointments.length === 0 ? (
                      <p className="text-sm text-slate-600">No appointments found.</p>
                    ) : (
                      <div className="space-y-3 text-sm">
                        {doctorAppointments.map((apt) => (
                          <div key={apt.id} className="border rounded-lg p-3">
                            <div className="flex justify-between">
                              <div>
                                <div className="font-semibold">{apt.reason || 'Appointment'}</div>
                                <div className="text-slate-600">
                                  {apt.appointmentDateTime
                                    ? format(new Date(apt.appointmentDateTime), 'MMM dd, yyyy h:mm a')
                                    : 'N/A'}
                                </div>
                              </div>
                              <Badge variant="outline">{apt.status}</Badge>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </Card>
                </TabsContent>

                <TabsContent value="audit">
                  <Card className="p-5">
                    <h4 className="font-semibold mb-2">Audit Trail</h4>
                    <p className="text-sm text-slate-600">Audit trail coming soon.</p>
                  </Card>
                </TabsContent>
              </Tabs>
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav userName="Admin" userRole="Admin" showPortalLinks={false} onLogout={() => {}} />
      <div className="flex">
        <Sidebar items={adminSidebarItems} currentPath="/admin/doctors" />
        <main className="flex-1 p-4 sm:px-6 lg:px-8">
          <div className="max-w-7xl mx-auto space-y-8">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h1 className="text-3xl text-slate-900 mb-2">Doctor Management</h1>
                <p className="text-slate-600">Manage doctor registrations, verifications, and performance</p>
              </div>
              <div className="flex gap-3">
                <Button variant="outline" onClick={exportToCSV}>
                  <Download className="w-4 h-4 mr-2" />
                  Export CSV
                </Button>
              </div>
            </div>

            {/* Stats Cards */}
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-6">
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Total Doctors</CardDescription>
                  <CardTitle className="text-2xl">{stats.total}</CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Verified</CardDescription>
                  <CardTitle className="text-2xl text-green-600">{stats.verified}</CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Pending</CardDescription>
                  <CardTitle className="text-2xl text-yellow-600">{stats.pending}</CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Suspended</CardDescription>
                  <CardTitle className="text-2xl text-red-600">{stats.suspended}</CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Avg Rating</CardDescription>
                  <CardTitle className="text-2xl flex items-center gap-1">
                    <Star className="w-4 h-4 text-yellow-500 fill-yellow-500" />
                    {stats.averageRating.toFixed(1)}
                  </CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Total Revenue</CardDescription>
                  <CardTitle className="text-2xl">PKR {stats.totalRevenue.toLocaleString()}</CardTitle>
                </CardHeader>
              </Card>
      </div>

            {/* Auto-Approve Rules */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle>Auto-Approve Rules</CardTitle>
                    <CardDescription>Automatically approve doctors meeting criteria</CardDescription>
                  </div>
                  <div className="flex items-center gap-2">
                    <Switch
                      checked={autoApproveEnabled}
                      onCheckedChange={setAutoApproveEnabled}
                    />
                    <Label>Enable Auto-Approve</Label>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="grid md:grid-cols-3 gap-4">
                  <div>
                    <Label>Min Years Experience</Label>
                    <Input
                      type="number"
                      value={autoApproveRules.minYearsExperience}
                      onChange={(e) => setAutoApproveRules({
                        ...autoApproveRules,
                        minYearsExperience: parseInt(e.target.value) || 0
                      })}
                      disabled={!autoApproveEnabled}
                    />
                  </div>
                  <div className="flex items-center gap-2 pt-6">
                    <Switch
                      checked={autoApproveRules.requirePMDC}
                      onCheckedChange={(checked) => setAutoApproveRules({
                        ...autoApproveRules,
                        requirePMDC: checked
                      })}
                      disabled={!autoApproveEnabled}
                    />
                    <Label>Require PMDC</Label>
                  </div>
                  <div className="flex items-center gap-2 pt-6">
                    <Switch
                      checked={autoApproveRules.requireSpecialization}
                      onCheckedChange={(checked) => setAutoApproveRules({
                        ...autoApproveRules,
                        requireSpecialization: checked
                      })}
                      disabled={!autoApproveEnabled}
                    />
                    <Label>Require Specialization</Label>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Filters */}
            <Card>
              <CardContent className="pt-6">
                <div className="grid md:grid-cols-4 gap-4">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-slate-400" />
                    <Input
                      placeholder="Search doctors..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="pl-10"
                    />
                  </div>
                  <Select value={verificationFilter} onValueChange={setVerificationFilter}>
                    <SelectTrigger>
                      <SelectValue placeholder="Verification Status" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">All Verification</SelectItem>
                      <SelectItem value="VERIFIED">Verified</SelectItem>
                      <SelectItem value="PENDING">Pending</SelectItem>
                      <SelectItem value="REJECTED">Rejected</SelectItem>
                    </SelectContent>
                  </Select>
                  <Select value={statusFilter} onValueChange={setStatusFilter}>
                    <SelectTrigger>
                      <SelectValue placeholder="Account Status" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">All Status</SelectItem>
                      <SelectItem value="ACTIVE">Active</SelectItem>
                      <SelectItem value="SUSPENDED">Suspended</SelectItem>
                    </SelectContent>
                  </Select>
                  <Button variant="outline" onClick={() => {
                    setSearchQuery('');
                    setVerificationFilter('ALL');
                    setStatusFilter('ALL');
                  }}>
                    <XCircle className="w-4 h-4 mr-2" />
                    Clear Filters
                  </Button>
                </div>
              </CardContent>
            </Card>

            {/* Doctors Table */}
      <Card>
              <CardHeader>
                <CardTitle>Doctors ({filteredDoctors.length})</CardTitle>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <div className="text-center py-8">Loading...</div>
                ) : filteredDoctors.length === 0 ? (
                  <div className="text-center py-8 text-slate-500">No doctors found</div>
                ) : (
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b">
                <th className="text-left p-3">Name</th>
                <th className="text-left p-3">Email</th>
                <th className="text-left p-3">Specialization</th>
                          <th className="text-left p-3">PMDC ID</th>
                          <th className="text-left p-3">Rating</th>
                          <th className="text-left p-3">Experience</th>
                          <th className="text-left p-3">Status</th>
                <th className="text-left p-3">Actions</th>
              </tr>
            </thead>
            <tbody>
                        {filteredDoctors.map((doctor) => (
                          <tr key={doctor.id} className="border-b hover:bg-slate-50">
                            <td className="p-3">
                              <div className="font-medium">{doctor.firstName} {doctor.lastName}</div>
                              {doctor.isVerified && (
                                <Badge variant="default" className="mt-1">
                                  <CheckCircle2 className="w-3 h-3 mr-1" />
                                  Verified
                                </Badge>
                              )}
                            </td>
                            <td className="p-3 text-sm text-slate-600">{doctor.email}</td>
                            <td className="p-3 text-sm">{doctor.specialization || 'N/A'}</td>
                            <td className="p-3 text-sm">{doctor.pmdcId || 'N/A'}</td>
                            <td className="p-3">
                              <div className="flex items-center gap-1">
                                <Star className="w-4 h-4 text-yellow-500 fill-yellow-500" />
                                <span className="text-sm">{doctor.averageRating?.toFixed(1) || '0.0'}</span>
                              </div>
                  </td>
                            <td className="p-3 text-sm">{doctor.yearsOfExperience || 0} years</td>
                    <td className="p-3">
                              <Badge variant={doctor.isActive ? "default" : "destructive"}>
                                {doctor.isActive ? 'Active' : 'Suspended'}
                              </Badge>
                    </td>
                    <td className="p-3">
                              <div className="flex gap-2">
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => setSelectedDoctor(doctor)}
                                >
                                  <Eye className="w-4 h-4" />
                                </Button>
                                {!doctor.isVerified && (
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => handleVerify(doctor.id.toString())}
                                  >
                                    <CheckCircle2 className="w-4 h-4" />
                                  </Button>
                                )}
                      <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => handleSuspend(doctor.id.toString())}
                      >
                                  <Ban className="w-4 h-4" />
                      </Button>
                              </div>
                    </td>
                  </tr>
                        ))}
            </tbody>
          </table>
        </div>
                )}
              </CardContent>
      </Card>
          </div>
        </main>
      </div>
    </div>
  );
}
