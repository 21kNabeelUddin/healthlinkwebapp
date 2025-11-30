'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { prescriptionsApi, appointmentsApi } from '@/lib/api';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { format } from 'date-fns';
import { FileText, Plus, Pill, AlertTriangle, Calendar } from 'lucide-react';
import Link from 'next/link';

interface Prescription {
  id: string;
  patientId: string;
  appointmentId?: string;
  medications: Array<{
    id: string;
    name: string;
    dosage: string;
    frequency: string;
    duration: string;
    instructions?: string;
  }>;
  instructions?: string;
  validUntil: string;
  createdAt: string;
  warnings?: Array<{
    type: string;
    severity: string;
    message: string;
  }>;
}

export default function DoctorPrescriptionsPage() {
  const { user } = useAuth();
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadPrescriptions();
  }, []);

  const loadPrescriptions = async () => {
    setIsLoading(true);
    try {
      // Get all appointments first, then get prescriptions for each
      const appointments = await appointmentsApi.list();
      const allPrescriptions: Prescription[] = [];
      
      for (const apt of appointments) {
        try {
          const patientPrescriptions = await prescriptionsApi.listForPatient(apt.patientId.toString());
          allPrescriptions.push(...patientPrescriptions);
        } catch (error) {
          // Skip if patient has no prescriptions
        }
      }
      
      setPrescriptions(allPrescriptions);
    } catch (error: any) {
      toast.error('Failed to load prescriptions');
      console.error('Prescriptions load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="DOCTOR">
        <div className="text-center py-8">Loading prescriptions...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">Prescriptions</h1>
            <p className="text-slate-600 mt-1">Manage patient prescriptions and medications</p>
          </div>
          <Link href="/doctor/prescriptions/new">
            <Button className="bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700">
              <Plus className="w-4 h-4 mr-2" />
              Create Prescription
            </Button>
          </Link>
        </div>

        {prescriptions.length === 0 ? (
          <Card className="p-8 text-center">
            <FileText className="w-16 h-16 mx-auto text-slate-400 mb-4" />
            <h3 className="text-lg font-semibold text-slate-700 mb-2">No prescriptions yet</h3>
            <p className="text-slate-500 mb-4">Create your first prescription for a patient</p>
            <Link href="/doctor/prescriptions/new">
              <Button>Create Prescription</Button>
            </Link>
          </Card>
        ) : (
          <div className="grid gap-6">
            {prescriptions.map((prescription) => (
              <Card key={prescription.id} className="p-6">
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <div className="flex items-center gap-2 mb-2">
                      <Calendar className="w-5 h-5 text-slate-500" />
                      <span className="text-sm text-slate-600">
                        Created: {format(new Date(prescription.createdAt), 'MMM dd, yyyy')}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-slate-500">Valid until:</span>
                      <span className="text-sm font-medium text-slate-700">
                        {format(new Date(prescription.validUntil), 'MMM dd, yyyy')}
                      </span>
                    </div>
                  </div>
                  {prescription.warnings && prescription.warnings.length > 0 && (
                    <div className="flex flex-col gap-1">
                      {prescription.warnings.map((warning, idx) => (
                        <span
                          key={idx}
                          className={`text-xs px-2 py-1 rounded flex items-center gap-1 ${
                            warning.severity === 'HIGH' || warning.severity === 'CRITICAL'
                              ? 'bg-red-100 text-red-800'
                              : 'bg-yellow-100 text-yellow-800'
                          }`}
                        >
                          <AlertTriangle className="w-3 h-3" />
                          {warning.severity}
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                {prescription.instructions && (
                  <div className="mb-4 p-3 bg-blue-50 rounded-lg">
                    <p className="text-sm text-slate-700">{prescription.instructions}</p>
                  </div>
                )}

                <div className="space-y-3">
                  <h4 className="font-semibold text-slate-900 flex items-center gap-2">
                    <Pill className="w-5 h-5" />
                    Medications
                  </h4>
                  {prescription.medications.map((med) => (
                    <div key={med.id} className="pl-7 border-l-2 border-teal-200">
                      <div className="flex items-start justify-between">
                        <div>
                          <p className="font-medium text-slate-900">{med.name}</p>
                          <p className="text-sm text-slate-600">
                            {med.dosage} • {med.frequency} • {med.duration}
                          </p>
                          {med.instructions && (
                            <p className="text-xs text-slate-500 mt-1">{med.instructions}</p>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {prescription.warnings && prescription.warnings.length > 0 && (
                  <div className="mt-4 pt-4 border-t border-slate-200">
                    <h5 className="text-sm font-semibold text-slate-900 mb-2 flex items-center gap-2">
                      <AlertTriangle className="w-4 h-4 text-yellow-600" />
                      Warnings
                    </h5>
                    <div className="space-y-1">
                      {prescription.warnings.map((warning, idx) => (
                        <p key={idx} className="text-xs text-slate-600">
                          {warning.message}
                        </p>
                      ))}
                    </div>
                  </div>
                )}
              </Card>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}

