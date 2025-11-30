import { UserPlus, UserCheck, Video, CalendarCheck } from "lucide-react";

export function HowItWorks() {
  const steps = [
    {
      icon: UserPlus,
      title: "Sign up & verify via OTP",
      description: "Create your account with email and phone verification for security."
    },
    {
      icon: UserCheck,
      title: "Choose your role",
      description: "Select Patient, Doctor, or Admin to customize your experience."
    },
    {
      icon: Video,
      title: "Connect Zoom & clinics",
      description: "Integrate video consultation tools and clinic information."
    },
    {
      icon: CalendarCheck,
      title: "Manage appointments & records",
      description: "Start booking, confirming, and tracking healthcare activities."
    }
  ];

  return (
    <section className="py-20 lg:py-28 bg-gradient-to-b from-white to-slate-50">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-16">
          <h2 className="text-3xl sm:text-4xl lg:text-5xl text-slate-900 mb-4">
            How HealthLink+ Works
          </h2>
          <p className="text-lg text-slate-600 max-w-2xl mx-auto">
            Get started in minutes with our simple 4-step onboarding process.
          </p>
        </div>

        <div className="max-w-6xl mx-auto">
          <div className="grid md:grid-cols-4 gap-8 relative">
            {/* Connection lines for desktop */}
            <div className="hidden md:block absolute top-16 left-0 right-0 h-0.5 bg-gradient-to-r from-teal-500 via-violet-500 to-purple-500 opacity-20"></div>

            {steps.map((step, index) => {
              const Icon = step.icon;
              return (
                <div key={index} className="relative">
                  <div className="flex flex-col items-center text-center space-y-4">
                    {/* Step number & icon */}
                    <div className="relative">
                      <div className="w-32 h-32 bg-white rounded-2xl shadow-lg border border-slate-200 flex items-center justify-center relative z-10">
                        <div className="absolute -top-3 -right-3 w-8 h-8 bg-gradient-to-br from-teal-500 to-violet-600 rounded-full flex items-center justify-center shadow-lg">
                          <span className="text-white text-sm">{index + 1}</span>
                        </div>
                        <Icon className="w-12 h-12 text-slate-700" />
                      </div>
                    </div>

                    {/* Content */}
                    <div>
                      <h3 className="text-lg text-slate-900 mb-2">{step.title}</h3>
                      <p className="text-sm text-slate-600">{step.description}</p>
                    </div>
                  </div>

                  {/* Arrow for mobile */}
                  {index < steps.length - 1 && (
                    <div className="md:hidden flex justify-center my-6">
                      <svg className="w-6 h-6 text-teal-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                      </svg>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
}
