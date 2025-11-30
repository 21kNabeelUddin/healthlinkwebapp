'use client';

import { useState, useEffect } from 'react';
import { patientApi } from '@/lib/api';
import { Doctor } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import Link from 'next/link';

export default function DoctorsPage() {
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [filteredDoctors, setFilteredDoctors] = useState<Doctor[]>([]);
  const [specialization, setSpecialization] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadDoctors();
  }, []);

  useEffect(() => {
    if (specialization) {
      loadDoctors(specialization);
    } else {
      setFilteredDoctors(doctors);
    }
  }, [specialization, doctors]);

  const loadDoctors = async (specializationFilter?: string) => {
    setIsLoading(true);
    try {
      const response = await patientApi.getDoctors(specializationFilter);
      if (response.success && response.data) {
        setDoctors(response.data);
        setFilteredDoctors(response.data);
      }
    } catch (error: any) {
      toast.error('Failed to load doctors');
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="PATIENT">
        <div className="text-center">Loading...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="mb-8">
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Browse Doctors</h1>
        <p className="text-gray-600">Find and book appointments with qualified doctors</p>
      </div>

      <div className="mb-6">
        <Input
          label="Filter by Specialization"
          placeholder="e.g., Cardiology"
          value={specialization}
          onChange={(e) => setSpecialization(e.target.value)}
        />
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredDoctors.length === 0 ? (
          <div className="col-span-full text-center py-8 text-gray-500">
            No doctors found
          </div>
        ) : (
          filteredDoctors.map((doctor) => (
            <Card key={doctor.id}>
              <div className="space-y-3">
                <div>
                  <h3 className="text-xl font-semibold text-gray-800">
                    {doctor.firstName} {doctor.lastName}
                  </h3>
                  <p className="text-primary-600 font-medium">{doctor.specialization}</p>
                </div>

                <div className="space-y-1 text-sm text-gray-600">
                  <p>License: {doctor.licenseNumber}</p>
                  <p>Experience: {doctor.yearsOfExperience} years</p>
                  <p>Email: {doctor.email}</p>
                  <p>Phone: {doctor.phoneNumber}</p>
                </div>

                <Link href={`/patient/appointments/book?doctorId=${doctor.id}`}>
                  <Button className="w-full">Book Appointment</Button>
                </Link>
              </div>
            </Card>
          ))
        )}
      </div>
    </DashboardLayout>
  );
}

