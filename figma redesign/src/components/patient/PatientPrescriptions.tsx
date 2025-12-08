import React, { useState, useEffect } from 'react';
import { FileText, Calendar, User, Building2, X, Download, Printer, AlertTriangle } from 'lucide-react';
import { Card, CardContent, CardHeader } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { CardSkeleton } from '../ui/Skeleton';
import { prescriptionsApi } from '../../utils/mockApi';
import type { Prescription } from '../../utils/mockData';

export function PatientPrescriptions() {
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedPrescription, setSelectedPrescription] = useState<Prescription | null>(null);
  const [showDrawer, setShowDrawer] = useState(false);

  useEffect(() => {
    loadPrescriptions();
  }, []);

  const loadPrescriptions = async () => {
    setLoading(true);
    try {
      const data = await prescriptionsApi.listForPatient('pat-1');
      setPrescriptions(data);
    } catch (error) {
      console.error('Failed to load prescriptions:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetails = async (prescription: Prescription) => {
    const details = await prescriptionsApi.getDetail(prescription.id);
    if (details) {
      setSelectedPrescription(details);
      setShowDrawer(true);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-neutral-900">Prescriptions</h2>
        <p className="text-neutral-600 mt-1">View your medical prescriptions and medications</p>
      </div>

      {/* Prescriptions List */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map(i => <CardSkeleton key={i} />)}
        </div>
      ) : prescriptions.length === 0 ? (
        <Card>
          <EmptyState
            icon={<FileText className="w-8 h-8" />}
            title="No prescriptions found"
            description="You don't have any prescriptions on file."
          />
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {prescriptions.map((prescription) => (
            <Card key={prescription.id} className="hover:shadow-md transition-shadow">
              <CardContent>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-start gap-4">
                      <div className="w-12 h-12 bg-secondary-100 rounded-lg flex items-center justify-center flex-shrink-0">
                        <FileText className="w-6 h-6 text-secondary-700" />
                      </div>
                      
                      <div className="flex-1">
                        <h4 className="text-neutral-900 mb-1">{prescription.diagnosis}</h4>
                        
                        <div className="space-y-1 text-sm mb-3">
                          <div className="flex items-center gap-2 text-neutral-600">
                            <User className="w-4 h-4" />
                            <span>{prescription.doctorName}</span>
                          </div>
                          <div className="flex items-center gap-2 text-neutral-600">
                            <Building2 className="w-4 h-4" />
                            <span>{prescription.facilityName}</span>
                          </div>
                          <div className="flex items-center gap-2 text-neutral-600">
                            <Calendar className="w-4 h-4" />
                            <span>{new Date(prescription.date).toLocaleDateString()}</span>
                          </div>
                        </div>

                        <div className="flex flex-wrap gap-2">
                          {prescription.medications.slice(0, 3).map((med, idx) => (
                            <Badge key={idx} variant="info">
                              {med.name}
                            </Badge>
                          ))}
                          {prescription.medications.length > 3 && (
                            <Badge variant="default">
                              +{prescription.medications.length - 3} more
                            </Badge>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>

                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => handleViewDetails(prescription)}
                  >
                    View Details
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Details Drawer */}
      {showDrawer && selectedPrescription && (
        <div className="fixed inset-0 z-50 overflow-hidden">
          <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={() => setShowDrawer(false)} />
          
          <div className="absolute right-0 top-0 h-full w-full max-w-2xl bg-white shadow-2xl overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-neutral-200 px-6 py-4 flex items-center justify-between z-10">
              <h3 className="text-neutral-900">Prescription Details</h3>
              <button
                onClick={() => setShowDrawer(false)}
                className="p-2 hover:bg-neutral-100 rounded-lg transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6">
              {/* Header Info */}
              <div className="grid grid-cols-2 gap-4 p-4 bg-neutral-50 rounded-lg">
                <div>
                  <label className="text-sm text-neutral-600">Doctor</label>
                  <p className="text-neutral-900">{selectedPrescription.doctorName}</p>
                </div>
                <div>
                  <label className="text-sm text-neutral-600">Clinic</label>
                  <p className="text-neutral-900">{selectedPrescription.facilityName}</p>
                </div>
                <div>
                  <label className="text-sm text-neutral-600">Date</label>
                  <p className="text-neutral-900">
                    {new Date(selectedPrescription.date).toLocaleDateString('en-US', {
                      month: 'long',
                      day: 'numeric',
                      year: 'numeric'
                    })}
                  </p>
                </div>
                <div>
                  <label className="text-sm text-neutral-600">Diagnosis</label>
                  <p className="text-neutral-900">{selectedPrescription.diagnosis}</p>
                </div>
              </div>

              {/* Medications */}
              <div>
                <h4 className="text-neutral-900 mb-4">Medications</h4>
                <div className="space-y-3">
                  {selectedPrescription.medications.map((med, idx) => (
                    <Card key={idx}>
                      <CardContent className="!p-4">
                        <h4 className="text-neutral-900 mb-2">{med.name}</h4>
                        <div className="grid grid-cols-3 gap-3 text-sm">
                          <div>
                            <label className="text-neutral-600">Dosage</label>
                            <p className="text-neutral-900">{med.dosage}</p>
                          </div>
                          <div>
                            <label className="text-neutral-600">Frequency</label>
                            <p className="text-neutral-900">{med.frequency}</p>
                          </div>
                          <div>
                            <label className="text-neutral-600">Duration</label>
                            <p className="text-neutral-900">{med.duration}</p>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </div>

              {/* Warnings */}
              {selectedPrescription.warnings && selectedPrescription.warnings.length > 0 && (
                <div className="p-4 bg-warning-50 border border-warning-200 rounded-lg">
                  <div className="flex items-start gap-3">
                    <AlertTriangle className="w-5 h-5 text-warning-600 flex-shrink-0 mt-0.5" />
                    <div className="flex-1">
                      <h4 className="text-warning-900 mb-2">Important Warnings</h4>
                      <ul className="list-disc list-inside space-y-1 text-sm text-warning-800">
                        {selectedPrescription.warnings.map((warning, idx) => (
                          <li key={idx}>{warning}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </div>
              )}

              {/* Notes */}
              {selectedPrescription.notes && (
                <div>
                  <h4 className="text-neutral-900 mb-2">Doctor's Notes</h4>
                  <p className="text-neutral-700 bg-neutral-50 p-4 rounded-lg">
                    {selectedPrescription.notes}
                  </p>
                </div>
              )}

              {/* Actions */}
              <div className="flex gap-3 pt-4 border-t border-neutral-200">
                <Button variant="secondary">
                  <Download className="w-4 h-4" />
                  Download PDF
                </Button>
                <Button variant="secondary">
                  <Printer className="w-4 h-4" />
                  Print
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Building2({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
    </svg>
  );
}
