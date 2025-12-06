'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import Link from 'next/link';
import { patientApi, facilitiesApi } from '@/lib/api';
import { Doctor, Clinic } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import { Button } from '@/marketing/ui/button';
import { Badge } from '@/marketing/ui/badge';
import {
  ArrowLeft,
  Stethoscope,
  MapPin,
  Clock,
  Star,
  Phone,
  Mail,
  Building2,
} from 'lucide-react';

export default function DoctorProfilePage() {
  const router = useRouter();
  const params = useParams();
  const doctorId = params?.doctorId as string;

  const [doctor, setDoctor] = useState<Doctor | null>(null);
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (doctorId) {
      loadDoctor();
      loadClinics();
    }
  }, [doctorId]);

  const loadDoctor = async () => {
    setIsLoading(true);
    try {
      const response = await patientApi.getDoctors();
      let doctorsList: Doctor[] = [];
      if (Array.isArray(response)) {
        doctorsList = response;
      } else if (response && typeof response === 'object' && 'data' in response) {
        const data = (response as any).data;
        if (Array.isArray(data)) {
          doctorsList = data;
        }
      }
      const foundDoctor = doctorsList.find((d) => d.id.toString() === doctorId);
      if (foundDoctor) {
        setDoctor(foundDoctor);
      } else {
        toast.error('Doctor not found');
        router.push('/patient/doctors');
      }
    } catch (error: any) {
      toast.error('Failed to load doctor information');
      router.push('/patient/doctors');
    } finally {
      setIsLoading(false);
    }
  };

  const loadClinics = async () => {
    try {
      const data = await facilitiesApi.listForDoctor(doctorId);
      // Handle both array and ResponseEnvelope formats
      let clinicsList: Clinic[] = [];
      if (Array.isArray(data)) {
        clinicsList = data;
      } else if (data && typeof data === 'object' && 'data' in data) {
        const responseData = (data as any).data;
        if (Array.isArray(responseData)) {
          clinicsList = responseData;
        }
      }
      const activeClinics = clinicsList.filter((c) => c.active);
      setClinics(activeClinics || []);
      
      if (activeClinics.length === 0 && clinicsList.length > 0) {
        console.warn('Doctor has clinics but none are active');
      }
    } catch (error: any) {
      console.error('Error loading clinics:', error);
      toast.error('Failed to load clinics. Please try again.');
      setClinics([]);
    }
  };

  const handleBookAppointment = (clinicId: number) => {
    router.push(`/patient/doctors/${doctorId}/book?clinicId=${clinicId}`);
  };

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="PATIENT">
        <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
          <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="text-center py-20">
              <div className="inline-block animate-spin rounded-full h-12 w-12 border-4 border-teal-500 border-t-transparent"></div>
              <p className="mt-4 text-slate-600">Loading doctor information...</p>
            </div>
          </div>
        </div>
      </DashboardLayout>
    );
  }

  if (!doctor) {
    return null;
  }

  const rating = doctor.averageRating || 0;
  const reviewCount = doctor.totalReviews || 0;
  const consultationFee = doctor.consultationFee || 0;

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* Back Button */}
          <Link
            href="/patient/doctors"
            className="inline-flex items-center gap-2 text-teal-600 hover:text-teal-700 text-sm font-medium mb-6"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Search Doctors
          </Link>

          {/* Doctor Info Card */}
          <Card className="mb-6 bg-gradient-to-br from-teal-50 to-violet-50 border-teal-200">
            <div className="flex flex-col md:flex-row gap-6">
              {/* Avatar */}
              <div className="w-24 h-24 bg-gradient-to-br from-teal-500 to-violet-600 rounded-2xl flex items-center justify-center flex-shrink-0 shadow-md">
                <Stethoscope className="w-12 h-12 text-white" />
              </div>

              {/* Doctor Details */}
              <div className="flex-1">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h1 className="text-3xl font-bold text-slate-900 mb-2">
                      Dr. {doctor.firstName} {doctor.lastName}
                    </h1>
                    <Badge variant="secondary" className="text-sm mb-3">
                      {doctor.specialization}
                    </Badge>
                    {rating > 0 && (
                      <div className="flex items-center gap-2 mb-2">
                        <div className="flex items-center gap-1">
                          <Star className="w-5 h-5 fill-yellow-400 text-yellow-400" />
                          <span className="font-semibold text-slate-900">{rating.toFixed(1)}</span>
                        </div>
                        {reviewCount > 0 && (
                          <span className="text-slate-600 text-sm">({reviewCount} reviews)</span>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                {/* Contact Info */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm text-slate-600">
                  {doctor.phoneNumber && (
                    <div className="flex items-center gap-2">
                      <Phone className="w-4 h-4 text-teal-600" />
                      <span>{doctor.phoneNumber}</span>
                    </div>
                  )}
                  {doctor.email && (
                    <div className="flex items-center gap-2">
                      <Mail className="w-4 h-4 text-teal-600" />
                      <span>{doctor.email}</span>
                    </div>
                  )}
                  {doctor.yearsOfExperience > 0 && (
                    <div className="flex items-center gap-2">
                      <Stethoscope className="w-4 h-4 text-teal-600" />
                      <span>{doctor.yearsOfExperience} years of experience</span>
                    </div>
                  )}
                </div>

                {/* Consultation Fee */}
                {consultationFee > 0 && (
                  <div className="mt-4 bg-gradient-to-br from-blue-500 to-cyan-500 text-white p-4 rounded-lg inline-block">
                    <div className="text-xs opacity-90 mb-1">Consultation Fee</div>
                    <div className="text-2xl font-bold">PKR {consultationFee.toLocaleString()}</div>
                  </div>
                )}
              </div>
            </div>
          </Card>

          {/* Clinics Section */}
          <div className="mb-6">
            <h2 className="text-2xl font-bold text-slate-900 mb-4">Available Clinics</h2>
            {clinics.length === 0 ? (
              <Card>
                <div className="text-center py-8 text-slate-500">
                  <Building2 className="w-12 h-12 text-slate-300 mx-auto mb-3" />
                  <p>No clinics available for this doctor</p>
                </div>
              </Card>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {clinics.map((clinic) => (
                  <Card key={clinic.id} className="hover:shadow-lg transition-all">
                    <div className="space-y-4">
                      {/* Clinic Header */}
                      <div>
                        <h3 className="text-xl font-bold text-slate-900 mb-2">{clinic.name}</h3>
                        <div className="flex items-start gap-2 text-sm text-slate-600 mb-2">
                          <MapPin className="w-4 h-4 mt-0.5 flex-shrink-0 text-red-500" />
                          <div>
                            <div>{clinic.address}</div>
                            <div>{clinic.city}, {clinic.state}</div>
                          </div>
                        </div>
                        <div className="flex items-center gap-2 text-sm text-slate-600">
                          <Clock className="w-4 h-4 text-slate-500" />
                          <span>Opens {clinic.openingTime} • Closes {clinic.closingTime}</span>
                        </div>
                      </div>

                      {/* Clinic Fee */}
                      {clinic.consultationFee && clinic.consultationFee > 0 && (
                        <div className="bg-gradient-to-br from-blue-500 to-cyan-500 text-white p-3 rounded-lg">
                          <div className="text-xs opacity-90 mb-1">Consultation Fee</div>
                          <div className="text-xl font-bold">PKR {clinic.consultationFee.toLocaleString()}</div>
                        </div>
                      )}

                      {/* Book Appointment Button */}
                      <Button
                        onClick={() => handleBookAppointment(clinic.id)}
                        className="w-full bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700 text-white"
                      >
                        Book Appointment
                      </Button>
                    </div>
                  </Card>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}

