'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useSearchParams } from 'next/navigation';
import { prescriptionsApi } from '@/lib/api';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { format } from 'date-fns';
import { FileText, Calendar, User, Pill, AlertCircle, Download, Printer } from 'lucide-react';
import { Badge } from '@/marketing/ui/badge';

type MedicationEntry =
  | {
      id?: string;
      name?: string;
      dosage?: string;
      frequency?: string;
      duration?: string;
      instructions?: string;
    }
  | string;

interface Prescription {
  id: string;
  patientId: string;
  doctorId: string;
  appointmentId?: string;
  doctorName?: string;
  patientName?: string;
  clinicName?: string;
  title?: string;
  body?: string;
  medications: MedicationEntry[];
  instructions?: string; // legacy field
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
  const searchParams = useSearchParams();
  const appointmentId = searchParams.get('appointmentId');
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [highlightedPrescriptionId, setHighlightedPrescriptionId] = useState<string | null>(null);
  const formatDateSafe = (value?: string, pattern = 'MMM dd, yyyy') => {
    if (!value) return 'N/A';
    const d = new Date(value);
    if (isNaN(d.getTime())) return 'N/A';
    return format(d, pattern);
  };

  const renderMedication = (med: MedicationEntry, idx: number) => {
    if (typeof med === 'string') {
      return (
        <div key={idx} className="p-3 border border-slate-200 rounded-lg">
          <p className="text-sm text-slate-800">{med}</p>
        </div>
      );
    }
    const hasDetails = med.dosage || med.frequency || med.duration;
    return (
      <div key={med.id || idx} className="p-3 border border-slate-200 rounded-lg">
        <p className="font-medium text-slate-900">{med.name || 'Medication'}</p>
        {hasDetails && (
          <p className="text-sm text-slate-600">
            {[med.dosage, med.frequency, med.duration].filter(Boolean).join(' • ')}
          </p>
        )}
        {med.instructions && <p className="text-xs text-slate-500 mt-1">{med.instructions}</p>}
      </div>
    );
  };

  useEffect(() => {
    if (user?.id) {
      loadPrescriptions();
    }
  }, [user?.id, appointmentId]);

  const loadPrescriptions = async () => {
    if (!user?.id) return;

    setIsLoading(true);
    try {
      // If appointmentId is provided, try to get prescription for that appointment first
      if (appointmentId) {
        try {
          const appointmentPrescription = await prescriptionsApi.getByAppointmentId(appointmentId);
          if (appointmentPrescription) {
            setHighlightedPrescriptionId(appointmentPrescription.id);
            // Scroll to the prescription after a short delay
            setTimeout(() => {
              const element = document.getElementById(`prescription-${appointmentPrescription.id}`);
              if (element) {
                element.scrollIntoView({ behavior: 'smooth', block: 'center' });
              }
            }, 500);
          }
        } catch (error) {
          // If no prescription found for appointment, continue loading all prescriptions
          console.log('No prescription found for appointment:', appointmentId);
        }
      }

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
        <div className="text-center py-12 text-slate-600">Loading prescriptions...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm text-slate-500 uppercase tracking-wide">Prescriptions</p>
              <h1 className="text-4xl font-bold text-slate-900">My Prescriptions</h1>
              <p className="text-slate-600">View and review your medications and instructions</p>
            </div>
          </div>

          {prescriptions.length === 0 ? (
            <Card className="p-10 text-center">
              <div className="mx-auto mb-3 w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center">
                <FileText className="w-6 h-6 text-slate-500" />
              </div>
              <h3 className="text-xl font-semibold text-slate-900 mb-1">No prescriptions yet</h3>
              <p className="text-slate-600">Your prescriptions will appear here after appointments.</p>
            </Card>
          ) : (
            <div className="grid gap-5">
              {prescriptions.map((prescription) => {
                const isHighlighted = highlightedPrescriptionId === prescription.id;
                return (
                  <div
                    key={prescription.id}
                    id={`prescription-${prescription.id}`}
                    className={`transition-all duration-500 ${isHighlighted ? 'ring-4 ring-teal-500 shadow-xl rounded-xl' : ''}`}
                  >
                    <Card className={`overflow-hidden ${isHighlighted ? 'bg-teal-50' : ''}`}>
                      <div className="bg-gradient-to-r from-teal-50 to-violet-50 border-b border-slate-200 p-4 flex flex-wrap items-center justify-between gap-3">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-lg bg-white border border-slate-200 flex items-center justify-center">
                            <Pill className="w-5 h-5 text-teal-600" />
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-sm text-slate-600">
                                {formatDateSafe(prescription.createdAt)}
                              </span>
                              <Badge variant="outline" className="bg-white text-slate-700 border-slate-200">
                                Valid until {formatDateSafe(prescription.validUntil)}
                              </Badge>
                            </div>
                            <div className="flex flex-wrap items-center gap-3 text-sm text-slate-600 mt-2">
                              <span className="flex items-center gap-2">
                                <User className="w-4 h-4 text-slate-400" />
                                <span>{prescription.doctorName || 'Doctor'}</span>
                              </span>
                              {prescription.clinicName && (
                                <Badge variant="outline" className="bg-white text-slate-700 border-slate-200">
                                  {prescription.clinicName}
                                </Badge>
                              )}
                              {prescription.appointmentId && (
                                <Badge variant="secondary" className="text-xs">
                                  Appt: {prescription.appointmentId.slice(0, 8)}…
                                </Badge>
                              )}
                            </div>
                          </div>
                        </div>
                        <div className="flex gap-2">
                          <Button variant="secondary" className="inline-flex items-center gap-2">
                            <Download className="w-4 h-4" />
                            Download
                          </Button>
                          <Button variant="secondary" className="inline-flex items-center gap-2">
                            <Printer className="w-4 h-4" />
                            Print
                          </Button>
                        </div>
                      </div>

                      <div className="p-5 space-y-4">
                        {prescription.warnings && prescription.warnings.length > 0 && (
                          <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg flex items-start gap-3">
                            <AlertCircle className="w-5 h-5 text-amber-600 mt-0.5" />
                            <div className="space-y-1 text-sm text-amber-800">
                              {prescription.warnings.map((warning, idx) => (
                                <div key={idx} className="flex items-center gap-2">
                                  <Badge
                                    variant="outline"
                                    className={`text-xs ${warning.severity === 'HIGH' || warning.severity === 'CRITICAL'
                                      ? 'bg-red-100 text-red-700 border-red-200'
                                      : 'bg-amber-100 text-amber-700 border-amber-200'
                                    }`}
                                  >
                                    {warning.severity}
                                  </Badge>
                                  <span>{warning.message}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}

                        {(prescription.instructions || prescription.body) && (
                          <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-700">
                            {prescription.instructions || prescription.body}
                          </div>
                        )}

                        <div className="space-y-3">
                          <h4 className="font-semibold text-slate-900 flex items-center gap-2">
                            <FileText className="w-4 h-4" />
                            Medications
                          </h4>
                          {prescription.medications && prescription.medications.length > 0 ? (
                            prescription.medications.map((med, idx) => renderMedication(med, idx))
                          ) : (
                            <p className="text-sm text-slate-600">No medications listed.</p>
                          )}
                        </div>
                      </div>
                    </Card>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}

