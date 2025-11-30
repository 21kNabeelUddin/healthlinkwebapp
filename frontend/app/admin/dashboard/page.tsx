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
} from 'lucide-react';

import { useAuth } from '@/contexts/AuthContext';
import { adminApi } from '@/lib/api';
import { AdminDashboard as AdminDashboardData } from '@/types';

import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
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
      setDashboardData(data);
    } catch (error: any) {
      toast.error('Failed to load dashboard data');
      console.error('Dashboard load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const sidebarItems = [
    { icon: LayoutDashboard, label: 'Dashboard', href: '/admin/dashboard' },
    { icon: Users, label: 'Manage Patients', href: '/admin/patients' },
    { icon: Stethoscope, label: 'Manage Doctors', href: '/admin/doctors' },
    { icon: Building2, label: 'Manage Clinics', href: '/admin/clinics' },
    { icon: Calendar, label: 'Appointments', href: '/admin/appointments' },
    { icon: AlertTriangle, label: 'System Alerts', href: '/admin/dashboard#alerts' },
    { icon: Settings, label: 'Settings', href: '/admin/profile' },
  ];

  const stats = useMemo(() => {
    if (!dashboardData) return [];
    return [
      { icon: Users, label: 'Total Patients', value: dashboardData.totalPatients },
      { icon: Stethoscope, label: 'Total Doctors', value: dashboardData.totalDoctors },
      { icon: Shield, label: 'Admins', value: dashboardData.totalAdmins },
      { icon: Calendar, label: 'Appointments', value: dashboardData.totalAppointments },
      { icon: Building2, label: 'Clinics', value: dashboardData.totalClinics },
      { icon: CheckCircle2, label: 'Completed', value: dashboardData.completedAppointments },
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
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav
        userName={user?.firstName ?? 'Admin'}
        userRole="Admin"
        showPortalLinks={false}
        onLogout={handleLogout}
      />

      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/admin/dashboard" />

        <main className="flex-1 p-4 sm:px-6 lg:px-8">
          <div className="max-w-7xl mx-auto space-y-8">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h1 className="text-3xl text-slate-900 mb-2">Platform Overview</h1>
                <p className="text-slate-600">Monitor and manage HealthLink+ operations</p>
              </div>
              <div className="flex gap-3">
                <Button variant="outline">
                  <Activity className="w-4 h-4 mr-2" />
                  Generate Report
                </Button>
              </div>
            </div>

            <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-6">
              {stats.map((stat) => (
                <StatsCard
                  key={stat.label}
                  icon={stat.icon}
                  label={stat.label}
                  value={stat.value.toString()}
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
                  <div className="grid sm:grid-cols-3 gap-4">
                    <Link href="/admin/patients">
                      <Button className="h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600">
                        <Users className="w-8 h-8" />
                        <span>Manage Patients</span>
                      </Button>
                    </Link>
                    <Link href="/admin/doctors">
                      <Button className="h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-teal-500 to-emerald-500 hover:from-teal-600 hover:to-emerald-600">
                        <Stethoscope className="w-8 h-8" />
                        <span>Manage Doctors</span>
                      </Button>
                    </Link>
                    <Link href="/admin/clinics">
                      <Button className="h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-violet-500 to-purple-500 hover:from-violet-600 hover:to-purple-600">
                        <Building2 className="w-8 h-8" />
                        <span>Manage Clinics</span>
                      </Button>
                    </Link>
                  </div>
                </div>
              </div>

              <div className="space-y-6" id="alerts">
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg text-slate-900">System Alerts</h3>
                    <Badge variant="destructive">Live</Badge>
                  </div>
                  <p className="text-sm text-slate-500">
                    Integrate real alerts once backend endpoints are available.
                  </p>
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
    </div>
  );
}

