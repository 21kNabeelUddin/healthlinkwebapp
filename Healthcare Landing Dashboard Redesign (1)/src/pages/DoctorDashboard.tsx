import { 
  LayoutDashboard, 
  Calendar, 
  Users, 
  Building2,
  Settings,
  Clock,
  Video,
  CheckCircle2,
  XCircle,
  ClipboardList,
  TrendingUp
} from "lucide-react";
import { TopNav } from "../components/layout/TopNav";
import { Sidebar } from "../components/layout/Sidebar";
import { StatsCard } from "../components/dashboard/StatsCard";
import { AppointmentCard } from "../components/dashboard/AppointmentCard";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";

export function DoctorDashboard() {
  const sidebarItems = [
    { icon: LayoutDashboard, label: "Dashboard", href: "/doctor" },
    { icon: Calendar, label: "Appointments", href: "/doctor/appointments", badge: 8 },
    { icon: Users, label: "Patients", href: "/doctor/patients" },
    { icon: Building2, label: "Clinics", href: "/doctor/clinics" },
    { icon: ClipboardList, label: "Tasks", href: "/doctor/tasks", badge: 4 },
    { icon: Settings, label: "Settings", href: "/doctor/settings" }
  ];

  const appointments = [
    {
      patientName: "Emma Rodriguez",
      specialty: "Follow-up Consultation",
      date: "Today",
      time: "2:00 PM",
      type: "online" as const,
      status: "pending" as const
    },
    {
      patientName: "Michael Thompson",
      specialty: "Initial Consultation",
      date: "Today",
      time: "3:30 PM",
      type: "onsite" as const,
      status: "pending" as const,
      clinicName: "Downtown Clinic"
    },
    {
      patientName: "Sarah Wilson",
      specialty: "Check-up",
      date: "Today",
      time: "4:45 PM",
      type: "online" as const,
      status: "confirmed" as const
    }
  ];

  const patients = [
    {
      name: "Emma Rodriguez",
      lastVisit: "Nov 20, 2025",
      condition: "Hypertension",
      status: "stable"
    },
    {
      name: "Michael Thompson",
      lastVisit: "Nov 18, 2025",
      condition: "Diabetes Type 2",
      status: "monitoring"
    },
    {
      name: "Sarah Wilson",
      lastVisit: "Nov 15, 2025",
      condition: "Asthma",
      status: "stable"
    }
  ];

  const clinics = [
    {
      name: "Downtown Clinic",
      address: "123 Main St, San Francisco",
      status: "active",
      appointments: 12
    },
    {
      name: "Westside Medical Center",
      address: "456 Oak Ave, San Francisco",
      status: "active",
      appointments: 8
    }
  ];

  const tasks = [
    { title: "Complete follow-up notes for Emma Rodriguez", priority: "high" },
    { title: "Review lab results for Michael Thompson", priority: "high" },
    { title: "Update prescription for Sarah Wilson", priority: "medium" },
    { title: "Prepare monthly report", priority: "low" }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav userName="Dr. Sarah Johnson" userRole="Doctor" />
      
      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/doctor" />
        
        <main className="flex-1 p-4 sm:p-6 lg:p-8 ml-0 lg:ml-0">
          <div className="max-w-7xl mx-auto space-y-8">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h1 className="text-3xl text-slate-900 mb-2">Good afternoon, Dr. Johnson</h1>
                <p className="text-slate-600">You have 8 appointments today</p>
              </div>
              <div className="flex gap-3">
                <Button variant="outline">
                  <Calendar className="w-4 h-4 mr-2" />
                  View Calendar
                </Button>
              </div>
            </div>

            {/* Stats Cards */}
            <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
              <StatsCard
                icon={Calendar}
                label="Today's Appointments"
                value="8"
                gradient="from-blue-500 to-cyan-500"
              />
              <StatsCard
                icon={Clock}
                label="Pending Confirmations"
                value="3"
                gradient="from-orange-500 to-amber-500"
                trend={{ value: 2, isPositive: false }}
              />
              <StatsCard
                icon={Building2}
                label="Active Clinics"
                value="2"
                gradient="from-teal-500 to-emerald-500"
              />
              <StatsCard
                icon={Video}
                label="Video Consults Today"
                value="5"
                gradient="from-violet-500 to-purple-500"
                trend={{ value: 10, isPositive: true }}
              />
            </div>

            <div className="grid lg:grid-cols-3 gap-8">
              {/* Main Content */}
              <div className="lg:col-span-2 space-y-6">
                {/* Appointment Queue */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Appointment Queue</h2>
                    <Button variant="ghost" size="sm">View All</Button>
                  </div>
                  
                  <div className="space-y-4">
                    {appointments.map((apt, index) => (
                      <AppointmentCard
                        key={index}
                        {...apt}
                        showActions={apt.status === "pending"}
                        onConfirm={() => console.log("Confirmed")}
                        onReject={() => console.log("Rejected")}
                        onJoinZoom={() => console.log("Joining Zoom")}
                      />
                    ))}
                  </div>
                </div>

                {/* Clinic Management */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Clinic Management</h2>
                    <Button variant="ghost" size="sm">Manage</Button>
                  </div>
                  
                  <div className="space-y-4">
                    {clinics.map((clinic, index) => (
                      <div key={index} className="flex items-start justify-between p-4 bg-white rounded-xl border border-slate-200">
                        <div className="flex items-start gap-3">
                          <div className="w-12 h-12 bg-gradient-to-br from-teal-500 to-emerald-500 rounded-xl flex items-center justify-center">
                            <Building2 className="w-6 h-6 text-white" />
                          </div>
                          <div>
                            <div className="flex items-center gap-2 mb-1">
                              <p className="text-slate-900">{clinic.name}</p>
                              <Badge variant="default" className="text-xs">
                                {clinic.status}
                              </Badge>
                            </div>
                            <p className="text-sm text-slate-600 mb-2">{clinic.address}</p>
                            <p className="text-xs text-slate-500">{clinic.appointments} appointments this week</p>
                          </div>
                        </div>
                        <Button size="sm" variant="outline">
                          Edit
                        </Button>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Patient List */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Recent Patients</h2>
                    <Button variant="ghost" size="sm">View All</Button>
                  </div>
                  
                  <div className="space-y-3">
                    {patients.map((patient, index) => (
                      <div key={index} className="flex items-center justify-between p-4 bg-white rounded-xl border border-slate-200 hover:shadow-md transition">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 bg-gradient-to-br from-violet-500 to-purple-500 rounded-full flex items-center justify-center">
                            <span className="text-white text-sm">{patient.name[0]}</span>
                          </div>
                          <div>
                            <p className="text-slate-900 text-sm">{patient.name}</p>
                            <p className="text-xs text-slate-600">{patient.condition}</p>
                          </div>
                        </div>
                        <div className="text-right">
                          <Badge 
                            variant={patient.status === "stable" ? "default" : "secondary"}
                            className="text-xs mb-1"
                          >
                            {patient.status}
                          </Badge>
                          <p className="text-xs text-slate-500">{patient.lastVisit}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Sidebar */}
              <div className="space-y-6">
                {/* Task List */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <h3 className="text-lg text-slate-900 mb-4">Today's Tasks</h3>
                  
                  <div className="space-y-3">
                    {tasks.map((task, index) => (
                      <div key={index} className="flex items-start gap-3">
                        <input 
                          type="checkbox" 
                          className="mt-1 w-4 h-4 text-teal-600 rounded"
                        />
                        <div className="flex-1">
                          <p className="text-sm text-slate-900 mb-1">{task.title}</p>
                          <Badge 
                            variant={
                              task.priority === "high" ? "destructive" :
                              task.priority === "medium" ? "secondary" : "outline"
                            }
                            className="text-xs"
                          >
                            {task.priority}
                          </Badge>
                        </div>
                      </div>
                    ))}
                  </div>
                  
                  <Button variant="ghost" size="sm" className="w-full mt-4">
                    View All Tasks
                  </Button>
                </div>

                {/* Performance */}
                <div className="bg-gradient-to-br from-teal-500 to-violet-600 rounded-2xl p-6 text-white shadow-lg">
                  <div className="flex items-center gap-2 mb-4">
                    <TrendingUp className="w-5 h-5" />
                    <h3 className="text-lg">This Month</h3>
                  </div>
                  
                  <div className="space-y-4">
                    <div>
                      <p className="text-white/80 text-sm mb-1">Patients Seen</p>
                      <p className="text-3xl">142</p>
                    </div>
                    <div>
                      <p className="text-white/80 text-sm mb-1">Avg. Rating</p>
                      <div className="flex items-center gap-2">
                        <p className="text-3xl">4.9</p>
                        <span className="text-sm">⭐</span>
                      </div>
                    </div>
                    <div>
                      <p className="text-white/80 text-sm mb-1">Completion Rate</p>
                      <p className="text-3xl">98%</p>
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
