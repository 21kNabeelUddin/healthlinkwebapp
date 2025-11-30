import { 
  Calendar, 
  Video, 
  Users, 
  TrendingUp,
  Clock,
  CheckCircle2
} from "lucide-react";

export function ProductShowcase() {
  return (
    <section className="py-20 lg:py-28 bg-gradient-to-b from-slate-50 to-white relative overflow-hidden">
      {/* Background decoration */}
      <div className="absolute top-0 left-0 w-96 h-96 bg-teal-500/10 rounded-full blur-3xl"></div>
      <div className="absolute bottom-0 right-0 w-96 h-96 bg-violet-500/10 rounded-full blur-3xl"></div>

      <div className="container mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="text-center mb-16">
          <h2 className="text-3xl sm:text-4xl lg:text-5xl text-slate-900 mb-4">
            See HealthLink+ In Action
          </h2>
          <p className="text-lg text-slate-600 max-w-2xl mx-auto">
            Real-time dashboards built for efficiency, security, and ease of use.
          </p>
        </div>

        <div className="max-w-6xl mx-auto">
          {/* Layered showcase */}
          <div className="relative">
            {/* Back panel - Admin Analytics */}
            <div className="absolute top-8 left-0 right-0 transform rotate-1 opacity-50 blur-sm">
              <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 p-6">
                <div className="h-64"></div>
              </div>
            </div>

            {/* Middle panel - Doctor Clinic Management */}
            <div className="absolute top-4 left-4 right-4 transform -rotate-1 opacity-75">
              <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 p-6">
                <div className="h-64"></div>
              </div>
            </div>

            {/* Front panel - Patient Dashboard */}
            <div className="relative z-10 bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden">
              {/* Dashboard header */}
              <div className="bg-gradient-to-r from-slate-900 to-slate-800 px-6 py-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 bg-gradient-to-br from-teal-500 to-violet-600 rounded-lg"></div>
                  <div>
                    <p className="text-white text-sm">Patient Dashboard</p>
                    <p className="text-slate-400 text-xs">John Smith</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-slate-400 hidden sm:inline">Nov 24, 2025</span>
                </div>
              </div>

              {/* Dashboard content */}
              <div className="p-6 space-y-6">
                {/* Stats row */}
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                  <div className="bg-gradient-to-br from-blue-50 to-cyan-50 rounded-xl p-4 border border-blue-200">
                    <div className="flex items-center gap-2 mb-2">
                      <Calendar className="w-4 h-4 text-blue-600" />
                      <span className="text-xs text-blue-900">Appointments</span>
                    </div>
                    <p className="text-2xl text-blue-900">12</p>
                  </div>
                  <div className="bg-gradient-to-br from-teal-50 to-emerald-50 rounded-xl p-4 border border-teal-200">
                    <div className="flex items-center gap-2 mb-2">
                      <Video className="w-4 h-4 text-teal-600" />
                      <span className="text-xs text-teal-900">Video Consults</span>
                    </div>
                    <p className="text-2xl text-teal-900">8</p>
                  </div>
                  <div className="bg-gradient-to-br from-violet-50 to-purple-50 rounded-xl p-4 border border-violet-200">
                    <div className="flex items-center gap-2 mb-2">
                      <Users className="w-4 h-4 text-violet-600" />
                      <span className="text-xs text-violet-900">Doctors</span>
                    </div>
                    <p className="text-2xl text-violet-900">5</p>
                  </div>
                  <div className="bg-gradient-to-br from-pink-50 to-rose-50 rounded-xl p-4 border border-pink-200">
                    <div className="flex items-center gap-2 mb-2">
                      <TrendingUp className="w-4 h-4 text-pink-600" />
                      <span className="text-xs text-pink-900">Records</span>
                    </div>
                    <p className="text-2xl text-pink-900">24</p>
                  </div>
                </div>

                {/* Appointments list */}
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-slate-900">Upcoming Appointments</h3>
                    <button className="text-sm text-teal-600 hover:text-teal-700">View all</button>
                  </div>

                  <div className="space-y-3">
                    {/* Appointment 1 */}
                    <div className="bg-gradient-to-r from-teal-50 to-violet-50 rounded-xl p-4 border border-teal-200">
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                        <div className="flex items-start gap-3">
                          <div className="w-12 h-12 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center flex-shrink-0">
                            <Users className="w-6 h-6 text-white" />
                          </div>
                          <div>
                            <p className="text-slate-900">Dr. Sarah Johnson</p>
                            <p className="text-sm text-slate-600">Cardiology Consultation</p>
                            <div className="flex items-center gap-2 mt-1">
                              <Clock className="w-3 h-3 text-slate-500" />
                              <span className="text-xs text-slate-600">Today, 2:00 PM</span>
                              <span className="text-xs px-2 py-0.5 bg-green-100 text-green-700 rounded">Online</span>
                            </div>
                          </div>
                        </div>
                        <button className="px-4 py-2 bg-gradient-to-r from-teal-500 to-violet-600 text-white rounded-lg hover:from-teal-600 hover:to-violet-700 transition flex items-center gap-2 text-sm">
                          <Video className="w-4 h-4" />
                          <span>Join Zoom</span>
                        </button>
                      </div>
                    </div>

                    {/* Appointment 2 */}
                    <div className="bg-white rounded-xl p-4 border border-slate-200">
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                        <div className="flex items-start gap-3">
                          <div className="w-12 h-12 bg-slate-200 rounded-xl flex items-center justify-center flex-shrink-0">
                            <Users className="w-6 h-6 text-slate-600" />
                          </div>
                          <div>
                            <p className="text-slate-900">Dr. Michael Chen</p>
                            <p className="text-sm text-slate-600">General Checkup</p>
                            <div className="flex items-center gap-2 mt-1">
                              <Clock className="w-3 h-3 text-slate-500" />
                              <span className="text-xs text-slate-600">Tomorrow, 10:00 AM</span>
                              <span className="text-xs px-2 py-0.5 bg-blue-100 text-blue-700 rounded">On-site</span>
                            </div>
                          </div>
                        </div>
                        <button className="px-4 py-2 bg-slate-100 text-slate-700 rounded-lg hover:bg-slate-200 transition flex items-center gap-2 text-sm">
                          <CheckCircle2 className="w-4 h-4" />
                          <span>Confirmed</span>
                        </button>
                      </div>
                    </div>

                    {/* Appointment 3 */}
                    <div className="bg-white rounded-xl p-4 border border-slate-200">
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                        <div className="flex items-start gap-3">
                          <div className="w-12 h-12 bg-slate-200 rounded-xl flex items-center justify-center flex-shrink-0">
                            <Users className="w-6 h-6 text-slate-600" />
                          </div>
                          <div>
                            <p className="text-slate-900">Dr. Emily Davis</p>
                            <p className="text-sm text-slate-600">Dermatology Review</p>
                            <div className="flex items-center gap-2 mt-1">
                              <Clock className="w-3 h-3 text-slate-500" />
                              <span className="text-xs text-slate-600">Dec 1, 3:30 PM</span>
                              <span className="text-xs px-2 py-0.5 bg-green-100 text-green-700 rounded">Online</span>
                            </div>
                          </div>
                        </div>
                        <button className="px-4 py-2 bg-slate-100 text-slate-700 rounded-lg hover:bg-slate-200 transition text-sm">
                          View Details
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
