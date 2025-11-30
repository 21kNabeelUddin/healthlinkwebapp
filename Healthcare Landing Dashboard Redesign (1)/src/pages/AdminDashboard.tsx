import { 
  LayoutDashboard, 
  Users, 
  Stethoscope,
  Shield,
  Building2,
  Calendar,
  Settings,
  AlertTriangle,
  TrendingUp,
  Activity,
  CheckCircle2,
  XCircle
} from "lucide-react";
import { TopNav } from "../components/layout/TopNav";
import { Sidebar } from "../components/layout/Sidebar";
import { StatsCard } from "../components/dashboard/StatsCard";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";

export function AdminDashboard() {
  const sidebarItems = [
    { icon: LayoutDashboard, label: "Dashboard", href: "/admin" },
    { icon: Users, label: "Manage Patients", href: "/admin/patients" },
    { icon: Stethoscope, label: "Manage Doctors", href: "/admin/doctors" },
    { icon: Building2, label: "Manage Clinics", href: "/admin/clinics" },
    { icon: Calendar, label: "Appointments", href: "/admin/appointments" },
    { icon: AlertTriangle, label: "System Alerts", href: "/admin/alerts", badge: 3 },
    { icon: Settings, label: "Settings", href: "/admin/settings" }
  ];

  const recentSignups = [
    {
      name: "Emily Davis",
      role: "Patient",
      email: "emily.davis@email.com",
      date: "Nov 24, 2025",
      verified: true
    },
    {
      name: "Dr. Robert Martinez",
      role: "Doctor",
      email: "r.martinez@healthlink.com",
      date: "Nov 24, 2025",
      verified: true
    },
    {
      name: "Jessica Lee",
      role: "Patient",
      email: "j.lee@email.com",
      date: "Nov 23, 2025",
      verified: false
    },
    {
      name: "Dr. Amanda White",
      role: "Doctor",
      email: "a.white@healthlink.com",
      date: "Nov 23, 2025",
      verified: true
    }
  ];

  const systemAlerts = [
    {
      type: "warning",
      message: "OTP verification failed for 3 users",
      time: "10 mins ago"
    },
    {
      type: "error",
      message: "Zoom integration timeout reported",
      time: "1 hour ago"
    },
    {
      type: "info",
      message: "System maintenance scheduled for Dec 1",
      time: "2 hours ago"
    }
  ];

  const appointmentData = [
    { month: "Jun", count: 320 },
    { month: "Jul", count: 450 },
    { month: "Aug", count: 520 },
    { month: "Sep", count: 480 },
    { month: "Oct", count: 610 },
    { month: "Nov", count: 750 }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav userName="Admin User" userRole="Admin" />
      
      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/admin" />
        
        <main className="flex-1 p-4 sm:p-6 lg:p-8 ml-0 lg:ml-0">
          <div className="max-w-7xl mx-auto space-y-8">
            {/* Header */}
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

            {/* Stats Grid */}
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-6">
              <StatsCard
                icon={Users}
                label="Total Patients"
                value="10,482"
                gradient="from-blue-500 to-cyan-500"
                trend={{ value: 12, isPositive: true }}
              />
              <StatsCard
                icon={Stethoscope}
                label="Total Doctors"
                value="524"
                gradient="from-teal-500 to-emerald-500"
                trend={{ value: 8, isPositive: true }}
              />
              <StatsCard
                icon={Shield}
                label="Admins"
                value="12"
                gradient="from-violet-500 to-purple-500"
              />
              <StatsCard
                icon={Calendar}
                label="Total Appointments"
                value="52,341"
                gradient="from-pink-500 to-rose-500"
                trend={{ value: 15, isPositive: true }}
              />
              <StatsCard
                icon={Building2}
                label="Active Clinics"
                value="148"
                gradient="from-orange-500 to-amber-500"
              />
              <StatsCard
                icon={CheckCircle2}
                label="OTP Success Rate"
                value="98.2%"
                gradient="from-green-500 to-emerald-500"
                trend={{ value: 2, isPositive: true }}
              />
            </div>

            <div className="grid lg:grid-cols-3 gap-8">
              {/* Main Content */}
              <div className="lg:col-span-2 space-y-6">
                {/* Appointment Trend Chart */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Appointment Trends</h2>
                    <div className="flex gap-2">
                      <Button variant="ghost" size="sm">Week</Button>
                      <Button variant="ghost" size="sm">Month</Button>
                      <Button size="sm" className="bg-gradient-to-r from-teal-500 to-violet-600 text-white">
                        6 Months
                      </Button>
                    </div>
                  </div>
                  
                  {/* Simple bar chart visualization */}
                  <div className="flex items-end justify-between gap-4 h-64">
                    {appointmentData.map((data, index) => (
                      <div key={index} className="flex-1 flex flex-col items-center gap-2">
                        <div className="w-full bg-gradient-to-t from-teal-500 to-violet-600 rounded-t-lg relative" 
                             style={{ height: `${(data.count / 800) * 100}%` }}>
                          <span className="absolute -top-6 left-1/2 -translate-x-1/2 text-xs text-slate-700">
                            {data.count}
                          </span>
                        </div>
                        <span className="text-xs text-slate-600">{data.month}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Recent Signups Table */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Recent Signups</h2>
                    <Button variant="ghost" size="sm">View All</Button>
                  </div>
                  
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b border-slate-200">
                          <th className="text-left text-sm text-slate-600 pb-3">Name</th>
                          <th className="text-left text-sm text-slate-600 pb-3">Role</th>
                          <th className="text-left text-sm text-slate-600 pb-3">Email</th>
                          <th className="text-left text-sm text-slate-600 pb-3">Date</th>
                          <th className="text-left text-sm text-slate-600 pb-3">Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {recentSignups.map((user, index) => (
                          <tr key={index} className="border-b border-slate-100">
                            <td className="py-3 text-sm text-slate-900">{user.name}</td>
                            <td className="py-3">
                              <Badge variant={user.role === "Doctor" ? "default" : "secondary"} className="text-xs">
                                {user.role}
                              </Badge>
                            </td>
                            <td className="py-3 text-sm text-slate-600">{user.email}</td>
                            <td className="py-3 text-sm text-slate-600">{user.date}</td>
                            <td className="py-3">
                              {user.verified ? (
                                <div className="flex items-center gap-1 text-green-600">
                                  <CheckCircle2 className="w-4 h-4" />
                                  <span className="text-xs">Verified</span>
                                </div>
                              ) : (
                                <div className="flex items-center gap-1 text-orange-600">
                                  <AlertTriangle className="w-4 h-4" />
                                  <span className="text-xs">Pending</span>
                                </div>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Actions Section */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <h2 className="text-xl text-slate-900 mb-6">Quick Actions</h2>
                  
                  <div className="grid sm:grid-cols-3 gap-4">
                    <Button className="h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-blue-500 to-cyan-500 hover:from-blue-600 hover:to-cyan-600">
                      <Users className="w-8 h-8" />
                      <span>Manage Patients</span>
                    </Button>
                    <Button className="h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-teal-500 to-emerald-500 hover:from-teal-600 hover:to-emerald-600">
                      <Stethoscope className="w-8 h-8" />
                      <span>Manage Doctors</span>
                    </Button>
                    <Button className="h-auto py-6 flex flex-col items-center gap-3 bg-gradient-to-br from-violet-500 to-purple-500 hover:from-violet-600 hover:to-purple-600">
                      <Building2 className="w-8 h-8" />
                      <span>Manage Clinics</span>
                    </Button>
                  </div>
                </div>
              </div>

              {/* Sidebar */}
              <div className="space-y-6">
                {/* System Alerts */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg text-slate-900">System Alerts</h3>
                    <Badge variant="destructive">3</Badge>
                  </div>
                  
                  <div className="space-y-3">
                    {systemAlerts.map((alert, index) => (
                      <div
                        key={index}
                        className={`p-3 rounded-lg border ${
                          alert.type === "error"
                            ? "bg-red-50 border-red-200"
                            : alert.type === "warning"
                            ? "bg-orange-50 border-orange-200"
                            : "bg-blue-50 border-blue-200"
                        }`}
                      >
                        <div className="flex items-start gap-2 mb-1">
                          <AlertTriangle
                            className={`w-4 h-4 flex-shrink-0 mt-0.5 ${
                              alert.type === "error"
                                ? "text-red-600"
                                : alert.type === "warning"
                                ? "text-orange-600"
                                : "text-blue-600"
                            }`}
                          />
                          <p className="text-sm text-slate-900">{alert.message}</p>
                        </div>
                        <p className="text-xs text-slate-500 ml-6">{alert.time}</p>
                      </div>
                    ))}
                  </div>
                  
                  <Button variant="ghost" size="sm" className="w-full mt-4">
                    View All Alerts
                  </Button>
                </div>

                {/* Security & Compliance */}
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
                  
                  <Button variant="outline" size="sm" className="w-full mt-4 border-white/20 text-white hover:bg-white/10">
                    View Audit Logs
                  </Button>
                </div>

                {/* Role Distribution */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <h3 className="text-lg text-slate-900 mb-4">Role Distribution</h3>
                  
                  {/* Simple pie visualization */}
                  <div className="flex items-center justify-center mb-6">
                    <div className="relative w-40 h-40">
                      <svg viewBox="0 0 100 100" className="transform -rotate-90">
                        <circle cx="50" cy="50" r="40" fill="none" stroke="#e2e8f0" strokeWidth="20" />
                        <circle
                          cx="50"
                          cy="50"
                          r="40"
                          fill="none"
                          stroke="#14b8a6"
                          strokeWidth="20"
                          strokeDasharray="188 251"
                          strokeDashoffset="0"
                        />
                        <circle
                          cx="50"
                          cy="50"
                          r="40"
                          fill="none"
                          stroke="#8b5cf6"
                          strokeWidth="20"
                          strokeDasharray="50 251"
                          strokeDashoffset="-188"
                        />
                      </svg>
                      <div className="absolute inset-0 flex items-center justify-center">
                        <div className="text-center">
                          <p className="text-2xl text-slate-900">11,018</p>
                          <p className="text-xs text-slate-600">Total Users</p>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className="w-3 h-3 bg-teal-500 rounded"></div>
                        <span className="text-sm text-slate-700">Patients</span>
                      </div>
                      <span className="text-sm text-slate-900">95.1%</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className="w-3 h-3 bg-violet-500 rounded"></div>
                        <span className="text-sm text-slate-700">Doctors</span>
                      </div>
                      <span className="text-sm text-slate-900">4.8%</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className="w-3 h-3 bg-slate-300 rounded"></div>
                        <span className="text-sm text-slate-700">Admins</span>
                      </div>
                      <span className="text-sm text-slate-900">0.1%</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
