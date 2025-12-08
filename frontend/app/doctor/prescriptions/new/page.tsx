'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { useForm } from 'react-hook-form';
import { prescriptionsApi, appointmentsApi } from '@/lib/api';
import { Appointment } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import { FileText, Plus, X, AlertTriangle, User, Calendar, Pill } from 'lucide-react';
import Link from 'next/link';

interface PrescriptionFormData {
  title: string;
  body: string;
  medications: string[];
}

export default function NewPrescriptionPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const appointmentId = searchParams.get('appointmentId');
  const patientId = searchParams.get('patientId');
  
  const [appointment, setAppointment] = useState<Appointment | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingAppointment, setIsLoadingAppointment] = useState(!!appointmentId);
  const [medications, setMedications] = useState<string[]>(['']);
  const [interactionWarnings, setInteractionWarnings] = useState<string[]>([]);
  const [isCheckingInteractions, setIsCheckingInteractions] = useState(false);

  const { register, handleSubmit, formState: { errors }, setValue } = useForm<PrescriptionFormData>({
    defaultValues: {
      title: '',
      body: '',
    },
  });

  useEffect(() => {
    if (appointmentId) {
      loadAppointment();
    }
  }, [appointmentId]);

  const loadAppointment = async () => {
    if (!appointmentId) return;
    setIsLoadingAppointment(true);
    try {
      const apt = await appointmentsApi.getById(appointmentId);
      setAppointment(apt);
      // Pre-fill title
      if (apt.patientName) {
        setValue('title', `Prescription for ${apt.patientName} - ${new Date(apt.appointmentDateTime).toLocaleDateString()}`);
      }
    } catch (error: any) {
      toast.error('Failed to load appointment details');
      console.error('Appointment load error:', error);
    } finally {
      setIsLoadingAppointment(false);
    }
  };

  const addMedication = () => {
    setMedications([...medications, '']);
  };

  const removeMedication = (index: number) => {
    setMedications(medications.filter((_, i) => i !== index));
  };

  const updateMedication = (index: number, value: string) => {
    const updated = [...medications];
    updated[index] = value;
    setMedications(updated);
  };

  const checkInteractions = async () => {
    const medsToCheck = medications.filter(m => m.trim() !== '');
    if (medsToCheck.length < 2) {
      setInteractionWarnings([]);
      return;
    }

    setIsCheckingInteractions(true);
    try {
      const response = await prescriptionsApi.checkInteractions({ medications: medsToCheck });
      // Response format: { warnings: string[] }
      const warnings = response.warnings || (Array.isArray(response) ? response : []);
      setInteractionWarnings(warnings);
      if (response.warnings && response.warnings.length > 0) {
        toast.warning(`Found ${response.warnings.length} potential drug interaction(s)`);
      } else {
        toast.success('No drug interactions detected');
      }
    } catch (error: any) {
      console.error('Interaction check error:', error);
      toast.error('Failed to check drug interactions');
    } finally {
      setIsCheckingInteractions(false);
    }
  };

  useEffect(() => {
    // Auto-check interactions when medications change (debounced)
    const timer = setTimeout(() => {
      if (medications.filter(m => m.trim() !== '').length >= 2) {
        checkInteractions();
      } else {
        setInteractionWarnings([]);
      }
    }, 1000);

    return () => clearTimeout(timer);
  }, [medications]);

  const onSubmit = async (data: PrescriptionFormData) => {
    if (!user?.id) return;

    if (!appointmentId && !patientId) {
      toast.error('Appointment ID or Patient ID is required');
      return;
    }

    // Validate appointment has started if appointmentId is provided
    if (appointmentId && appointment) {
      const appointmentTime = new Date(appointment.appointmentDateTime);
      const now = new Date();
      if (now < appointmentTime) {
        toast.error(
          `Cannot create prescription before appointment time. Appointment starts on ${new Date(appointment.appointmentDateTime).toLocaleString()}`
        );
        return;
      }
    }

    // Validate all fields are filled
    if (!data.title || !data.title.trim()) {
      toast.error('Prescription title is required');
      return;
    }

    if (!data.body || !data.body.trim()) {
      toast.error('Prescription instructions are required');
      return;
    }

    const medsList = medications.filter(m => m.trim() !== '');
    if (medsList.length === 0) {
      toast.error('At least one medication is required');
      return;
    }

    setIsLoading(true);
    try {
      const prescriptionData: any = {
        title: data.title,
        body: data.body,
        medications: medsList,
      };

      // Add appointmentId if available (backend expects UUID)
      if (appointmentId) {
        prescriptionData.appointmentId = appointmentId;
      }

      // Add patientId (required, backend expects UUID)
      const finalPatientId = patientId || (appointment?.patientId ? appointment.patientId.toString() : '');
      if (finalPatientId) {
        prescriptionData.patientId = finalPatientId;
      } else {
        toast.error('Patient ID is required');
        setIsLoading(false);
        return;
      }

      await prescriptionsApi.create(prescriptionData);
      toast.success('Prescription created successfully!');
      
      if (appointmentId) {
        router.push(`/doctor/appointments`);
      } else {
        router.push('/doctor/prescriptions');
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to create prescription');
      console.error('Prescription creation error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoadingAppointment) {
    return (
      <DashboardLayout requiredUserType="DOCTOR">
        <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
          <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="text-center py-20">
              <div className="inline-block animate-spin rounded-full h-12 w-12 border-4 border-teal-500 border-t-transparent"></div>
              <p className="mt-4 text-slate-600">Loading appointment details...</p>
            </div>
          </div>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          {/* Header */}
          <div className="mb-6">
            <Link
              href={appointmentId ? '/doctor/appointments' : '/doctor/prescriptions'}
              className="inline-flex items-center gap-2 text-teal-600 hover:text-teal-700 text-sm font-medium mb-4"
            >
              ← Back
            </Link>
            <div className="flex items-center gap-3 mb-2">
              <div className="w-12 h-12 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center">
                <FileText className="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 className="text-3xl font-bold text-slate-900">Create Prescription</h1>
                <p className="text-slate-600 text-sm mt-1">Create a new prescription for your patient</p>
              </div>
            </div>
          </div>

          {/* Appointment Info Card */}
          {appointment && (
            <Card className="mb-6 bg-gradient-to-br from-teal-50 to-violet-50 border-teal-200">
              <div className="flex items-start gap-4">
                <div className="w-10 h-10 bg-teal-500 rounded-lg flex items-center justify-center flex-shrink-0">
                  <User className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1">
                  <h3 className="font-semibold text-slate-900 mb-1">{appointment.patientName}</h3>
                  <div className="flex items-center gap-4 text-sm text-slate-600">
                    <div className="flex items-center gap-1">
                      <Calendar className="w-4 h-4" />
                      <span>{new Date(appointment.appointmentDateTime).toLocaleDateString()}</span>
                    </div>
                    {appointment.reason && (
                      <span className="text-slate-500">Reason: {appointment.reason}</span>
                    )}
                  </div>
                </div>
              </div>
            </Card>
          )}

          <Card className="shadow-lg">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Title */}
              <Input
                label="Prescription Title *"
                {...register('title', { required: 'Title is required' })}
                error={errors.title?.message}
                placeholder="e.g., Prescription for John Doe - Dec 7, 2025"
              />

              {/* Medications */}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-3">
                  Medications *
                </label>
                <div className="space-y-3">
                  {medications.map((med, index) => (
                    <div key={index} className="flex items-center gap-2">
                      <div className="flex-1">
                        <Input
                          value={med}
                          onChange={(e) => updateMedication(index, e.target.value)}
                          placeholder={`Medication ${index + 1} (e.g., Paracetamol 500mg)`}
                        />
                      </div>
                      {medications.length > 1 && (
                        <Button
                          type="button"
                          variant="danger"
                          className="text-sm px-3 py-1.5"
                          onClick={() => removeMedication(index)}
                        >
                          <X className="w-4 h-4" />
                        </Button>
                      )}
                    </div>
                  ))}
                  <Button
                    type="button"
                    variant="secondary"
                    className="text-sm px-3 py-1.5"
                    onClick={addMedication}
                  >
                    <Plus className="w-4 h-4 mr-2" />
                    Add Medication
                  </Button>
                </div>
                <p className="mt-2 text-xs text-slate-500">
                  Enter medication names. Drug interactions will be checked automatically.
                </p>
              </div>

              {/* Drug Interaction Warnings */}
              {interactionWarnings.length > 0 && (
                <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg">
                  <div className="flex items-start gap-2 mb-2">
                    <AlertTriangle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
                    <div className="flex-1">
                      <h4 className="font-semibold text-amber-900 mb-2">Drug Interaction Warnings</h4>
                      <ul className="space-y-1">
                        {interactionWarnings.map((warning, index) => (
                          <li key={index} className="text-sm text-amber-800">
                            • {warning}
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </div>
              )}

              {/* Body/Instructions */}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Prescription Instructions *
                </label>
                <textarea
                  {...register('body', { required: 'Prescription instructions are required' })}
                  className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 resize-none"
                  rows={8}
                  placeholder="Enter detailed prescription instructions, dosage information, frequency, duration, and any special instructions for the patient..."
                />
                {errors.body && (
                  <p className="mt-1 text-sm text-red-600">{errors.body.message}</p>
                )}
              </div>

              {/* Action Buttons */}
              <div className="flex gap-4 pt-6 border-t border-slate-200">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => router.back()}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  isLoading={isLoading}
                  className="flex-1 bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700 text-white"
                >
                  Create Prescription
                </Button>
              </div>
            </form>
          </Card>
        </div>
      </div>
    </DashboardLayout>
  );
}

