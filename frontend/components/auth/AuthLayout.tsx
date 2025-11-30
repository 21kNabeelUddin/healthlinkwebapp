'use client';

import { ReactNode } from 'react';
import { Activity, CheckCircle2, Shield, Sparkles } from 'lucide-react';

type Role = 'PATIENT' | 'DOCTOR' | 'ADMIN';

interface AuthLayoutProps {
  role: Role;
  title: string;
  subtitle: string;
  children: ReactNode;
  footer?: ReactNode;
}

const roleCopy: Record<
  Role,
  {
    label: string;
    accent: string;
    highlights: string[];
    statLabel: string;
    statValue: string;
  }
> = {
  PATIENT: {
    label: 'Patient Portal',
    accent: 'from-teal-500 to-violet-600',
    highlights: [
      'Book and join appointments instantly',
      'Secure access to medical history',
      'Real-time updates from your care team',
    ],
    statLabel: 'Verified Patients',
    statValue: '10,482+',
  },
  DOCTOR: {
    label: 'Doctor Workspace',
    accent: 'from-blue-500 to-cyan-500',
    highlights: [
      'Manage clinics & schedules on the go',
      'One-click Zoom consultations',
      'Unified view of patient insights',
    ],
    statLabel: 'Clinics Managed',
    statValue: '148+',
  },
  ADMIN: {
    label: 'Admin Control Center',
    accent: 'from-slate-900 to-slate-700',
    highlights: [
      'System-wide governance & analytics',
      'OTP, security & compliance in one place',
      'Monitor clinics, doctors, and patients',
    ],
    statLabel: 'System Uptime',
    statValue: '99.9%',
  },
};

export function AuthLayout({ role, title, subtitle, children, footer }: AuthLayoutProps) {
  const copy = roleCopy[role];

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50 py-10 px-4">
      <div className="max-w-6xl mx-auto grid md:grid-cols-2 shadow-2xl rounded-3xl border border-slate-100 overflow-hidden bg-white/70 backdrop-blur">
        <div className="hidden md:flex flex-col justify-between p-10 bg-gradient-to-b from-white/70 to-white/20 border-r border-slate-100">
          <div className="space-y-6">
            <div className="inline-flex items-center gap-3 px-4 py-2 rounded-full bg-white shadow">
              <div className={`w-10 h-10 rounded-2xl bg-gradient-to-br ${copy.accent} flex items-center justify-center text-white`}>
                <Activity className="w-5 h-5" />
              </div>
              <span className="text-sm font-medium text-slate-700">{copy.label}</span>
            </div>

            <div className={`rounded-3xl p-6 text-white bg-gradient-to-br ${copy.accent} shadow-lg`}>
              <p className="text-sm uppercase tracking-[0.3em] text-white/80 mb-2">Trusted Access</p>
              <h3 className="text-3xl font-semibold leading-tight mb-4">
                HealthLink+ keeps your data secure and experiences effortless.
              </h3>
              <div className="flex items-center gap-3 text-white/80">
                <Shield className="w-5 h-5" />
                HIPAA-ready & OTP protected
              </div>
            </div>

            <div>
              <p className="text-xs font-semibold text-slate-500 tracking-wider mb-3">WHAT YOU GET</p>
              <ul className="space-y-3">
                {copy.highlights.map((item) => (
                  <li key={item} className="flex items-start gap-3 text-sm text-slate-600">
                    <CheckCircle2 className="w-4 h-4 text-teal-500 mt-0.5" />
                    {item}
                  </li>
                ))}
              </ul>
            </div>
          </div>

          <div className="flex items-center gap-4 bg-white rounded-2xl border border-slate-100 px-5 py-4 shadow-sm">
            <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${copy.accent} flex items-center justify-center`}>
              <Sparkles className="w-5 h-5 text-white" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-widest">{copy.statLabel}</p>
              <p className="text-2xl font-semibold text-slate-900">{copy.statValue}</p>
            </div>
          </div>
        </div>

        <div className="bg-white p-8 md:p-12">
          <div className="mb-8">
            <p className="text-sm font-semibold text-teal-600 uppercase tracking-[0.4em]">{copy.label}</p>
            <h1 className="text-3xl md:text-4xl font-semibold text-slate-900 mt-2">{title}</h1>
            <p className="text-slate-600 mt-2">{subtitle}</p>
          </div>

          <div className="space-y-6">{children}</div>

          {footer && <div className="mt-8 text-center text-sm text-slate-500">{footer}</div>}
        </div>
      </div>
    </div>
  );
}

export default AuthLayout;

