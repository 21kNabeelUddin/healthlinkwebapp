import { 
  User, 
  Stethoscope, 
  Shield, 
  Calendar,
  Video,
  FileText,
  Building2,
  Users,
  BarChart3
} from "lucide-react";

export function RoleCards() {
  const roles = [
    {
      icon: User,
      title: "Patients",
      description: "Book online/on-site visits, store medical histories, access Zoom links.",
      features: [
        "Easy appointment booking",
        "Medical history storage",
        "Instant Zoom access",
        "Prescription management"
      ],
      gradient: "from-blue-500 to-cyan-500",
      bgGradient: "from-blue-50 to-cyan-50"
    },
    {
      icon: Stethoscope,
      title: "Doctors",
      description: "Manage clinics, confirm appointments, view patient details.",
      features: [
        "Clinic management",
        "Patient records access",
        "Appointment confirmation",
        "Video consultation tools"
      ],
      gradient: "from-teal-500 to-emerald-500",
      bgGradient: "from-teal-50 to-emerald-50"
    },
    {
      icon: Shield,
      title: "Admins",
      description: "Monitor platform metrics, manage staff, control access.",
      features: [
        "Platform analytics",
        "Staff management",
        "Access control",
        "System monitoring"
      ],
      gradient: "from-violet-500 to-purple-500",
      bgGradient: "from-violet-50 to-purple-50"
    }
  ];

  return (
    <section className="py-20 lg:py-28">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-16">
          <h2 className="text-3xl sm:text-4xl lg:text-5xl text-slate-900 mb-4">
            Built For Every Role In Healthcare
          </h2>
          <p className="text-lg text-slate-600 max-w-2xl mx-auto">
            HealthLink+ provides tailored experiences for patients, doctors, and administrators.
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          {roles.map((role, index) => {
            const Icon = role.icon;
            return (
              <div
                key={index}
                className="group relative bg-white rounded-2xl shadow-lg hover:shadow-2xl transition-all duration-300 overflow-hidden border border-slate-200"
              >
                {/* Gradient overlay on hover */}
                <div className={`absolute inset-0 bg-gradient-to-br ${role.bgGradient} opacity-0 group-hover:opacity-100 transition-opacity duration-300`}></div>
                
                <div className="relative p-8 space-y-6">
                  {/* Icon */}
                  <div className={`w-16 h-16 bg-gradient-to-br ${role.gradient} rounded-2xl flex items-center justify-center shadow-lg`}>
                    <Icon className="w-8 h-8 text-white" />
                  </div>

                  {/* Title & Description */}
                  <div>
                    <h3 className="text-2xl text-slate-900 mb-2">{role.title}</h3>
                    <p className="text-slate-600">{role.description}</p>
                  </div>

                  {/* Features List */}
                  <ul className="space-y-3">
                    {role.features.map((feature, idx) => (
                      <li key={idx} className="flex items-start gap-2">
                        <div className={`w-5 h-5 rounded-full bg-gradient-to-br ${role.gradient} flex items-center justify-center flex-shrink-0 mt-0.5`}>
                          <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                          </svg>
                        </div>
                        <span className="text-sm text-slate-700">{feature}</span>
                      </li>
                    ))}
                  </ul>

                  {/* Mini UI Snippet */}
                  <div className="pt-4 border-t border-slate-200">
                    <div className="bg-slate-50 rounded-lg p-3 space-y-2">
                      <div className="flex items-center gap-2">
                        <div className={`w-2 h-2 rounded-full bg-gradient-to-r ${role.gradient}`}></div>
                        <div className="h-2 bg-slate-200 rounded flex-1"></div>
                      </div>
                      <div className="flex items-center gap-2">
                        <div className={`w-2 h-2 rounded-full bg-gradient-to-r ${role.gradient}`}></div>
                        <div className="h-2 bg-slate-200 rounded w-2/3"></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
