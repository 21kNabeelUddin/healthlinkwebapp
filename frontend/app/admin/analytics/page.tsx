'use client';

import { useState, useEffect } from 'react';
import { adminApi } from '@/lib/api';
import { toast } from 'react-hot-toast';
import { BarChart3, Download, Plus, Trash2, Mail } from 'lucide-react';
import DashboardLayout from '@/components/layout/DashboardLayout';
import { Button } from '@/marketing/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/marketing/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/marketing/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/marketing/ui/tabs';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';

export default function AdminAnalyticsPage() {
  const [dashboardData, setDashboardData] = useState<any>(null);
  const [timeFilter, setTimeFilter] = useState('ALL_TIME');
  const [isLoading, setIsLoading] = useState(true);
  const [scheduledReports, setScheduledReports] = useState([
    { id: '1', name: 'Weekly User Growth', frequency: 'WEEKLY', day: 'Monday', time: '09:00', email: 'admin@healthlink.com', enabled: true },
    { id: '2', name: 'Monthly Revenue Report', frequency: 'MONTHLY', day: '1st', time: '08:00', email: 'admin@healthlink.com', enabled: true },
    { id: '3', name: 'Daily Appointment Summary', frequency: 'DAILY', day: 'Every day', time: '18:00', email: 'admin@healthlink.com', enabled: false },
  ]);
  const [showNewReport, setShowNewReport] = useState(false);
  const [newReport, setNewReport] = useState({
    name: '',
    frequency: 'WEEKLY' as 'DAILY' | 'WEEKLY' | 'MONTHLY',
    day: '',
    time: '09:00',
    email: '',
  });

  useEffect(() => {
    loadAnalytics();
  }, [timeFilter]);

  useEffect(() => {
    ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);
  }, []);

  const loadAnalytics = async () => {
    setIsLoading(true);
    try {
      const data = await adminApi.getDashboard();
      setDashboardData(data);
    } catch (error: any) {
      toast.error('Failed to load analytics');
    } finally {
      setIsLoading(false);
    }
  };

  const exportReport = (format: 'PDF' | 'Excel' | 'CSV') => {
    toast.success(`Exporting report as ${format}...`);
  };

  const makeLineOptions = (title: string) => ({
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'top' as const },
      title: { display: true, text: title },
    },
    scales: {
      y: { beginAtZero: true },
    },
  });

  const userGrowthData = {
    labels: dashboardData?.userGrowth?.labels || ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
    datasets: [
      {
        label: 'New Users',
        data: dashboardData?.userGrowth?.values || [5, 9, 12, 18, 22, 30],
        borderColor: '#0ea5e9',
        backgroundColor: 'rgba(14,165,233,0.2)',
        tension: 0.35,
      },
    ],
  };

  const appointmentTrendData = {
    labels: dashboardData?.appointmentTrends?.labels || ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
    datasets: [
      {
        label: 'Appointments',
        data: dashboardData?.appointmentTrends?.values || [3, 5, 6, 4, 8, 9, 7],
        borderColor: '#22c55e',
        backgroundColor: 'rgba(34,197,94,0.2)',
        tension: 0.35,
      },
    ],
  };

  return (
    <DashboardLayout requiredUserType="ADMIN">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
        <div className="flex-1 p-8">
          <div className="mb-8 flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold text-gray-800 mb-2">Analytics & Reporting</h1>
              <p className="text-gray-600">Platform analytics and insights</p>
            </div>
            <div className="flex gap-2">
              <Select value={timeFilter} onValueChange={setTimeFilter}>
                <SelectTrigger className="w-[180px]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="TODAY">Today</SelectItem>
                  <SelectItem value="WEEK">This Week</SelectItem>
                  <SelectItem value="MONTH">This Month</SelectItem>
                  <SelectItem value="YEAR">This Year</SelectItem>
                  <SelectItem value="ALL_TIME">All Time</SelectItem>
                </SelectContent>
              </Select>
              <Button variant="outline" onClick={() => exportReport('PDF')}>
                <Download className="mr-2" size={16} />
                Export PDF
              </Button>
              <Button variant="outline" onClick={() => exportReport('Excel')}>
                <Download className="mr-2" size={16} />
                Export Excel
              </Button>
            </div>
          </div>

          <Tabs defaultValue="overview" className="space-y-6">
            <TabsList>
              <TabsTrigger value="overview">Overview</TabsTrigger>
              <TabsTrigger value="users">User Growth</TabsTrigger>
              <TabsTrigger value="revenue">Revenue</TabsTrigger>
              <TabsTrigger value="appointments">Appointments</TabsTrigger>
              <TabsTrigger value="doctors">Doctor Performance</TabsTrigger>
              <TabsTrigger value="custom">Custom Report</TabsTrigger>
              <TabsTrigger value="scheduled">Scheduled Reports</TabsTrigger>
            </TabsList>

            <TabsContent value="overview">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-sm font-medium text-gray-600">Total Users</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">{dashboardData?.totalPatients + dashboardData?.totalDoctors || 0}</div>
                    <p className="text-xs text-gray-500 mt-1">+12% from last month</p>
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-sm font-medium text-gray-600">Total Appointments</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">{dashboardData?.totalAppointments || 0}</div>
                    <p className="text-xs text-gray-500 mt-1">+8% from last month</p>
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-sm font-medium text-gray-600">Revenue</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">PKR 0</div>
                    <p className="text-xs text-gray-500 mt-1">+15% from last month</p>
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-sm font-medium text-gray-600">Active Doctors</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">{dashboardData?.totalDoctors || 0}</div>
                    <p className="text-xs text-gray-500 mt-1">+5% from last month</p>
                  </CardContent>
                </Card>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle>User Growth Trend</CardTitle>
                    <CardDescription>Monthly user registration trends</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="h-64">
                      <Line data={userGrowthData} options={makeLineOptions('User Growth')} />
                    </div>
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader>
                    <CardTitle>Appointment Trends</CardTitle>
                    <CardDescription>Appointment booking patterns</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="h-64">
                      <Line data={appointmentTrendData} options={makeLineOptions('Appointments')} />
                    </div>
                  </CardContent>
                </Card>
              </div>
            </TabsContent>

            <TabsContent value="users">
              <Card>
                <CardHeader>
                  <CardTitle>User Growth Report</CardTitle>
                  <CardDescription>Detailed user registration analytics</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="h-96 flex items-center justify-center text-gray-400">
                    User growth chart would go here
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="revenue">
              <Card>
                <CardHeader>
                  <CardTitle>Revenue Analytics</CardTitle>
                  <CardDescription>Revenue trends and breakdown</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="h-96 flex items-center justify-center text-gray-400">
                    Revenue chart would go here
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="appointments">
              <Card>
                <CardHeader>
                  <CardTitle>Appointment Trends</CardTitle>
                  <CardDescription>Appointment booking and completion patterns</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="h-96 flex items-center justify-center text-gray-400">
                    Appointment trends chart would go here
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="doctors">
              <Card>
                <CardHeader>
                  <CardTitle>Doctor Performance</CardTitle>
                  <CardDescription>Doctor ratings, appointment completion rates</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="h-96 flex items-center justify-center text-gray-400">
                    Doctor performance chart would go here
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="custom">
              <Card>
                <CardHeader>
                  <CardTitle>Custom Report Builder</CardTitle>
                  <CardDescription>Create custom reports with drag-and-drop metrics</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="h-96 flex items-center justify-center text-gray-400">
                    Custom report builder interface would go here
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="scheduled">
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle>Scheduled Reports</CardTitle>
                      <CardDescription>Automatically generated and emailed reports</CardDescription>
                    </div>
                    <Button onClick={() => setShowNewReport(true)}>
                      <Plus className="w-4 h-4 mr-2" />
                      New Scheduled Report
                    </Button>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {scheduledReports.map((report) => (
                      <div key={report.id} className="border rounded-lg p-4">
                        <div className="flex items-center justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                              <h3 className="font-semibold">{report.name}</h3>
                              <span className={`px-2 py-1 rounded text-xs ${
                                report.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                              }`}>
                                {report.enabled ? 'Active' : 'Paused'}
                              </span>
                            </div>
                            <div className="text-sm text-gray-600 space-y-1">
                              <p><strong>Frequency:</strong> {report.frequency}</p>
                              <p><strong>Schedule:</strong> {report.day} at {report.time}</p>
                              <p><strong>Email:</strong> {report.email}</p>
                            </div>
                          </div>
                          <div className="flex gap-2">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setScheduledReports(scheduledReports.map(r =>
                                  r.id === report.id ? { ...r, enabled: !r.enabled } : r
                                ));
                                toast.success(`Report ${report.enabled ? 'paused' : 'activated'}`);
                              }}
                            >
                              {report.enabled ? 'Pause' : 'Activate'}
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setScheduledReports(scheduledReports.filter(r => r.id !== report.id));
                                toast.success('Report deleted');
                              }}
                            >
                              <Trash2 className="w-4 h-4" />
                            </Button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>

          {/* New Scheduled Report Modal */}
          {showNewReport && (
            <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
              <Card className="w-full max-w-md">
                <CardHeader>
                  <CardTitle>Create Scheduled Report</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <label className="text-sm font-medium mb-1 block">Report Name</label>
                    <input
                      type="text"
                      value={newReport.name}
                      onChange={(e) => setNewReport({ ...newReport, name: e.target.value })}
                      className="w-full px-3 py-2 border rounded-lg"
                      placeholder="e.g., Weekly User Growth"
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium mb-1 block">Frequency</label>
                    <Select
                      value={newReport.frequency}
                      onValueChange={(v) => setNewReport({ ...newReport, frequency: v as any })}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="DAILY">Daily</SelectItem>
                        <SelectItem value="WEEKLY">Weekly</SelectItem>
                        <SelectItem value="MONTHLY">Monthly</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <label className="text-sm font-medium mb-1 block">Day</label>
                    <input
                      type="text"
                      value={newReport.day}
                      onChange={(e) => setNewReport({ ...newReport, day: e.target.value })}
                      className="w-full px-3 py-2 border rounded-lg"
                      placeholder={newReport.frequency === 'WEEKLY' ? 'e.g., Monday' : newReport.frequency === 'MONTHLY' ? 'e.g., 1st' : 'Every day'}
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium mb-1 block">Time</label>
                    <input
                      type="time"
                      value={newReport.time}
                      onChange={(e) => setNewReport({ ...newReport, time: e.target.value })}
                      className="w-full px-3 py-2 border rounded-lg"
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium mb-1 block">Email</label>
                    <input
                      type="email"
                      value={newReport.email}
                      onChange={(e) => setNewReport({ ...newReport, email: e.target.value })}
                      className="w-full px-3 py-2 border rounded-lg"
                      placeholder="admin@healthlink.com"
                    />
                  </div>
                  <div className="flex gap-2">
                    <Button
                      onClick={() => {
                        if (!newReport.name || !newReport.email) {
                          toast.error('Please fill in all fields');
                          return;
                        }
                        const report = {
                          id: Date.now().toString(),
                          ...newReport,
                          enabled: true,
                        };
                        setScheduledReports([...scheduledReports, report]);
                        setNewReport({ name: '', frequency: 'WEEKLY', day: '', time: '09:00', email: '' });
                        setShowNewReport(false);
                        toast.success('Scheduled report created');
                      }}
                      className="flex-1"
                    >
                      <Mail className="w-4 h-4 mr-2" />
                      Create
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => {
                        setShowNewReport(false);
                        setNewReport({ name: '', frequency: 'WEEKLY', day: '', time: '09:00', email: '' });
                      }}
                    >
                      Cancel
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}

