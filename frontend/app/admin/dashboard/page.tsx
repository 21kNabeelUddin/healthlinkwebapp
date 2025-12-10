'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { toast } from 'react-hot-toast';
import {
  LayoutDashboard,
  Users,
  Stethoscope,
  Shield,
  Building2,
  Calendar,
  Settings,
  AlertTriangle,
  Activity,
  CheckCircle2,
  Bell,
  Send,
  Download,
  Server,
  Database,
  Mail,
  CreditCard,
  Video,
  Cpu,
  HardDrive,
  MemoryStick,
} from 'lucide-react';

import { useAuth } from '@/contexts/AuthContext';
import { adminApi } from '@/lib/api';
import { AdminDashboard as AdminDashboardData } from '@/types';

import DashboardLayout from '@/components/layout/DashboardLayout';
import { StatsCard } from '@/marketing/dashboard/StatsCard';
import { Button } from '@/marketing/ui/button';
import { Badge } from '@/marketing/ui/badge';

export default function AdminDashboard() {
  const router = useRouter();
  const { user, logout } = useAuth();
  const [dashboardData, setDashboardData] = useState<AdminDashboardData | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    setIsLoading(true);
    try {
      const data = await adminApi.getDashboard();
      console.log('Dashboard data received:', data);
      // Ensure all fields are numbers
      const normalizedData = {
        totalPatients: Number(data?.totalPatients ?? 0),
        totalDoctors: Number(data?.totalDoctors ?? 0),
        totalAdmins: Number(data?.totalAdmins ?? 0),
        totalAppointments: Number(data?.totalAppointments ?? 0),
        totalClinics: Number(data?.totalClinics ?? 0),
        totalMedicalHistories: Number(data?.totalMedicalHistories ?? 0),
        pendingAppointments: Number(data?.pendingAppointments ?? 0),
        confirmedAppointments: Number(data?.confirmedAppointments ?? 0),
        completedAppointments: Number(data?.completedAppointments ?? 0),
      };
      console.log('Normalized dashboard data:', normalizedData);
      setDashboardData(normalizedData);
    } catch (error: any) {
      const errorMessage = error?.response?.data?.message || error?.message || 'Failed to load dashboard data';
      const status = error?.response?.status;
      
      console.error('Dashboard load error:', error);
      console.error('Error response:', error?.response);
      console.error('Error status:', status);
      console.error('API URL:', process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080');
      
      // Show more detailed error message
      if (status === 500) {
        toast.error('Server error: The backend may not have the admin dashboard endpoint deployed yet. Check backend logs.');
      } else if (status === 401) {
        toast.error('Authentication failed. Please log in again.');
        logout();
        router.replace('/auth/admin/login');
      } else if (status === 403) {
        toast.error('Access denied. You do not have admin permissions.');
      } else {
        toast.error(`Failed to load dashboard: ${errorMessage} (Status: ${status || 'Unknown'})`);
      }
      
      // Set empty dashboard data so the page doesn't stay stuck loading
      setDashboardData({
        totalPatients: 0,
        totalDoctors: 0,
        totalAdmins: 0,
        totalAppointments: 0,
        totalClinics: 0,
        totalMedicalHistories: 0,
        pendingAppointments: 0,
        confirmedAppointments: 0,
        completedAppointments: 0,
      });
    } finally {
      setIsLoading(false);
    }
  };


  const stats = useMemo(() => {
    if (!dashboardData) return [];
    return [
      { icon: Users, label: 'Total Patients', value: dashboardData.totalPatients ?? 0, gradient: 'from-blue-500 to-cyan-500' },
      { icon: Stethoscope, label: 'Total Doctors', value: dashboardData.totalDoctors ?? 0, gradient: 'from-emerald-500 to-teal-500' },
      { icon: Shield, label: 'Admins', value: dashboardData.totalAdmins ?? 0, gradient: 'from-slate-500 to-slate-700' },
      { icon: Calendar, label: 'Appointments', value: dashboardData.totalAppointments ?? 0, gradient: 'from-indigo-500 to-purple-500' },
      { icon: Building2, label: 'Clinics', value: dashboardData.totalClinics ?? 0, gradient: 'from-orange-500 to-amber-500' },
      { icon: CheckCircle2, label: 'Completed', value: dashboardData.completedAppointments ?? 0, gradient: 'from-green-500 to-lime-500' },
    ];
  }, [dashboardData]);

  const handleLogout = () => {
    logout();
    router.replace('/');
  };

  if (isLoading || !dashboardData) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white flex items-center justify-center text-slate-600">
        Loading dashboard...
      </div>
    );
  }

  return (
    <DashboardLayout requiredUserType="ADMIN">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
        <main className="flex-1 p-4 sm:px-6 lg:px-8">
          <div className="max-w-7xl mx-auto space-y-8">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h1 className="text-3xl text-slate-900 mb-2">Platform Overview</h1>
                <p className="text-slate-600">Monitor and manage HealthLink+ operations</p>
              </div>
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={() => {
                    window.print();
                  }}
                >
                  <Download className="w-4 h-4 mr-2" />
                  Print/Export
                </Button>
                <Button
                  variant="outline"
                  onClick={() => {
                    const data = {
                      stats: dashboardData,
                      timestamp: new Date().toISOString(),
                    };
                    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
                    const url = URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = `dashboard-export-${new Date().toISOString().split('T')[0]}.json`;
                    a.click();
                    toast.success('Dashboard exported');
                  }}
                >
                  <Download className="w-4 h-4 mr-2" />
                  Export JSON
                </Button>
              </div>
            </div>

            <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-6">
              {stats.map((stat) => (
                <StatsCard
                  key={stat.label}
                  icon={stat.icon}
                  label={stat.label}
                  value={(stat.value ?? 0).toString()}
                />
              ))}
            </div>

            <div className="grid lg:grid-cols-3 gap-8">
              <div className="lg:col-span-2 space-y-6">
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Appointment Snapshot</h2>
                    <div className="flex gap-2">
                      <Badge variant="secondary">{dashboardData.pendingAppointments} pending</Badge>
                      <Badge variant="default">{dashboardData.confirmedAppointments} confirmed</Badge>
                    </div>
                  </div>
                  <div className="grid sm:grid-cols-3 gap-4">
                    <div className="p-4 rounded-xl border border-slate-200">
                      <p className="text-sm text-slate-500 mb-1">Pending</p>
                      <p className="text-2xl text-orange-500">{dashboardData.pendingAppointments}</p>
                    </div>
                    <div className="p-4 rounded-xl border border-slate-200">
                      <p className="text-sm text-slate-500 mb-1">Confirmed</p>
                      <p className="text-2xl text-green-600">{dashboardData.confirmedAppointments}</p>
                    </div>
                    <div className="p-4 rounded-xl border border-slate-200">
                      <p className="text-sm text-slate-500 mb-1">Completed</p>
                      <p className="text-2xl text-blue-600">{dashboardData.completedAppointments}</p>
                    </div>
                  </div>
                </div>

                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <h2 className="text-xl text-slate-900 mb-6">Quick Actions</h2>
                  <div className="grid sm:grid-cols-2 md:grid-cols-3 gap-2">
                    <Link href="/admin/patients">
                      <Button className="w-full h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600">
                        <Users className="w-8 h-8" />
                        <span>Manage Patients</span>
                      </Button>
                    </Link>
                    <Link href="/admin/doctors">
                      <Button className="w-full h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-teal-500 to-emerald-500 hover:from-teal-600 hover:to-emerald-600">
                        <Stethoscope className="w-8 h-8" />
                        <span>Manage Doctors</span>
                      </Button>
                    </Link>
                    <Link href="/admin/clinics">
                      <Button className="w-full h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-violet-500 to-purple-500 hover:from-violet-600 hover:to-purple-600">
                        <Building2 className="w-8 h-8" />
                        <span>Manage Clinics</span>
                      </Button>
                    </Link>
                    <Link href="/admin/appointments/enhanced">
                      <Button className="w-full h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600">
                        <Calendar className="w-8 h-8" />
                        <span>View Appointments</span>
                      </Button>
                    </Link>
                    <Link href="/admin/notifications/new">
                      <Button className="w-full h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-pink-500 to-rose-500 hover:from-pink-600 hover:to-rose-600">
                        <Bell className="w-8 h-8" />
                        <span>Send Notification</span>
                      </Button>
                    </Link>
                    <Link href="/admin/settings">
                      <Button className="w-full h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-indigo-500 to-blue-500 hover:from-indigo-600 hover:to-blue-600">
                        <Settings className="w-8 h-8" />
                        <span>System Settings</span>
                      </Button>
                    </Link>
                  </div>
                </div>

                {/* Trend Charts */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                    <h3 className="text-lg text-slate-900 mb-4">User Growth Trend</h3>
                    <div className="h-48 flex items-center justify-center text-slate-400 text-sm">
                      Chart visualization would go here
                    </div>
                  </div>
                  <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                    <h3 className="text-lg text-slate-900 mb-4">Appointment Trends</h3>
                    <div className="h-48 flex items-center justify-center text-slate-400 text-sm">
                      Chart visualization would go here
                    </div>
                  </div>
                </div>

                {/* Time-based Filters */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg text-slate-900">Time Filters</h3>
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm">Today</Button>
                      <Button variant="outline" size="sm">This Week</Button>
                      <Button variant="outline" size="sm">This Month</Button>
                      <Button variant="default" size="sm">All Time</Button>
                    </div>
                  </div>
                </div>

                {/* System Status Indicators */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <h3 className="text-lg text-slate-900 mb-4">System Status</h3>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between p-3 rounded-lg bg-slate-50">
                      <div className="flex items-center gap-3">
                        <Server className="w-5 h-5 text-blue-500" />
                        <span className="text-sm font-medium text-slate-700">API Gateway</span>
                      </div>
                      <Badge className="bg-green-100 text-green-800 border-green-200">Operational</Badge>
                    </div>
                    <div className="flex items-center justify-between p-3 rounded-lg bg-slate-50">
                      <div className="flex items-center gap-3">
                        <Database className="w-5 h-5 text-indigo-500" />
                        <span className="text-sm font-medium text-slate-700">Database</span>
                      </div>
                      <Badge className="bg-green-100 text-green-800 border-green-200">Operational</Badge>
                    </div>
                    <div className="flex items-center justify-between p-3 rounded-lg bg-slate-50">
                      <div className="flex items-center gap-3">
                        <Mail className="w-5 h-5 text-purple-500" />
                        <span className="text-sm font-medium text-slate-700">Email Service</span>
                      </div>
                      <Badge className="bg-green-100 text-green-800 border-green-200">Operational</Badge>
                    </div>
                    <div className="flex items-center justify-between p-3 rounded-lg bg-slate-50">
                      <div className="flex items-center gap-3">
                        <CreditCard className="w-5 h-5 text-emerald-500" />
                        <span className="text-sm font-medium text-slate-700">Payment Gateway</span>
                      </div>
                      <Badge className="bg-green-100 text-green-800 border-green-200">Operational</Badge>
                    </div>
                    <div className="flex items-center justify-between p-3 rounded-lg bg-slate-50">
                      <div className="flex items-center gap-3">
                        <Video className="w-5 h-5 text-orange-500" />
                        <span className="text-sm font-medium text-slate-700">Zoom Integration</span>
                      </div>
                      <Badge className="bg-green-100 text-green-800 border-green-200">Operational</Badge>
                    </div>
                  </div>
                </div>

                {/* Resource Utilization */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <h3 className="text-lg text-slate-900 mb-4">Resource Utilization</h3>
                  <div className="space-y-4">
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <Cpu className="w-4 h-4 text-blue-500" />
                          <span className="text-sm font-medium text-slate-700">CPU Usage</span>
                        </div>
                        <span className="text-sm font-semibold text-slate-700">42%</span>
                      </div>
                      <div className="w-full bg-slate-200 rounded-full h-2.5">
                        <div className="bg-blue-500 h-2.5 rounded-full" style={{ width: '42%' }}></div>
                      </div>
                    </div>
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <Activity className="w-4 h-4 text-purple-500" />
                          <span className="text-sm font-medium text-slate-700">Memory Usage</span>
                        </div>
                        <span className="text-sm font-semibold text-slate-700">68%</span>
                      </div>
                      <div className="w-full bg-slate-200 rounded-full h-2.5">
                        <div className="bg-purple-500 h-2.5 rounded-full" style={{ width: '68%' }}></div>
                      </div>
                    </div>
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <HardDrive className="w-4 h-4 text-emerald-500" />
                          <span className="text-sm font-medium text-slate-700">Disk Space</span>
                        </div>
                        <span className="text-sm font-semibold text-slate-700">35%</span>
                      </div>
                      <div className="w-full bg-slate-200 rounded-full h-2.5">
                        <div className="bg-emerald-500 h-2.5 rounded-full" style={{ width: '35%' }}></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="space-y-6" id="alerts">
                {/* Top Banner with Critical Alerts */}
                <div className="bg-gradient-to-r from-red-500 to-orange-500 rounded-2xl p-6 text-white shadow-lg">
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-2">
                      <AlertTriangle className="w-5 h-5" />
                      <h3 className="text-lg font-semibold">Critical Alerts</h3>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant="secondary" className="bg-white/20 text-white">2 Active</Badge>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-white hover:bg-white/20"
                        onClick={() => router.push('/admin/smart-suggestions')}
                      >
                        View All
                      </Button>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <div className="bg-white/10 rounded-lg p-3">
                      <p className="text-sm font-medium">3 doctors need license renewal this month</p>
                    </div>
                    <div className="bg-white/10 rounded-lg p-3">
                      <p className="text-sm font-medium">Appointment cancellation rate increased 15%</p>
                    </div>
                  </div>
                </div>

                {/* Live Activity Feed */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-2">
                      <Activity className="w-5 h-5 text-slate-700" />
                      <h3 className="text-lg text-slate-900">Live Activity Feed</h3>
                    </div>
                    <Badge variant="outline">Live</Badge>
                  </div>
                  <div className="space-y-3 max-h-64 overflow-y-auto">
                    <div className="flex items-start gap-3 p-2 rounded-lg hover:bg-slate-50">
                      <div className="w-2 h-2 bg-green-500 rounded-full mt-2"></div>
                      <div className="flex-1">
                        <p className="text-sm text-slate-700">
                          <span className="font-semibold">New patient registered:</span> John Doe
                        </p>
                        <p className="text-xs text-slate-500">2 minutes ago</p>
                      </div>
                    </div>
                    <div className="flex items-start gap-3 p-2 rounded-lg hover:bg-slate-50">
                      <div className="w-2 h-2 bg-blue-500 rounded-full mt-2"></div>
                      <div className="flex-1">
                        <p className="text-sm text-slate-700">
                          <span className="font-semibold">Appointment booked:</span> Dr. Smith - Patient: Jane
                        </p>
                        <p className="text-xs text-slate-500">5 minutes ago</p>
                      </div>
                    </div>
                    <div className="flex items-start gap-3 p-2 rounded-lg hover:bg-slate-50">
                      <div className="w-2 h-2 bg-purple-500 rounded-full mt-2"></div>
                      <div className="flex-1">
                        <p className="text-sm text-slate-700">
                          <span className="font-semibold">Doctor logged in:</span> Dr. Ahmed
                        </p>
                        <p className="text-xs text-slate-500">10 minutes ago</p>
                      </div>
                    </div>
                  </div>
                </div>


                <div className="bg-gradient-to-br from-slate-900 to-slate-800 rounded-2xl p-6 text-white shadow-lg">
                  <div className="flex items-center gap-2 mb-4">
                    <Shield className="w-5 h-5" />
                    <h3 className="text-lg">Security & Compliance</h3>
                  </div>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between pb-3 border-b border-white/10">
                      <span className="text-sm text-white/80">HIPAA Compliance</span>
                      <CheckCircle2 className="w-5 h-5 text-green-400" />
                    </div>
                    <div className="flex items-center justify-between pb-3 border-b border-white/10">
                      <span className="text-sm text-white/80">Data Encryption</span>
                      <CheckCircle2 className="w-5 h-5 text-green-400" />
                    </div>
                    <div className="flex items-center justify-between pb-3 border-b border-white/10">
                      <span className="text-sm text-white/80">Audit Logs</span>
                      <CheckCircle2 className="w-5 h-5 text-green-400" />
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-white/80">2FA Enabled</span>
                      <CheckCircle2 className="w-5 h-5 text-green-400" />
                    </div>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    className="w-full mt-4 border-white/20 text-white hover:bg-white/10"
                  >
                    View Audit Logs
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </DashboardLayout>
  );
}

