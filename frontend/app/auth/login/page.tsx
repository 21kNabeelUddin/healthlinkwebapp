'use client';

import Link from 'next/link';

const loginOptions = [
  {
    label: 'Patient',
    description: 'Book appointments, manage records, and chat with doctors.',
    href: '/auth/patient/login',
    accent: 'from-teal-500 to-violet-500',
  },
  {
    label: 'Doctor',
    description: 'Manage clinics, appointments, and patient records.',
    href: '/auth/doctor/login',
    accent: 'from-blue-500 to-cyan-500',
  },
  {
    label: 'Admin',
    description: 'Oversee system operations, users, and security.',
    href: '/auth/admin/login',
    accent: 'from-slate-900 to-slate-700',
  },
];

export default function LoginSelectionPage() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white flex items-center justify-center px-4">
      <div className="w-full max-w-4xl bg-white shadow-2xl rounded-3xl border border-slate-100 p-8 md:p-12">
        <div className="text-center mb-10">
          <p className="text-sm uppercase tracking-widest text-teal-600 font-semibold mb-3">
            HealthLink+
          </p>
          <h1 className="text-3xl md:text-4xl font-semibold text-slate-900 mb-3">
            Choose how you want to sign in
          </h1>
          <p className="text-slate-600">
            Select your role to continue. Each experience is tailored to what you need to get done.
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-6">
          {loginOptions.map((option) => (
            <Link
              key={option.label}
              href={option.href}
              className="group rounded-2xl border border-slate-100 bg-slate-50/60 hover:bg-white hover:shadow-lg transition p-6 flex flex-col gap-4"
            >
              <div
                className={`w-12 h-12 rounded-xl bg-gradient-to-br ${option.accent} text-white flex items-center justify-center text-lg font-semibold`}
              >
                {option.label[0]}
              </div>
              <div>
                <h2 className="text-xl font-semibold text-slate-900 mb-2">{option.label} Portal</h2>
                <p className="text-sm text-slate-600">{option.description}</p>
              </div>
              <span className="mt-auto text-sm font-semibold text-teal-600 group-hover:text-violet-600 flex items-center gap-2">
                Continue
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  className="w-4 h-4"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
                </svg>
              </span>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}

