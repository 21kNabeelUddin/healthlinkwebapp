'use client';

import { useAuth } from '@/contexts/AuthContext';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Link from 'next/link';
import { Badge } from '@/marketing/ui/badge';
import { Mail, Phone, ShieldCheck, Stethoscope, Building2, Clock, User as UserIcon } from 'lucide-react';
import { useEffect, useState } from 'react';
import { doctorApi, facilitiesApi } from '@/lib/api';
import { User, Clinic } from '@/types';

export default function DoctorProfilePage() {
  const { user } = useAuth();
  const [doctor, setDoctor] = useState<User | null>(null);
  const [clinicCount, setClinicCount] = useState(0);
  const [activeClinicCount, setActiveClinicCount] = useState(0);

  useEffect(() => {
    const loadDoctor = async () => {
      if (!user?.id) return;
      try {
        // Prefer /users/me for authenticated doctor to get full profile
        const data = await doctorApi.getCurrentProfile();
        let searchData: any = null;

        // If key fields are missing, try public search endpoint as fallback
        const needsFallback =
          !data?.specialization ||
          !data?.pmdcId && !(data as any)?.pmdc_id && !(data as any)?.license_number && !data?.licenseNumber ||
          (!(data as any)?.years_of_experience && data?.yearsOfExperience == null && (data as any)?.experienceYears == null);

        if (needsFallback) {
          try {
            searchData = await doctorApi.getDoctorById(user.id);
          } catch (e) {
            // ignore fallback errors
          }
        }

        const merged = { ...data, ...(searchData || {}) };
        // Normalize fields from backend variants
        const normalized: User = {
          ...user,
          ...merged,
          isVerified:
            (merged as any).isEmailVerified ??
            (merged as any).emailVerified ??
            (merged as any).is_email_verified ??
            merged?.isVerified ??
            user.isVerified ??
            false,
          isActive:
            (merged as any).isActive ??
            (merged as any).active ??
            (merged as any).is_active ??
            merged?.isActive ??
            user.isActive ??
            true,
          approvalStatus:
            (merged as any).approvalStatus ??
            (merged as any).approval_status ??
            merged?.approvalStatus ??
            user.approvalStatus ??
            'PENDING',
          specialization:
            merged?.specialization ??
            (merged as any)?.speciality ??
            (merged as any)?.specializationName ??
            user.specialization,
          licenseNumber:
            merged?.licenseNumber ??
            (merged as any)?.license_number ??
            (merged as any)?.pmdc_id ??
            (merged as any)?.pmdcId ??
            user.licenseNumber,
          pmdcId:
            (merged as any)?.pmdcId ??
            (merged as any)?.pmdc_id ??
            merged?.pmdcId ??
            merged?.licenseNumber ??
            (merged as any)?.license_number ??
            user.pmdcId ??
            user.licenseNumber,
          yearsOfExperience:
            merged?.yearsOfExperience ??
            (merged as any)?.experienceYears ??
            (merged as any)?.years_of_experience ??
            user.yearsOfExperience ??
            user.experienceYears,
          experienceYears:
            (merged as any)?.experienceYears ??
            (merged as any)?.years_of_experience ??
            merged?.yearsOfExperience ??
            user.experienceYears ??
            user.yearsOfExperience,
        };
        setDoctor(normalized);

        // Load clinics for this doctor
        try {
          const clinics: Clinic[] = await facilitiesApi.listForDoctor(user.id);
          const arrayClinics = Array.isArray(clinics) ? clinics : [];
          setClinicCount(arrayClinics.length);
          setActiveClinicCount(arrayClinics.filter((c) => c.active).length);
        } catch (e) {
          setClinicCount(0);
          setActiveClinicCount(0);
        }
      } catch (e) {
        // fall back to auth user
        setDoctor(user);
        setClinicCount(0);
        setActiveClinicCount(0);
      }
    };
    loadDoctor();
  }, [user]);

  const display = doctor || user;

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <p className="text-sm text-slate-500 uppercase tracking-wide">Profile</p>
              <h1 className="text-4xl font-bold text-slate-900">Doctor Profile</h1>
              <p className="text-slate-600">Manage your professional identity and account status</p>
            </div>
            <div className="bg-white border border-slate-200 rounded-xl px-4 py-3 shadow-sm">
              <p className="text-xs uppercase text-slate-500">Account</p>
              <div className="flex items-center gap-2 mt-1">
                <ShieldCheck className={`w-4 h-4 ${display?.isVerified ? 'text-teal-600' : 'text-amber-500'}`} />
                <span className="text-sm font-medium text-slate-900">
                  {display?.isVerified ? 'Verified' : 'Pending verification'}
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Member since {display?.createdAt ? new Date(display.createdAt).toLocaleDateString() : '—'}
              </p>
            </div>
          </div>

          <div className="grid lg:grid-cols-3 gap-6">
            {/* Identity */}
            <Card className="lg:col-span-1 p-5 shadow-sm">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-11 h-11 rounded-full bg-gradient-to-br from-teal-500 to-violet-600 text-white flex items-center justify-center text-lg font-semibold">
                  {display ? (display.firstName?.[0] || 'D') : 'D'}
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-slate-900">
                    {display ? `${display.firstName} ${display.lastName}` : 'Doctor'}
                  </h3>
                  <p className="text-sm text-slate-600 flex items-center gap-1">
                    <UserIcon className="w-4 h-4 text-slate-400" />
                    {display?.userType ?? 'DOCTOR'}
                  </p>
                </div>
              </div>

              <div className="space-y-3 text-sm text-slate-700">
                <div className="flex items-start gap-2">
                  <Mail className="w-4 h-4 text-slate-400 mt-0.5" />
                  <div>
                    <p className="text-xs uppercase text-slate-400">Email</p>
                    <p className="text-slate-900 break-all">{display?.email ?? '—'}</p>
                  </div>
                </div>
                <div className="flex items-start gap-2">
                  <Phone className="w-4 h-4 text-slate-400 mt-0.5" />
                  <div>
                    <p className="text-xs uppercase text-slate-400">Phone</p>
                    <p className="text-slate-900">{display?.phoneNumber ?? '—'}</p>
                  </div>
                </div>
              </div>
            </Card>

            {/* Professional */}
            <Card className="lg:col-span-2 p-6 shadow-sm">
              <div className="flex items-center gap-2 mb-4">
                <Stethoscope className="w-5 h-5 text-teal-600" />
                <h3 className="text-xl font-semibold text-slate-900">Professional</h3>
              </div>
              <div className="grid md:grid-cols-2 gap-4 text-sm text-slate-700">
                <div>
                  <p className="text-xs uppercase text-slate-400">Specialization</p>
                  <p className="text-slate-900">{display?.specialization ?? 'Not set'}</p>
                </div>
                <div>
                  <p className="text-xs uppercase text-slate-400">License / PMDC</p>
                  <p className="text-slate-900">{display?.pmdcId ?? display?.licenseNumber ?? 'Not set'}</p>
                </div>
                <div>
                  <p className="text-xs uppercase text-slate-400">Experience</p>
                  <p className="text-slate-900">
                    {display?.experienceYears || display?.yearsOfExperience
                      ? `${display?.experienceYears ?? display?.yearsOfExperience} years`
                      : 'Not set'}
                  </p>
                </div>
                <div>
                  <p className="text-xs uppercase text-slate-400">Availability</p>
                  <p className="text-slate-900">{display?.availabilityNote ?? 'Not provided'}</p>
                </div>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-200 flex items-center justify-between">
                <div className="text-sm text-slate-600">
                  Profile editing is coming soon. Contact support to update critical info.
                </div>
                <Button disabled className="opacity-60 cursor-not-allowed">
                  Edit Profile (coming soon)
                </Button>
              </div>
            </Card>
          </div>

          {/* Clinics & security */}
          <div className="grid lg:grid-cols-3 gap-6">
            <Card className="p-5 shadow-sm">
              <div className="flex items-center gap-2 mb-3">
                <Building2 className="w-5 h-5 text-teal-600" />
                <h3 className="text-lg font-semibold text-slate-900">Clinics</h3>
              </div>
              <p className="text-sm text-slate-600 mb-3">
                Manage your clinic locations, hours, and services.
              </p>
              <div className="flex items-center gap-2 text-sm text-slate-700">
                <Badge variant="outline" className="bg-slate-100 text-slate-700 border-slate-200">
                  {clinicCount} clinics
                </Badge>
                <Badge variant="outline" className="bg-teal-50 text-teal-700 border-teal-200">
                  {activeClinicCount} active
                </Badge>
              </div>
              <div className="mt-4">
                <Link href="/doctor/clinics">
                  <Button variant="outline" className="w-full">
                    Go to Clinics
                  </Button>
                </Link>
              </div>
            </Card>

            <Card className="p-5 shadow-sm lg:col-span-2">
              <div className="flex items-center gap-2 mb-3">
                <ShieldCheck className="w-5 h-5 text-teal-600" />
                <h3 className="text-lg font-semibold text-slate-900">Security</h3>
              </div>
              <p className="text-sm text-slate-600 mb-4">
                Protect your account with verified email and strong passwords.
              </p>
              <div className="grid md:grid-cols-2 gap-3 text-sm text-slate-700">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-teal-600" />
                  <span>Email verification: {display?.isVerified ? 'Completed' : 'Pending'}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Clock className="w-4 h-4 text-slate-400" />
                  <span>Last updated: {display?.updatedAt ? new Date(display.updatedAt).toLocaleDateString() : '—'}</span>
                </div>
              </div>
              <div className="mt-4">
                <Button disabled className="opacity-60 cursor-not-allowed">
                  Change Password (coming soon)
                </Button>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}


