'use client';

import { useState, useEffect, useMemo } from 'react';
import { adminApi, appointmentsApi, medicalRecordsApi, prescriptionsApi, facilitiesApi } from '@/lib/api';
import { User, Appointment, MedicalHistory, Clinic } from '@/types';
import { toast } from 'react-hot-toast';
import { format } from 'date-fns';
import { 
  Search, Filter, Download, UserCheck, UserX, Trash2, Eye, 
  Users, UserCheck as UserCheckIcon, Clock, Ban, CheckCircle2,
  Mail, Phone, Calendar, MapPin
} from 'lucide-react';
import DashboardLayout from '@/components/layout/DashboardLayout';
import { Button } from '@/marketing/ui/button';
import { Input } from '@/marketing/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/marketing/ui/select';
import { Checkbox } from '@/marketing/ui/checkbox';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/marketing/ui/card';
import { Badge } from '@/marketing/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/marketing/ui/tabs';

// Helper function to safely format dates
const formatDate = (dateString: string | undefined | null): string => {
  if (!dateString) return 'N/A';
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return 'Invalid Date';
    }
    return format(date, 'MMM dd, yyyy');
  } catch (error) {
    return 'Invalid Date';
  }
};

// Helper function to safely format dates for CSV
const formatDateForCSV = (dateString: string | undefined | null): string => {
  if (!dateString) return 'N/A';
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return 'Invalid Date';
    }
    return format(date, 'yyyy-MM-dd');
  } catch (error) {
    return 'Invalid Date';
  }
};

export default function AdminUsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<User[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<Set<string>>(new Set());
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [approvalFilter, setApprovalFilter] = useState<string>('ALL');
  const [activeFilter, setActiveFilter] = useState<string>('ALL');
  const [dateFilter, setDateFilter] = useState<string>('ALL');
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [activeTab, setActiveTab] = useState('overview');

  const normalizeUser = (u: User): User => {
    const emailVerified =
      (u as any).isEmailVerified ??
      (u as any).emailVerified ??
      (u as any).is_email_verified ??
      (u as any).email_verified ??
      u.isVerified ??
      false;

    const approval =
      (u as any).approvalStatus ??
      (u as any).approval_status ??
      'PENDING';

    const active =
      (u as any).isActive ??
      (u as any).active ??
      (u as any).is_active ??
      u.isActive ??
      true;

    return {
      ...u,
      userType: u.userType || u.role,
      isVerified: emailVerified,
      isActive: active,
      approvalStatus: approval,
    };
  };

  useEffect(() => {
    loadUsers();
  }, []);

  useEffect(() => {
    filterUsers();
  }, [users, searchQuery, roleFilter, statusFilter, approvalFilter, activeFilter, dateFilter]);

  const loadUsers = async () => {
    setIsLoading(true);
    try {
      // Load all users (patients, doctors, admins)
      const [patients, doctors, admins] = await Promise.all([
        adminApi.getAllPatients().catch(() => []),
        adminApi.getAllDoctors().catch(() => []),
        adminApi.getAllAdmins().catch(() => []),
      ]);
      const all = [...patients, ...doctors, ...admins].map(normalizeUser);
      setUsers(all);
    } catch (error: any) {
      toast.error('Failed to load users');
      console.error('Users load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const filterUsers = () => {
    let filtered = [...users];

    // Search filter
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (user) =>
          user.firstName?.toLowerCase().includes(query) ||
          user.lastName?.toLowerCase().includes(query) ||
          user.email?.toLowerCase().includes(query) ||
          user.phoneNumber?.includes(query)
      );
    }

    // Role filter
    if (roleFilter !== 'ALL') {
      filtered = filtered.filter((user) => user.userType === roleFilter);
    }

    // Status filter
    if (statusFilter !== 'ALL') {
      if (statusFilter === 'VERIFIED') {
        filtered = filtered.filter((user) => user.isVerified);
      } else if (statusFilter === 'UNVERIFIED') {
        filtered = filtered.filter((user) => !user.isVerified);
      }
    }

    // Approval status filter
    if (approvalFilter !== 'ALL') {
      filtered = filtered.filter((user) => (user.approvalStatus || 'PENDING') === approvalFilter);
    }

    // Active status filter
    if (activeFilter !== 'ALL') {
      if (activeFilter === 'ACTIVE') {
        filtered = filtered.filter((user) => user.isActive !== false);
      } else if (activeFilter === 'SUSPENDED') {
        filtered = filtered.filter((user) => user.isActive === false);
      }
    }

    // Date filter
    if (dateFilter !== 'ALL') {
      const now = new Date();
      filtered = filtered.filter((user) => {
        if (!user.createdAt) return false;
        const userDate = new Date(user.createdAt);
        if (isNaN(userDate.getTime())) return false;
        switch (dateFilter) {
          case 'TODAY':
            return userDate.toDateString() === now.toDateString();
          case 'WEEK':
            const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
            return userDate >= weekAgo;
          case 'MONTH':
            const monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
            return userDate >= monthAgo;
          default:
            return true;
        }
      });
    }

    setFilteredUsers(filtered);
  };

  const stats = useMemo(() => {
    const total = users.length;
    const active = users.filter((u) => u.isActive !== false).length;
    const pending = users.filter((u) => (u.approvalStatus || 'PENDING') === 'PENDING').length;
    const suspended = users.filter((u) => u.isActive === false).length;
    return { total, active, pending, suspended };
  }, [users]);

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedUsers(new Set(filteredUsers.map((u) => u.id)));
    } else {
      setSelectedUsers(new Set());
    }
  };

  const handleSelectUser = (userId: string, checked: boolean) => {
    const newSelected = new Set(selectedUsers);
    if (checked) {
      newSelected.add(userId);
    } else {
      newSelected.delete(userId);
    }
    setSelectedUsers(newSelected);
  };

  const handleBulkAction = async (action: string) => {
    if (selectedUsers.size === 0) {
      toast.error('Please select at least one user');
      return;
    }

    try {
      switch (action) {
        case 'APPROVE':
          await Promise.all(
            Array.from(selectedUsers).map((id) => 
              adminApi.approveUser(id).catch((err) => {
                console.error(`Failed to approve user ${id}:`, err);
                throw err;
              })
            )
          );
          toast.success(`Approved ${selectedUsers.size} user(s)`);
          loadUsers();
          break;
        case 'SUSPEND':
          await Promise.all(
            Array.from(selectedUsers).map((id) => 
              adminApi.suspendUser(id).catch((err) => {
                console.error(`Failed to suspend user ${id}:`, err);
                throw err;
              })
            )
          );
          toast.success(`Suspended ${selectedUsers.size} user(s)`);
          loadUsers();
          break;
        case 'UNSUSPEND':
          await Promise.all(
            Array.from(selectedUsers).map((id) => 
              adminApi.unsuspendUser(id).catch((err) => {
                console.error(`Failed to unsuspend user ${id}:`, err);
                throw err;
              })
            )
          );
          toast.success(`Unsuspended ${selectedUsers.size} user(s)`);
          loadUsers();
          break;
        case 'DELETE':
          if (confirm(`Delete ${selectedUsers.size} users?`)) {
            await Promise.all(Array.from(selectedUsers).map((id) => adminApi.deleteUser(id).catch(() => {})));
            toast.success(`Deleted ${selectedUsers.size} users`);
            loadUsers();
          }
          break;
        case 'EXPORT':
          exportToCSV();
          break;
      }
      setSelectedUsers(new Set());
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Bulk action failed');
      console.error('Bulk action error:', error);
    }
  };

  const handleApproveUser = async (userId: string) => {
    try {
      await adminApi.approveUser(userId);
      toast.success('User approved successfully');
      loadUsers();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Failed to approve user');
      console.error('Approve user error:', error);
    }
  };

  const handleSuspendUser = async (userId: string) => {
    if (!confirm('Are you sure you want to suspend this user?')) return;
    try {
      await adminApi.suspendUser(userId);
      toast.success('User suspended successfully');
      loadUsers();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Failed to suspend user');
      console.error('Suspend user error:', error);
    }
  };

  const handleUnsuspendUser = async (userId: string) => {
    try {
      await adminApi.unsuspendUser(userId);
      toast.success('User reactivated successfully');
      loadUsers();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Failed to unsuspend user');
      console.error('Unsuspend user error:', error);
    }
  };

  const exportToCSV = () => {
    const headers = ['ID', 'Name', 'Email', 'Phone', 'Role', 'Verified', 'Approval', 'Active', 'Created At'];
    const rows = filteredUsers.map((user) => [
      user.id,
      `${user.firstName} ${user.lastName}`,
      user.email,
      user.phoneNumber,
      user.userType,
      user.isVerified ? 'Verified' : 'Unverified',
      user.approvalStatus || 'PENDING',
      user.isActive === false ? 'Suspended' : 'Active',
      formatDateForCSV(user.createdAt),
    ]);

    const csv = [headers, ...rows].map((row) => row.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `users-${format(new Date(), 'yyyy-MM-dd')}.csv`;
    a.click();
    toast.success('Users exported to CSV');
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white flex items-center justify-center">
        <div className="text-slate-600">Loading users...</div>
      </div>
    );
  }

  if (selectedUser) {
    return (
      <DashboardLayout requiredUserType="ADMIN">
        <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
          <div className="flex-1 p-8">
            <Button variant="outline" onClick={() => setSelectedUser(null)} className="mb-4">
              ← Back to Users
            </Button>
            <UserDetailView user={selectedUser} />
          </div>
        </div>
      </DashboardLayout>
    );
  }

    return (
      <DashboardLayout requiredUserType="ADMIN">
        <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
          <div className="flex-1 p-8">
          <div className="mb-8">
            <h1 className="text-4xl font-bold text-gray-800 mb-2">User Management</h1>
            <p className="text-gray-600">Manage all platform users</p>
          </div>

          {/* Quick Stats */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-gray-600">Total Users</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.total}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-gray-600">Active</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-green-600">{stats.active}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-gray-600">Pending</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-yellow-600">{stats.pending}</div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-gray-600">Suspended</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-red-600">{stats.suspended}</div>
              </CardContent>
            </Card>
          </div>

          <Tabs value={activeTab} onValueChange={setActiveTab}>
            <TabsList>
              <TabsTrigger value="overview">Overview</TabsTrigger>
              <TabsTrigger value="filters">Filters</TabsTrigger>
            </TabsList>

            <TabsContent value="overview" className="space-y-4">
              {/* Search and Filters Bar */}
              <Card>
                <CardContent className="pt-6">
                  <div className="flex flex-col md:flex-row gap-4">
                    <div className="flex-1 relative">
                      <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                      <Input
                        placeholder="Search by name, email, phone..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="pl-10"
                      />
                    </div>
                    <Select value={roleFilter} onValueChange={setRoleFilter}>
                      <SelectTrigger className="w-[180px]">
                        <SelectValue placeholder="Role" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ALL">All Roles</SelectItem>
                        <SelectItem value="PATIENT">Patients</SelectItem>
                        <SelectItem value="DOCTOR">Doctors</SelectItem>
                        <SelectItem value="ADMIN">Admins</SelectItem>
                      </SelectContent>
                    </Select>
                    <Select value={statusFilter} onValueChange={setStatusFilter}>
                      <SelectTrigger className="w-[180px]">
                        <SelectValue placeholder="Status" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ALL">All Status</SelectItem>
                        <SelectItem value="VERIFIED">Verified</SelectItem>
                        <SelectItem value="UNVERIFIED">Unverified</SelectItem>
                      </SelectContent>
                    </Select>
                    <Select value={approvalFilter} onValueChange={setApprovalFilter}>
                      <SelectTrigger className="w-[200px]">
                        <SelectValue placeholder="Approval" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ALL">All Approval</SelectItem>
                        <SelectItem value="APPROVED">Approved</SelectItem>
                        <SelectItem value="PENDING">Pending</SelectItem>
                        <SelectItem value="REJECTED">Rejected</SelectItem>
                      </SelectContent>
                    </Select>
                    <Select value={activeFilter} onValueChange={setActiveFilter}>
                      <SelectTrigger className="w-[200px]">
                        <SelectValue placeholder="Active Status" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ALL">All Activity</SelectItem>
                        <SelectItem value="ACTIVE">Active</SelectItem>
                        <SelectItem value="SUSPENDED">Suspended</SelectItem>
                      </SelectContent>
                    </Select>
                    <Select value={dateFilter} onValueChange={setDateFilter}>
                      <SelectTrigger className="w-[180px]">
                        <SelectValue placeholder="Date" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ALL">All Time</SelectItem>
                        <SelectItem value="TODAY">Today</SelectItem>
                        <SelectItem value="WEEK">This Week</SelectItem>
                        <SelectItem value="MONTH">This Month</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </CardContent>
              </Card>

              {/* Bulk Actions */}
              {selectedUsers.size > 0 && (
                <Card>
                  <CardContent className="pt-6">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">
                        {selectedUsers.size} user{selectedUsers.size !== 1 ? 's' : ''} selected
                      </span>
                      <div className="flex gap-2">
                        <Button variant="outline" size="sm" onClick={() => handleBulkAction('APPROVE')}>
                          <UserCheck className="mr-2" size={16} />
                          Approve
                        </Button>
                        <Button variant="outline" size="sm" onClick={() => handleBulkAction('SUSPEND')}>
                          <Ban className="mr-2" size={16} />
                          Suspend
                        </Button>
                        <Button variant="outline" size="sm" onClick={() => handleBulkAction('UNSUSPEND')}>
                          <CheckCircle2 className="mr-2" size={16} />
                          Unsuspend
                        </Button>
                        <Button variant="outline" size="sm" onClick={() => handleBulkAction('DELETE')}>
                          <Trash2 className="mr-2" size={16} />
                          Delete
                        </Button>
                        <Button variant="outline" size="sm" onClick={() => handleBulkAction('EXPORT')}>
                          <Download className="mr-2" size={16} />
                          Export
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Users Table */}
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle>Users ({filteredUsers.length})</CardTitle>
                      <CardDescription>All registered platform users</CardDescription>
                    </div>
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm" onClick={exportToCSV}>
                        <Download className="mr-2" size={16} />
                        Export CSV
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b">
                          <th className="text-left p-3 w-12">
                            <Checkbox
                              checked={selectedUsers.size === filteredUsers.length && filteredUsers.length > 0}
                              onCheckedChange={handleSelectAll}
                            />
                          </th>
                          <th className="text-left p-3">Avatar</th>
                          <th className="text-left p-3">Name</th>
                          <th className="text-left p-3">Email</th>
                          <th className="text-left p-3">Role</th>
                          <th className="text-left p-3">Verified</th>
                          <th className="text-left p-3">Approval</th>
                          <th className="text-left p-3">Active</th>
                          <th className="text-left p-3">Last Active</th>
                          <th className="text-left p-3">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredUsers.length === 0 ? (
                          <tr>
                            <td colSpan={8} className="text-center p-8 text-gray-500">
                              No users found
                            </td>
                          </tr>
                        ) : (
                          filteredUsers.map((user) => (
                            <tr key={user.id} className="border-b hover:bg-gray-50">
                              <td className="p-3">
                                <Checkbox
                                  checked={selectedUsers.has(user.id)}
                                  onCheckedChange={(checked) => handleSelectUser(user.id, checked as boolean)}
                                />
                              </td>
                              <td className="p-3">
                                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white font-semibold">
                                  {user.firstName?.[0]}{user.lastName?.[0]}
                                </div>
                              </td>
                              <td className="p-3 font-medium">
                                {user.firstName} {user.lastName}
                              </td>
                              <td className="p-3 text-gray-600">{user.email}</td>
                              <td className="p-3">
                                <Badge variant="outline">{user.userType || user.role || 'N/A'}</Badge>
                              </td>
                              <td className="p-3">
                                <Badge variant={user.isVerified ? 'default' : 'secondary'}>
                                  {user.isVerified ? 'Verified' : 'Unverified'}
                                </Badge>
                              </td>
                              <td className="p-3">
                                <Badge variant={user.approvalStatus === 'APPROVED' ? 'default' : user.approvalStatus === 'REJECTED' ? 'destructive' : 'secondary'}>
                                  {user.approvalStatus || 'Pending'}
                                </Badge>
                              </td>
                              <td className="p-3">
                                <Badge variant={user.isActive === false ? 'destructive' : 'outline'}>
                                  {user.isActive === false ? 'Suspended' : 'Active'}
                                </Badge>
                              </td>
                              <td className="p-3 text-sm text-gray-500">
                                {formatDate(user.createdAt)}
                              </td>
                              <td className="p-3">
                                <div className="flex gap-2">
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => setSelectedUser(user)}
                                    title="View Details"
                                  >
                                    <Eye size={16} />
                                  </Button>
                                  {(!user.isVerified || user.approvalStatus === 'PENDING') && (
                                    <Button
                                      variant="ghost"
                                      size="sm"
                                      onClick={() => handleApproveUser(user.id)}
                                      title="Approve User"
                                    >
                                      <UserCheck size={16} className="text-green-600" />
                                    </Button>
                                  )}
                                  {user.isActive !== false && (
                                    <Button
                                      variant="ghost"
                                      size="sm"
                                      onClick={() => handleSuspendUser(user.id)}
                                      title="Suspend User"
                                    >
                                      <Ban size={16} className="text-orange-600" />
                                    </Button>
                                  )}
                                  {user.isActive === false && (
                                    <Button
                                      variant="ghost"
                                      size="sm"
                                      onClick={() => handleUnsuspendUser(user.id)}
                                      title="Unsuspend User"
                                    >
                                      <CheckCircle2 size={16} className="text-green-600" />
                                    </Button>
                                  )}
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => {
                                      if (confirm(`Delete user ${user.firstName} ${user.lastName}?`)) {
                                        adminApi.deleteUser(user.id).then(() => {
                                          toast.success('User deleted successfully');
                                          loadUsers();
                                        }).catch((err) => {
                                          toast.error('Failed to delete user');
                                          console.error(err);
                                        });
                                      }
                                    }}
                                    title="Delete User"
                                  >
                                    <Trash2 size={16} className="text-red-600" />
                                  </Button>
                                </div>
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}

function UserDetailView({ user }: { user: User }) {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [medicalHistory, setMedicalHistory] = useState<MedicalHistory[]>([]);
  const [prescriptions, setPrescriptions] = useState<any[]>([]);
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('profile');
  const isPatient = user.userType === 'PATIENT' || user.role === 'PATIENT';
  const isDoctor = user.userType === 'DOCTOR' || user.role === 'DOCTOR';

  useEffect(() => {
    if (activeTab === 'appointments' && appointments.length === 0) {
      loadAppointments();
    } else if (activeTab === 'medical' && medicalHistory.length === 0 && isPatient) {
      loadMedicalHistory();
    } else if (activeTab === 'prescriptions' && prescriptions.length === 0 && isPatient) {
      loadPrescriptions();
    } else if (activeTab === 'clinics' && clinics.length === 0 && isDoctor) {
      loadClinics();
    }
  }, [activeTab, user.id]);

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      // For admin viewing, we need to get appointments by user ID
      // Since the API uses the authenticated user's email, we'll need to use admin endpoints
      const allAppointments = await adminApi.getAllAppointments();
      // Filter appointments for this user
      const userAppointments = allAppointments.filter((apt: Appointment) => {
        if (isPatient) {
          return apt.patientId?.toString() === user.id;
        } else if (isDoctor) {
          return apt.doctorId?.toString() === user.id;
        }
        return false;
      });
      setAppointments(userAppointments);
    } catch (error: any) {
      console.error('Failed to load appointments:', error);
      toast.error('Failed to load appointments');
    } finally {
      setIsLoading(false);
    }
  };

  const loadMedicalHistory = async () => {
    if (!isPatient) return;
    setIsLoading(true);
    try {
      const history = await medicalRecordsApi.listForPatient(user.id);
      setMedicalHistory(Array.isArray(history) ? history : []);
    } catch (error: any) {
      console.error('Failed to load medical history:', error);
      toast.error('Failed to load medical history');
      setMedicalHistory([]);
    } finally {
      setIsLoading(false);
    }
  };

  const loadPrescriptions = async () => {
    if (!isPatient) return;
    setIsLoading(true);
    try {
      // Get prescriptions for all appointments of this patient
      const userPrescriptions: any[] = [];
      for (const apt of appointments) {
        try {
          const prescription = await prescriptionsApi.getByAppointmentId(apt.id.toString());
          if (prescription) {
            userPrescriptions.push(prescription);
          }
        } catch (error) {
          // Prescription not found for this appointment, skip
        }
      }
      setPrescriptions(userPrescriptions);
    } catch (error: any) {
      console.error('Failed to load prescriptions:', error);
      toast.error('Failed to load prescriptions');
    } finally {
      setIsLoading(false);
    }
  };

  const loadClinics = async () => {
    if (!isDoctor) return;
    setIsLoading(true);
    try {
      const doctorClinics = await facilitiesApi.listForDoctor(user.id);
      setClinics(Array.isArray(doctorClinics) ? doctorClinics : []);
    } catch (error: any) {
      console.error('Failed to load clinics:', error);
      toast.error('Failed to load clinics');
      setClinics([]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>User Profile</CardTitle>
          <CardDescription>
            {isPatient && 'Patient Profile'}
            {isDoctor && 'Doctor Profile'}
            {!isPatient && !isDoctor && 'User Profile'}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-4">
            <div className="w-20 h-20 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white text-2xl font-semibold">
              {user.firstName?.[0]}{user.lastName?.[0]}
            </div>
            <div>
              <h2 className="text-2xl font-bold">
                {user.firstName} {user.lastName}
              </h2>
              <p className="text-gray-600">{user.email}</p>
                  <div className="flex flex-wrap gap-2 mt-2">
                    <Badge variant="outline">{user.userType || user.role || 'N/A'}</Badge>
                    <Badge variant={user.isVerified ? 'default' : 'secondary'}>
                      {user.isVerified ? 'Verified' : 'Unverified'}
                    </Badge>
                    <Badge variant={user.approvalStatus === 'APPROVED' ? 'default' : user.approvalStatus === 'REJECTED' ? 'destructive' : 'secondary'}>
                      {user.approvalStatus || 'Pending'}
                    </Badge>
                    <Badge variant={user.isActive === false ? 'destructive' : 'outline'}>
                      {user.isActive === false ? 'Suspended' : 'Active'}
                    </Badge>
                  </div>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex items-center gap-2">
              <Phone size={16} className="text-gray-400" />
              <span>{user.phoneNumber || 'N/A'}</span>
            </div>
            <div className="flex items-center gap-2">
              <Mail size={16} className="text-gray-400" />
              <span>{user.email}</span>
            </div>
            {user.address && (
              <div className="flex items-center gap-2">
                <MapPin size={16} className="text-gray-400" />
                <span>{user.address}</span>
              </div>
            )}
            <div className="flex items-center gap-2">
              <Calendar size={16} className="text-gray-400" />
              <span>Joined {formatDate(user.createdAt)}</span>
            </div>
            {isDoctor && user.specialization && (
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium">Specialization:</span>
                <span>{user.specialization}</span>
              </div>
            )}
            {isDoctor && user.yearsOfExperience && (
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium">Experience:</span>
                <span>{user.yearsOfExperience} years</span>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="profile">Profile</TabsTrigger>
          <TabsTrigger value="appointments">Appointments ({appointments.length})</TabsTrigger>
          {isPatient && (
            <>
              <TabsTrigger value="medical">Medical Records ({medicalHistory.length})</TabsTrigger>
              <TabsTrigger value="prescriptions">Prescriptions ({prescriptions.length})</TabsTrigger>
            </>
          )}
          {isDoctor && (
            <TabsTrigger value="clinics">Clinics ({clinics.length})</TabsTrigger>
          )}
          <TabsTrigger value="audit">Audit Trail</TabsTrigger>
        </TabsList>
        <TabsContent value="profile">
          <Card>
            <CardContent className="pt-6">
              <div className="space-y-4">
                <div>
                  <h3 className="font-semibold mb-2">Account Information</h3>
                  <div className="space-y-2 text-sm">
                    <div><span className="font-medium">Email:</span> {user.email}</div>
                    <div><span className="font-medium">Phone:</span> {user.phoneNumber || 'N/A'}</div>
                    <div><span className="font-medium">Status:</span> {user.isVerified ? 'Verified' : 'Unverified'}</div>
                    <div><span className="font-medium">Account Status:</span> {user.isActive !== false ? 'Active' : 'Inactive'}</div>
                    {user.approvalStatus && (
                      <div><span className="font-medium">Approval Status:</span> {user.approvalStatus}</div>
                    )}
                  </div>
                </div>
                {isDoctor && (
                  <div>
                    <h3 className="font-semibold mb-2">Doctor Information</h3>
                    <div className="space-y-2 text-sm">
                      {user.specialization && <div><span className="font-medium">Specialization:</span> {user.specialization}</div>}
                      {user.yearsOfExperience && <div><span className="font-medium">Years of Experience:</span> {user.yearsOfExperience}</div>}
                      {user.licenseNumber && <div><span className="font-medium">License Number:</span> {user.licenseNumber}</div>}
                      {user.pmdcId && <div><span className="font-medium">PMDC ID:</span> {user.pmdcId}</div>}
                      {user.averageRating && <div><span className="font-medium">Average Rating:</span> {user.averageRating.toFixed(1)} / 5.0</div>}
                    </div>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
        <TabsContent value="appointments">
          <Card>
            <CardHeader>
              <CardTitle>Appointments</CardTitle>
              <CardDescription>
                {isPatient && 'All appointments for this patient'}
                {isDoctor && 'All appointments for this doctor'}
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-6">
              {isLoading ? (
                <p className="text-gray-600">Loading appointments...</p>
              ) : appointments.length === 0 ? (
                <p className="text-gray-600">No appointments found</p>
              ) : (
                <div className="space-y-4">
                  {appointments.map((apt) => (
                    <div key={apt.id} className="border rounded-lg p-4">
                      <div className="flex justify-between items-start">
                        <div>
                          <p className="font-medium">
                            {format(new Date(apt.appointmentDateTime), 'MMM dd, yyyy h:mm a')}
                          </p>
                          <p className="text-sm text-gray-600">{apt.reason}</p>
                          {isPatient && apt.doctorName && (
                            <p className="text-sm text-gray-500">Dr. {apt.doctorName}</p>
                          )}
                          {isDoctor && apt.patientName && (
                            <p className="text-sm text-gray-500">Patient: {apt.patientName}</p>
                          )}
                        </div>
                        <Badge variant={apt.status === 'COMPLETED' ? 'default' : apt.status === 'IN_PROGRESS' ? 'secondary' : 'outline'}>
                          {apt.status}
                        </Badge>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
        {isPatient && (
          <>
            <TabsContent value="medical">
              <Card>
                <CardHeader>
                  <CardTitle>Medical Records</CardTitle>
                  <CardDescription>Medical history for this patient</CardDescription>
                </CardHeader>
                <CardContent className="pt-6">
                  {isLoading ? (
                    <p className="text-gray-600">Loading medical records...</p>
                  ) : medicalHistory.length === 0 ? (
                    <p className="text-gray-600">No medical records found</p>
                  ) : (
                    <div className="space-y-4">
                      {medicalHistory.map((record) => (
                        <div key={record.id} className="border rounded-lg p-4">
                          <div className="flex justify-between items-start mb-2">
                            <p className="font-medium">{record.condition || 'Medical Record'}</p>
                            <span className="text-sm text-gray-500">
                              {record.diagnosisDate ? format(new Date(record.diagnosisDate), 'MMM dd, yyyy') : 'N/A'}
                            </span>
                          </div>
                          {record.description && <p className="text-sm text-gray-600">{record.description}</p>}
                          {record.treatment && <p className="text-sm text-gray-600 mt-1"><strong>Treatment:</strong> {record.treatment}</p>}
                          {record.medications && <p className="text-sm text-gray-600 mt-1"><strong>Medications:</strong> {record.medications}</p>}
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            </TabsContent>
            <TabsContent value="prescriptions">
              <Card>
                <CardHeader>
                  <CardTitle>Prescriptions</CardTitle>
                  <CardDescription>Prescription history for this patient</CardDescription>
                </CardHeader>
                <CardContent className="pt-6">
                  {isLoading ? (
                    <p className="text-gray-600">Loading prescriptions...</p>
                  ) : prescriptions.length === 0 ? (
                    <p className="text-gray-600">No prescriptions found</p>
                  ) : (
                    <div className="space-y-4">
                      {prescriptions.map((prescription, idx) => (
                        <div key={idx} className="border rounded-lg p-4">
                          <p className="font-medium">Prescription #{idx + 1}</p>
                          {prescription.medications && prescription.medications.length > 0 && (
                            <div className="mt-2">
                              <p className="text-sm font-medium">Medications:</p>
                              <ul className="list-disc list-inside text-sm text-gray-600">
                                {prescription.medications.map((med: any, medIdx: number) => (
                                  <li key={medIdx}>{med.name} - {med.dosage}</li>
                                ))}
                              </ul>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            </TabsContent>
          </>
        )}
        {isDoctor && (
          <TabsContent value="clinics">
            <Card>
              <CardHeader>
                <CardTitle>Clinics</CardTitle>
                <CardDescription>Clinics associated with this doctor</CardDescription>
              </CardHeader>
              <CardContent className="pt-6">
                {isLoading ? (
                  <p className="text-gray-600">Loading clinics...</p>
                ) : clinics.length === 0 ? (
                  <p className="text-gray-600">No clinics found</p>
                ) : (
                  <div className="space-y-4">
                    {clinics.map((clinic) => (
                      <div key={clinic.id} className="border rounded-lg p-4">
                        <div className="flex justify-between items-start mb-2">
                          <div>
                            <p className="font-medium text-lg">{clinic.name}</p>
                            <p className="text-sm text-gray-600 mt-1">{clinic.address}</p>
                            {clinic.city && clinic.state && (
                              <p className="text-sm text-gray-500">
                                {clinic.city}, {clinic.state} {clinic.zipCode}
                              </p>
                            )}
                          </div>
                          <Badge variant={clinic.active ? 'default' : 'secondary'}>
                            {clinic.active ? 'Active' : 'Inactive'}
                          </Badge>
                        </div>
                        <div className="grid grid-cols-2 gap-4 mt-3 text-sm">
                          {clinic.phoneNumber && (
                            <div className="flex items-center gap-2">
                              <Phone size={14} className="text-gray-400" />
                              <span>{clinic.phoneNumber}</span>
                            </div>
                          )}
                          {clinic.email && (
                            <div className="flex items-center gap-2">
                              <Mail size={14} className="text-gray-400" />
                              <span>{clinic.email}</span>
                            </div>
                          )}
                          {clinic.openingTime && clinic.closingTime && (
                            <div className="flex items-center gap-2">
                              <Calendar size={14} className="text-gray-400" />
                              <span>{clinic.openingTime} - {clinic.closingTime}</span>
                            </div>
                          )}
                          {clinic.consultationFee && (
                            <div className="flex items-center gap-2">
                              <span className="text-gray-400">Fee:</span>
                              <span>PKR {clinic.consultationFee}</span>
                            </div>
                          )}
                        </div>
                        {clinic.description && (
                          <p className="text-sm text-gray-600 mt-3">{clinic.description}</p>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        )}
        <TabsContent value="audit">
          <Card>
            <CardContent className="pt-6">
              <p className="text-gray-600">Activity audit log</p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
        </div>
      </div>
    </DashboardLayout>
  );
}

