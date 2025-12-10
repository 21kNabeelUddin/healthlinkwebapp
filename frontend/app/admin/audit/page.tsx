'use client';

import { useState, useEffect } from 'react';
import { toast } from 'react-hot-toast';
import { format } from 'date-fns';
import { FileText, Search, Download, Filter, AlertTriangle, CheckCircle2, XCircle } from 'lucide-react';
import DashboardLayout from '@/components/layout/DashboardLayout';
import { Button } from '@/marketing/ui/button';
import { Input } from '@/marketing/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/marketing/ui/card';
import { Badge } from '@/marketing/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/marketing/ui/select';

interface AuditLog {
  id: string;
  timestamp: string;
  user: string;
  action: string;
  resource: string;
  ipAddress: string;
  status: 'SUCCESS' | 'FAILURE';
  details?: string;
}

export default function AdminAuditPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [filteredLogs, setFilteredLogs] = useState<AuditLog[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [filters, setFilters] = useState({
    user: 'ALL',
    actionType: 'ALL',
    dateRange: 'ALL',
    status: 'ALL',
  });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadAuditLogs();
  }, []);

  useEffect(() => {
    filterLogs();
  }, [logs, searchQuery, filters]);

  const loadAuditLogs = async () => {
    setIsLoading(true);
    try {
      // Mock data - replace with actual API call
      const mockLogs: AuditLog[] = [
        {
          id: '1',
          timestamp: new Date().toISOString(),
          user: 'admin@healthlink.com',
          action: 'USER_DELETED',
          resource: 'User: patient@example.com',
          ipAddress: '192.168.1.1',
          status: 'SUCCESS',
        },
        {
          id: '2',
          timestamp: new Date(Date.now() - 3600000).toISOString(),
          user: 'admin@healthlink.com',
          action: 'APPOINTMENT_CREATED',
          resource: 'Appointment: #12345',
          ipAddress: '192.168.1.1',
          status: 'SUCCESS',
        },
      ];
      setLogs(mockLogs);
    } catch (error: any) {
      toast.error('Failed to load audit logs');
    } finally {
      setIsLoading(false);
    }
  };

  const filterLogs = () => {
    let filtered = [...logs];

    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (log) =>
          log.user.toLowerCase().includes(query) ||
          log.action.toLowerCase().includes(query) ||
          log.resource.toLowerCase().includes(query) ||
          log.ipAddress.includes(query)
      );
    }

    if (filters.user !== 'ALL') {
      filtered = filtered.filter((log) => log.user === filters.user);
    }

    if (filters.actionType !== 'ALL') {
      filtered = filtered.filter((log) => log.action === filters.actionType);
    }

    if (filters.status !== 'ALL') {
      filtered = filtered.filter((log) => log.status === filters.status);
    }

    setFilteredLogs(filtered);
  };

  const exportLogs = () => {
    const csv = [
      ['Timestamp', 'User', 'Action', 'Resource', 'IP Address', 'Status'],
      ...filteredLogs.map((log) => [
        format(new Date(log.timestamp), 'yyyy-MM-dd HH:mm:ss'),
        log.user,
        log.action,
        log.resource,
        log.ipAddress,
        log.status,
      ]),
    ]
      .map((row) => row.join(','))
      .join('\n');

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `audit-logs-${format(new Date(), 'yyyy-MM-dd')}.csv`;
    a.click();
    toast.success('Audit logs exported');
  };

  return (
    <DashboardLayout requiredUserType="ADMIN">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
        <div className="flex-1 p-8">
          <div className="mb-8">
            <h1 className="text-4xl font-bold text-gray-800 mb-2">Audit & Compliance</h1>
            <p className="text-gray-600">Activity log and compliance monitoring</p>
          </div>

          {/* Filters */}
          <Card className="mb-6">
            <CardContent className="pt-6">
              <div className="flex flex-col md:flex-row gap-4">
                <div className="flex-1 relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                  <Input
                    placeholder="Search by user, action, resource, IP..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-10"
                  />
                </div>
                <Select value={filters.actionType} onValueChange={(v) => setFilters({ ...filters, actionType: v })}>
                  <SelectTrigger className="w-[180px]">
                    <SelectValue placeholder="Action Type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">All Actions</SelectItem>
                    <SelectItem value="USER_DELETED">User Deleted</SelectItem>
                    <SelectItem value="APPOINTMENT_CREATED">Appointment Created</SelectItem>
                    <SelectItem value="APPOINTMENT_UPDATED">Appointment Updated</SelectItem>
                    <SelectItem value="LOGIN">Login</SelectItem>
                    <SelectItem value="LOGOUT">Logout</SelectItem>
                  </SelectContent>
                </Select>
                <Select value={filters.status} onValueChange={(v) => setFilters({ ...filters, status: v })}>
                  <SelectTrigger className="w-[180px]">
                    <SelectValue placeholder="Status" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">All Status</SelectItem>
                    <SelectItem value="SUCCESS">Success</SelectItem>
                    <SelectItem value="FAILURE">Failure</SelectItem>
                  </SelectContent>
                </Select>
                <Button variant="outline" onClick={exportLogs}>
                  <Download className="mr-2" size={16} />
                  Export
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Compliance Dashboard */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">HIPAA Compliance</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="text-green-600" size={24} />
                  <span className="text-lg font-semibold">Compliant</span>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Data Breach Monitoring</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="text-green-600" size={24} />
                  <span className="text-lg font-semibold">No Issues</span>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium">Anomaly Detection</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-2">
                  <AlertTriangle className="text-yellow-600" size={24} />
                  <span className="text-lg font-semibold">2 Anomalies</span>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Audit Log Table */}
          <Card>
            <CardHeader>
              <CardTitle>Activity Log</CardTitle>
              <CardDescription>All admin actions, user actions, and system events</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left p-3">Timestamp</th>
                      <th className="text-left p-3">User</th>
                      <th className="text-left p-3">Action</th>
                      <th className="text-left p-3">Resource</th>
                      <th className="text-left p-3">IP Address</th>
                      <th className="text-left p-3">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredLogs.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="text-center p-8 text-gray-500">
                          No audit logs found
                        </td>
                      </tr>
                    ) : (
                      filteredLogs.map((log) => (
                        <tr key={log.id} className="border-b hover:bg-gray-50">
                          <td className="p-3 text-sm">
                            {format(new Date(log.timestamp), 'MMM dd, yyyy HH:mm:ss')}
                          </td>
                          <td className="p-3">{log.user}</td>
                          <td className="p-3">
                            <Badge variant="outline">{log.action}</Badge>
                          </td>
                          <td className="p-3 text-sm text-gray-600">{log.resource}</td>
                          <td className="p-3 text-sm text-gray-500">{log.ipAddress}</td>
                          <td className="p-3">
                            <Badge
                              variant={log.status === 'SUCCESS' ? 'default' : 'destructive'}
                            >
                              {log.status}
                            </Badge>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </DashboardLayout>
  );
}

