'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { prescriptionsApi } from '@/lib/api';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { format } from 'date-fns';
import { FileText, Calendar, User, Pill } from 'lucide-react';

interface Prescription {
  id: string;
  patientId: string;
  doctorId: string;
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

export default function PrescriptionsPage() {
  const { user } = useAuth();
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (user?.id) {
      loadPrescriptions();
    }
  }, [user?.id]);

  const loadPrescriptions = async () => {
    if (!user?.id) return;

    setIsLoading(true);
    try {
      const data = await prescriptionsApi.listForPatient(user.id.toString());
      // Ensure we always have an array, even if API returns unexpected format
      setPrescriptions(Array.isArray(data) ? data : []);
    } catch (error: any) {
      toast.error('Failed to load prescriptions');
      console.error('Prescriptions load error:', error);
      // Prevent follow-up issues by resetting to empty
      setPrescriptions([]);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="PATIENT">
        <div className="text-center py-8">Loading prescriptions...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">My Prescriptions</h1>
            <p className="text-slate-600 mt-1">View all your prescriptions and medications</p>
          </div>
        </div>

        {prescriptions.length === 0 ? (
          <Card className="p-8 text-center">
            <FileText className="w-16 h-16 mx-auto text-slate-400 mb-4" />
            <h3 className="text-lg font-semibold text-slate-700 mb-2">No prescriptions yet</h3>
            <p className="text-slate-500">Your prescriptions will appear here after appointments</p>
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
                        {format(new Date(prescription.createdAt), 'MMM dd, yyyy')}
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
                          className={`text-xs px-2 py-1 rounded ${
                            warning.severity === 'HIGH' || warning.severity === 'CRITICAL'
                              ? 'bg-red-100 text-red-800'
                              : 'bg-yellow-100 text-yellow-800'
                          }`}
                        >
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
              </Card>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}

