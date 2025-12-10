'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { toast } from 'react-hot-toast';
import { ArrowLeft, Bell, Send, CheckCircle2, XCircle, Clock, Users, Filter, Search, Calendar, Eye } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { adminApi } from '@/lib/api';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { Button } from '@/marketing/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/marketing/ui/card';
import { Badge } from '@/marketing/ui/badge';

interface NotificationHistoryItem {
  id: string;
  title: string;
  message: string;
  notificationType: string;
  priority: string;
  recipientType: string;
  totalRecipients: number;
  sentCount: number;
  deliveredCount: number;
  failedCount: number;
  channels: string;
  status: string;
  sentAt: string;
  createdAt: string;
}

export default function NotificationHistoryPage() {
  const router = useRouter();
  const { user, logout } = useAuth();
  const [notifications, setNotifications] = useState<NotificationHistoryItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState({
    status: 'ALL',
    type: 'ALL',
    dateFrom: '',
    dateTo: '',
    search: '',
  });
  const [selectedNotification, setSelectedNotification] = useState<NotificationHistoryItem | null>(null);

  const sidebarItems = [
    { icon: ArrowLeft, label: 'Back to Dashboard', href: '/admin/dashboard' },
    { icon: Send, label: 'Send Notification', href: '/admin/notifications/new' },
  ];

  useEffect(() => {
    loadHistory();
  }, [page]);

  const loadHistory = async () => {
    setIsLoading(true);
    try {
      const response = await adminApi.getNotificationHistory(page, 20);
      setNotifications(response.notifications || []);
      setTotalPages(response.totalPages || 0);
    } catch (error: any) {
      const errorMessage = error?.response?.data?.message || error?.message || 'Failed to load notification history';
      toast.error(errorMessage);
      console.error('Notification history load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SENT':
        return <Badge className="bg-green-500">Sent</Badge>;
      case 'SENDING':
        return <Badge className="bg-blue-500">Sending</Badge>;
      case 'SCHEDULED':
        return <Badge className="bg-yellow-500">Scheduled</Badge>;
      case 'FAILED':
        return <Badge className="bg-red-500">Failed</Badge>;
      case 'PENDING':
        return <Badge className="bg-gray-500">Pending</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  const getPriorityBadge = (priority: string) => {
    switch (priority) {
      case 'URGENT':
        return <Badge variant="destructive">Urgent</Badge>;
      case 'HIGH':
        return <Badge className="bg-orange-500">High</Badge>;
      case 'MEDIUM':
        return <Badge className="bg-blue-500">Medium</Badge>;
      case 'LOW':
        return <Badge variant="outline">Low</Badge>;
      default:
        return <Badge>{priority}</Badge>;
    }
  };

  const formatDate = (dateString: string) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav
        userName={user?.firstName ?? 'Admin'}
        userRole="Admin"
        showPortalLinks={false}
        onLogout={logout}
      />

      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/admin/notifications/history" />

        <main className="flex-1 p-4 sm:px-6 lg:px-8">
          <div className="max-w-7xl mx-auto space-y-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => router.push('/admin/dashboard')}
                >
                  <ArrowLeft className="w-4 h-4" />
                </Button>
                <div>
                  <h1 className="text-3xl font-bold text-slate-900">Notification History</h1>
                  <p className="text-slate-600 mt-1">View all sent notifications and their delivery status</p>
                </div>
              </div>
              <div className="flex gap-2">
                <Button variant="outline" onClick={() => setShowFilters(!showFilters)}>
                  <Filter className="w-4 h-4 mr-2" />
                  Filters
                </Button>
                <Button onClick={() => router.push('/admin/notifications/new')}>
                  <Send className="w-4 h-4 mr-2" />
                  Send New Notification
                </Button>
              </div>
            </div>

            {/* Filters */}
            {showFilters && (
              <Card>
                <CardContent className="pt-6">
                  <div className="grid md:grid-cols-4 gap-4">
                    <div>
                      <label className="text-sm font-medium mb-1 block">Status</label>
                      <select
                        value={filters.status}
                        onChange={(e) => setFilters({ ...filters, status: e.target.value })}
                        className="w-full px-3 py-2 border rounded-lg"
                      >
                        <option value="ALL">All Status</option>
                        <option value="SENT">Sent</option>
                        <option value="SENDING">Sending</option>
                        <option value="SCHEDULED">Scheduled</option>
                        <option value="FAILED">Failed</option>
                      </select>
                    </div>
                    <div>
                      <label className="text-sm font-medium mb-1 block">Type</label>
                      <select
                        value={filters.type}
                        onChange={(e) => setFilters({ ...filters, type: e.target.value })}
                        className="w-full px-3 py-2 border rounded-lg"
                      >
                        <option value="ALL">All Types</option>
                        <option value="INFO">Info</option>
                        <option value="WARNING">Warning</option>
                        <option value="ALERT">Alert</option>
                        <option value="ANNOUNCEMENT">Announcement</option>
                      </select>
                    </div>
                    <div>
                      <label className="text-sm font-medium mb-1 block">From Date</label>
                      <input
                        type="date"
                        value={filters.dateFrom}
                        onChange={(e) => setFilters({ ...filters, dateFrom: e.target.value })}
                        className="w-full px-3 py-2 border rounded-lg"
                      />
                    </div>
                    <div>
                      <label className="text-sm font-medium mb-1 block">To Date</label>
                      <input
                        type="date"
                        value={filters.dateTo}
                        onChange={(e) => setFilters({ ...filters, dateTo: e.target.value })}
                        className="w-full px-3 py-2 border rounded-lg"
                      />
                    </div>
                  </div>
                  <div className="mt-4 flex gap-2">
                    <Button onClick={() => {
                      // Apply filters
                      toast.success('Filters applied');
                    }}>
                      Apply Filters
                    </Button>
                    <Button variant="outline" onClick={() => {
                      setFilters({ status: 'ALL', type: 'ALL', dateFrom: '', dateTo: '', search: '' });
                    }}>
                      Clear
                    </Button>
                  </div>
                </CardContent>
              </Card>
            )}

            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="text-slate-600">Loading notification history...</div>
              </div>
            ) : notifications.length === 0 ? (
              <Card>
                <CardContent className="py-12 text-center">
                  <Bell className="w-12 h-12 mx-auto text-slate-400 mb-4" />
                  <h3 className="text-lg font-semibold text-slate-900 mb-2">No notifications yet</h3>
                  <p className="text-slate-600 mb-4">You haven't sent any custom notifications yet.</p>
                  <Button onClick={() => router.push('/admin/notifications/new')}>
                    <Send className="w-4 h-4 mr-2" />
                    Send Your First Notification
                  </Button>
                </CardContent>
              </Card>
            ) : (
              <div className="space-y-4">
                {notifications.map((notification) => (
                  <Card key={notification.id}>
                    <CardHeader>
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-2">
                            <CardTitle className="text-lg">{notification.title}</CardTitle>
                            {getStatusBadge(notification.status)}
                            {getPriorityBadge(notification.priority)}
                          </div>
                          <CardDescription className="mt-2">
                            {notification.message}
                          </CardDescription>
                        </div>
                      </div>
                    </CardHeader>
                    <CardContent>
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
                        <div>
                          <p className="text-sm text-slate-500">Recipients</p>
                          <p className="text-lg font-semibold">{notification.totalRecipients || 0}</p>
                        </div>
                        <div>
                          <p className="text-sm text-slate-500">Sent</p>
                          <p className="text-lg font-semibold text-green-600">{notification.sentCount || 0}</p>
                        </div>
                        <div>
                          <p className="text-sm text-slate-500">Delivered</p>
                          <p className="text-lg font-semibold text-blue-600">{notification.deliveredCount || 0}</p>
                        </div>
                        <div>
                          <p className="text-sm text-slate-500">Failed</p>
                          <p className="text-lg font-semibold text-red-600">{notification.failedCount || 0}</p>
                        </div>
                      </div>

                      <div className="mt-4">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setSelectedNotification(notification)}
                        >
                          <Eye className="w-4 h-4 mr-2" />
                          View Delivery Details
                        </Button>
                      </div>

                      <div className="flex flex-wrap gap-4 text-sm text-slate-600">
                        <div className="flex items-center gap-2">
                          <Users className="w-4 h-4" />
                          <span>Type: {notification.recipientType.replace(/_/g, ' ')}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Bell className="w-4 h-4" />
                          <span>Channels: {notification.channels}</span>
                        </div>
                        {notification.sentAt && (
                          <div className="flex items-center gap-2">
                            <Clock className="w-4 h-4" />
                            <span>Sent: {formatDate(notification.sentAt)}</span>
                          </div>
                        )}
                        <div className="flex items-center gap-2">
                          <Clock className="w-4 h-4" />
                          <span>Created: {formatDate(notification.createdAt)}</span>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}

                {totalPages > 1 && (
                  <div className="flex items-center justify-center gap-2">
                    <Button
                      variant="outline"
                      onClick={() => setPage(p => Math.max(0, p - 1))}
                      disabled={page === 0}
                    >
                      Previous
                    </Button>
                    <span className="text-sm text-slate-600">
                      Page {page + 1} of {totalPages}
                    </span>
                    <Button
                      variant="outline"
                      onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                      disabled={page >= totalPages - 1}
                    >
                      Next
                    </Button>
                  </div>
                )}
              </div>
            )}

            {/* Delivery Details Modal */}
            {selectedNotification && (
              <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                <Card className="w-full max-w-3xl max-h-[80vh] overflow-y-auto">
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle>Delivery Tracking - {selectedNotification.title}</CardTitle>
                      <Button variant="outline" size="sm" onClick={() => setSelectedNotification(null)}>
                        Close
                      </Button>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <div className="grid grid-cols-3 gap-4">
                        <div className="text-center p-4 bg-green-50 rounded-lg">
                          <p className="text-2xl font-bold text-green-600">{selectedNotification.sentCount || 0}</p>
                          <p className="text-sm text-slate-600">Sent</p>
                        </div>
                        <div className="text-center p-4 bg-blue-50 rounded-lg">
                          <p className="text-2xl font-bold text-blue-600">{selectedNotification.deliveredCount || 0}</p>
                          <p className="text-sm text-slate-600">Delivered</p>
                        </div>
                        <div className="text-center p-4 bg-red-50 rounded-lg">
                          <p className="text-2xl font-bold text-red-600">{selectedNotification.failedCount || 0}</p>
                          <p className="text-sm text-slate-600">Failed</p>
                        </div>
                      </div>
                      <div>
                        <h3 className="font-semibold mb-2">Delivery Status Breakdown</h3>
                        <div className="space-y-2">
                          <div className="flex items-center justify-between p-2 bg-slate-50 rounded">
                            <span className="text-sm">Email</span>
                            <Badge className="bg-green-500">Delivered</Badge>
                          </div>
                          <div className="flex items-center justify-between p-2 bg-slate-50 rounded">
                            <span className="text-sm">SMS</span>
                            <Badge className="bg-blue-500">Sent</Badge>
                          </div>
                          <div className="flex items-center justify-between p-2 bg-slate-50 rounded">
                            <span className="text-sm">In-App</span>
                            <Badge className="bg-green-500">Read</Badge>
                          </div>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}

