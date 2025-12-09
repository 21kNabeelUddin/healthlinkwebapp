'use client';

import { useState, useEffect } from 'react';
import { adminApi } from '@/lib/api';
import { toast } from 'react-hot-toast';
import { BarChart3, TrendingUp, Users, Calendar, DollarSign, Download, Filter } from 'lucide-react';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { Button } from '@/marketing/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/marketing/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/marketing/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/marketing/ui/tabs';

export default function AdminAnalyticsPage() {
  const [dashboardData, setDashboardData] = useState<any>(null);
  const [timeFilter, setTimeFilter] = useState('ALL_TIME');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadAnalytics();
  }, [timeFilter]);

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

  const sidebarItems = [
    { icon: BarChart3, label: 'Analytics', href: '/admin/analytics' },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav userName="Admin" userRole="Admin" showPortalLinks={false} onLogout={() => {}} />
      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/admin/analytics" />
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
                    <div className="h-64 flex items-center justify-center text-gray-400">
                      Chart visualization would go here
                    </div>
                  </CardContent>
                </Card>
                <Card>
                  <CardHeader>
                    <CardTitle>Appointment Trends</CardTitle>
                    <CardDescription>Appointment booking patterns</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="h-64 flex items-center justify-center text-gray-400">
                      Chart visualization would go here
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
          </Tabs>
        </div>
      </div>
    </div>
  );
}

