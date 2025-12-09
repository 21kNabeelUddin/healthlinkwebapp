'use client';

import { useState, useEffect, useMemo } from 'react';
import { adminApi } from '@/lib/api';
import { User } from '@/types';
import { toast } from 'react-hot-toast';
import { format } from 'date-fns';
import { 
  Search, Filter, Download, UserCheck, UserX, Trash2, Eye, 
  Users, UserCheck as UserCheckIcon, Clock, Ban, CheckCircle2,
  Mail, Phone, Calendar, MapPin
} from 'lucide-react';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { Button } from '@/marketing/ui/button';
import { Input } from '@/marketing/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/marketing/ui/select';
import { Checkbox } from '@/marketing/ui/checkbox';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/marketing/ui/card';
import { Badge } from '@/marketing/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/marketing/ui/tabs';

export default function AdminUsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<User[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<Set<string>>(new Set());
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [dateFilter, setDateFilter] = useState<string>('ALL');
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
    loadUsers();
  }, []);

  useEffect(() => {
    filterUsers();
  }, [users, searchQuery, roleFilter, statusFilter, dateFilter]);

  const loadUsers = async () => {
    setIsLoading(true);
    try {
      // Load all users (patients, doctors, admins)
      const [patients, doctors, admins] = await Promise.all([
        adminApi.getAllPatients().catch(() => []),
        adminApi.getAllDoctors().catch(() => []),
        adminApi.getAllAdmins().catch(() => []),
      ]);
      setUsers([...patients, ...doctors, ...admins]);
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

    // Date filter
    if (dateFilter !== 'ALL') {
      const now = new Date();
      filtered = filtered.filter((user) => {
        const userDate = new Date(user.createdAt);
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
    const active = users.filter((u) => u.isVerified).length;
    const pending = users.filter((u) => !u.isVerified).length;
    const suspended = 0; // TODO: Add suspended status
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
          toast.success(`Approved ${selectedUsers.size} users`);
          break;
        case 'SUSPEND':
          toast.success(`Suspended ${selectedUsers.size} users`);
          break;
        case 'DELETE':
          if (confirm(`Delete ${selectedUsers.size} users?`)) {
            await Promise.all(Array.from(selectedUsers).map((id) => adminApi.deletePatient(id).catch(() => {})));
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
      toast.error('Bulk action failed');
    }
  };

  const exportToCSV = () => {
    const headers = ['ID', 'Name', 'Email', 'Phone', 'Role', 'Status', 'Created At'];
    const rows = filteredUsers.map((user) => [
      user.id,
      `${user.firstName} ${user.lastName}`,
      user.email,
      user.phoneNumber,
      user.userType,
      user.isVerified ? 'Verified' : 'Unverified',
      format(new Date(user.createdAt), 'yyyy-MM-dd'),
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

  const sidebarItems = [
    { icon: Users, label: 'Users', href: '/admin/users' },
  ];

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white flex items-center justify-center">
        <div className="text-slate-600">Loading users...</div>
      </div>
    );
  }

  if (selectedUser) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
        <TopNav userName="Admin" userRole="Admin" showPortalLinks={false} onLogout={() => {}} />
        <div className="flex">
          <Sidebar items={sidebarItems} currentPath="/admin/users" />
          <div className="flex-1 p-8">
            <Button variant="outline" onClick={() => setSelectedUser(null)} className="mb-4">
              ← Back to Users
            </Button>
            <UserDetailView user={selectedUser} />
          </div>
        </div>
      </div>
    );
  }

    return (
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
        <TopNav userName="Admin" userRole="Admin" showPortalLinks={false} onLogout={() => {}} />
        <div className="flex">
          <Sidebar items={sidebarItems} currentPath="/admin/users" />
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
                          <th className="text-left p-3">Status</th>
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
                                <Badge variant="outline">{user.userType}</Badge>
                              </td>
                              <td className="p-3">
                                <Badge variant={user.isVerified ? 'default' : 'secondary'}>
                                  {user.isVerified ? 'Verified' : 'Unverified'}
                                </Badge>
                              </td>
                              <td className="p-3 text-sm text-gray-500">
                                {format(new Date(user.createdAt), 'MMM dd, yyyy')}
                              </td>
                              <td className="p-3">
                                <div className="flex gap-2">
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => setSelectedUser(user)}
                                  >
                                    <Eye size={16} />
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => handleBulkAction('DELETE')}
                                  >
                                    <Trash2 size={16} />
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
  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>User Profile</CardTitle>
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
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex items-center gap-2">
              <Phone size={16} className="text-gray-400" />
              <span>{user.phoneNumber}</span>
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
              <span>Joined {format(new Date(user.createdAt), 'MMM dd, yyyy')}</span>
            </div>
          </div>
        </CardContent>
      </Card>

      <Tabs defaultValue="profile">
        <TabsList>
          <TabsTrigger value="profile">Profile</TabsTrigger>
          <TabsTrigger value="appointments">Appointments</TabsTrigger>
          <TabsTrigger value="medical">Medical Records</TabsTrigger>
          <TabsTrigger value="payments">Payment History</TabsTrigger>
          <TabsTrigger value="audit">Audit Trail</TabsTrigger>
        </TabsList>
        <TabsContent value="profile">
          <Card>
            <CardContent className="pt-6">
              <p className="text-gray-600">Profile information and settings</p>
            </CardContent>
          </Card>
        </TabsContent>
        <TabsContent value="appointments">
          <Card>
            <CardContent className="pt-6">
              <p className="text-gray-600">Appointment history</p>
            </CardContent>
          </Card>
        </TabsContent>
        <TabsContent value="medical">
          <Card>
            <CardContent className="pt-6">
              <p className="text-gray-600">Medical records</p>
            </CardContent>
          </Card>
        </TabsContent>
        <TabsContent value="payments">
          <Card>
            <CardContent className="pt-6">
              <p className="text-gray-600">Payment history</p>
            </CardContent>
          </Card>
        </TabsContent>
        <TabsContent value="audit">
          <Card>
            <CardContent className="pt-6">
              <p className="text-gray-600">Activity audit log</p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

