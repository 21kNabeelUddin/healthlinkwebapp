import { 
  Calendar, 
  Video, 
  FileText, 
  Building2, 
  BarChart3, 
  ShieldCheck 
} from "lucide-react";

export function FeaturesGrid() {
  const features = [
    {
      icon: Calendar,
      title: "Unified Appointment Calendar",
      description: "Schedule and manage both online and on-site appointments in one place.",
      gradient: "from-blue-500 to-cyan-500"
    },
    {
      icon: Video,
      title: "Zoom Meetings Auto-Created",
      description: "Automatic video consultation links generated for every online appointment.",
      gradient: "from-teal-500 to-emerald-500"
    },
    {
      icon: FileText,
      title: "Medical History Management",
      description: "Complete CRUD operations for patient medical records and history.",
      gradient: "from-violet-500 to-purple-500"
    },
    {
      icon: Building2,
      title: "Clinic Management",
      description: "Doctors can create, update, and manage multiple clinic locations.",
      gradient: "from-pink-500 to-rose-500"
    },
    {
      icon: BarChart3,
      title: "Admin Dashboards & Analytics",
      description: "Comprehensive metrics and insights for platform administrators.",
      gradient: "from-orange-500 to-amber-500"
    },
    {
      icon: ShieldCheck,
      title: "Secure OTP/Email Verification",
      description: "Multi-factor authentication to ensure HIPAA-compliant security.",
      gradient: "from-indigo-500 to-blue-500"
    }
  ];

  return (
    <section className="py-20 lg:py-28">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-16">
          <h2 className="text-3xl sm:text-4xl lg:text-5xl text-slate-900 mb-4">
            Everything You Need For Healthcare Management
          </h2>
          <p className="text-lg text-slate-600 max-w-2xl mx-auto">
            Powerful features designed to streamline workflows across your entire healthcare organization.
          </p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8">
          {features.map((feature, index) => {
            const Icon = feature.icon;
            return (
              <div
                key={index}
                className="group relative bg-white/60 backdrop-blur-sm rounded-2xl p-8 border border-slate-200 hover:border-slate-300 transition-all duration-300 hover:shadow-xl"
              >
                {/* Gradient glow on hover */}
                <div className={`absolute inset-0 bg-gradient-to-br ${feature.gradient} opacity-0 group-hover:opacity-5 rounded-2xl transition-opacity duration-300`}></div>
                
                <div className="relative space-y-4">
                  {/* Icon */}
                  <div className={`w-14 h-14 bg-gradient-to-br ${feature.gradient} rounded-xl flex items-center justify-center shadow-lg transform group-hover:scale-110 transition-transform duration-300`}>
                    <Icon className="w-7 h-7 text-white" />
                  </div>

                  {/* Content */}
                  <div>
                    <h3 className="text-xl text-slate-900 mb-2">{feature.title}</h3>
                    <p className="text-slate-600 leading-relaxed">{feature.description}</p>
                  </div>

                  {/* Decorative accent */}
                  <div className={`h-1 w-12 bg-gradient-to-r ${feature.gradient} rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-300`}></div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
