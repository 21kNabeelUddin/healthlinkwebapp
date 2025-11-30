import { 
  LayoutDashboard, 
  Calendar, 
  FileText, 
  Settings,
  Bell,
  Plus,
  Video,
  Clock,
  CheckCircle2,
  AlertCircle
} from "lucide-react";
import { TopNav } from "../components/layout/TopNav";
import { Sidebar } from "../components/layout/Sidebar";
import { StatsCard } from "../components/dashboard/StatsCard";
import { AppointmentCard } from "../components/dashboard/AppointmentCard";
import { Button } from "../components/ui/button";

export function PatientDashboard() {
  const sidebarItems = [
    { icon: LayoutDashboard, label: "Dashboard", href: "/patient" },
    { icon: Calendar, label: "Appointments", href: "/patient/appointments", badge: 3 },
    { icon: FileText, label: "Medical History", href: "/patient/history" },
    { icon: Bell, label: "Notifications", href: "/patient/notifications", badge: 5 },
    { icon: Settings, label: "Settings", href: "/patient/settings" }
  ];

  const appointments = [
    {
      doctorName: "Dr. Sarah Johnson",
      specialty: "Cardiology Consultation",
      date: "Today",
      time: "2:00 PM",
      type: "online" as const,
      status: "confirmed" as const
    },
    {
      doctorName: "Dr. Michael Chen",
      specialty: "General Checkup",
      date: "Tomorrow",
      time: "10:00 AM",
      type: "onsite" as const,
      status: "confirmed" as const,
      clinicName: "Clinic A"
    },
    {
      doctorName: "Dr. Emily Davis",
      specialty: "Dermatology Review",
      date: "Dec 1",
      time: "3:30 PM",
      type: "online" as const,
      status: "confirmed" as const
    }
  ];

  const medicalHistory = [
    {
      date: "Nov 20, 2025",
      title: "Cardiology Report",
      doctor: "Dr. Sarah Johnson",
      status: "completed"
    },
    {
      date: "Oct 15, 2025",
      title: "Blood Test Results",
      doctor: "Dr. Michael Chen",
      status: "completed"
    },
    {
      date: "Sep 5, 2025",
      title: "Annual Physical",
      doctor: "Dr. Michael Chen",
      status: "completed"
    }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav userName="John Smith" userRole="Patient" />
      
      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/patient" />
        
        <main className="flex-1 p-4 sm:p-6 lg:p-8 ml-0 lg:ml-0">
          <div className="max-w-7xl mx-auto space-y-8">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h1 className="text-3xl text-slate-900 mb-2">Welcome back, John</h1>
                <p className="text-slate-600">Here's your health overview for today</p>
              </div>
              <div className="flex gap-3">
                <Button className="bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700">
                  <Plus className="w-4 h-4 mr-2" />
                  Book Appointment
                </Button>
              </div>
            </div>

            {/* Stats Cards */}
            <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
              <StatsCard
                icon={Calendar}
                label="Upcoming Appointments"
                value="3"
                gradient="from-blue-500 to-cyan-500"
                trend={{ value: 2, isPositive: true }}
              />
              <StatsCard
                icon={Video}
                label="Video Consultations"
                value="12"
                gradient="from-teal-500 to-emerald-500"
              />
              <StatsCard
                icon={FileText}
                label="Medical Records"
                value="24"
                gradient="from-violet-500 to-purple-500"
              />
              <StatsCard
                icon={Clock}
                label="Hours Saved"
                value="48"
                gradient="from-pink-500 to-rose-500"
                trend={{ value: 15, isPositive: true }}
              />
            </div>

            <div className="grid lg:grid-cols-3 gap-8">
              {/* Main Content */}
              <div className="lg:col-span-2 space-y-6">
                {/* Upcoming Appointments */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Upcoming Appointments</h2>
                    <Button variant="ghost" size="sm">View All</Button>
                  </div>
                  
                  <div className="space-y-4">
                    {appointments.map((apt, index) => (
                      <AppointmentCard
                        key={index}
                        {...apt}
                        onJoinZoom={() => console.log("Joining Zoom")}
                      />
                    ))}
                  </div>
                </div>

                {/* Medical History Timeline */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl text-slate-900">Medical History</h2>
                    <Button variant="ghost" size="sm">
                      <Plus className="w-4 h-4 mr-1" />
                      Add Record
                    </Button>
                  </div>
                  
                  <div className="space-y-4">
                    {medicalHistory.map((record, index) => (
                      <div key={index} className="flex items-start gap-4">
                        <div className="w-10 h-10 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center flex-shrink-0">
                          <FileText className="w-5 h-5 text-white" />
                        </div>
                        <div className="flex-1">
                          <div className="flex items-start justify-between mb-1">
                            <div>
                              <p className="text-slate-900">{record.title}</p>
                              <p className="text-sm text-slate-600">{record.doctor}</p>
                            </div>
                            <span className="text-xs text-slate-500">{record.date}</span>
                          </div>
                          <div className="flex items-center gap-2 mt-2">
                            <CheckCircle2 className="w-4 h-4 text-green-600" />
                            <span className="text-xs text-green-600">Completed</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Sidebar */}
              <div className="space-y-6">
                {/* Notifications */}
                <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-6 border border-slate-200 shadow-lg">
                  <h3 className="text-lg text-slate-900 mb-4">Notifications</h3>
                  
                  <div className="space-y-4">
                    <div className="flex items-start gap-3 p-3 bg-green-50 rounded-lg border border-green-200">
                      <CheckCircle2 className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                      <div>
                        <p className="text-sm text-slate-900 mb-1">OTP Verified</p>
                        <p className="text-xs text-slate-600">Your account is now verified</p>
                      </div>
                    </div>

                    <div className="flex items-start gap-3 p-3 bg-blue-50 rounded-lg border border-blue-200">
                      <Bell className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                      <div>
                        <p className="text-sm text-slate-900 mb-1">Appointment Reminder</p>
                        <p className="text-xs text-slate-600">Dr. Sarah Johnson in 2 hours</p>
                      </div>
                    </div>

                    <div className="flex items-start gap-3 p-3 bg-violet-50 rounded-lg border border-violet-200">
                      <FileText className="w-5 h-5 text-violet-600 flex-shrink-0 mt-0.5" />
                      <div>
                        <p className="text-sm text-slate-900 mb-1">New Test Results</p>
                        <p className="text-xs text-slate-600">Blood work from Dr. Chen</p>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Quick Actions */}
                <div className="bg-gradient-to-br from-teal-500 to-violet-600 rounded-2xl p-6 text-white shadow-lg">
                  <h3 className="text-lg mb-4">Quick Actions</h3>
                  
                  <div className="space-y-3">
                    <button className="w-full bg-white/20 backdrop-blur-sm hover:bg-white/30 rounded-lg p-3 text-left transition flex items-center gap-3">
                      <Plus className="w-5 h-5" />
                      <span className="text-sm">Book New Appointment</span>
                    </button>
                    <button className="w-full bg-white/20 backdrop-blur-sm hover:bg-white/30 rounded-lg p-3 text-left transition flex items-center gap-3">
                      <FileText className="w-5 h-5" />
                      <span className="text-sm">Upload Medical Record</span>
                    </button>
                    <button className="w-full bg-white/20 backdrop-blur-sm hover:bg-white/30 rounded-lg p-3 text-left transition flex items-center gap-3">
                      <Calendar className="w-5 h-5" />
                      <span className="text-sm">View All Appointments</span>
                    </button>
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
