import React, { useState, useEffect } from 'react';
import { Activity, Calendar, User, Building2, FileText, Filter } from 'lucide-react';
import { Card, CardContent } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { CardSkeleton } from '../ui/Skeleton';
import { Select } from '../ui/Select';
import { medicalHistoryApi } from '../../utils/mockApi';
import type { MedicalHistory } from '../../utils/mockData';

export function PatientMedicalHistory() {
  const [history, setHistory] = useState<MedicalHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState('ALL');

  useEffect(() => {
    loadHistory();
  }, [typeFilter]);

  const loadHistory = async () => {
    setLoading(true);
    try {
      const data = await medicalHistoryApi.list('pat-1', {
        type: typeFilter === 'ALL' ? undefined : typeFilter
      });
      setHistory(data);
    } catch (error) {
      console.error('Failed to load medical history:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredHistory = history.filter(item => {
    if (typeFilter !== 'ALL' && item.type !== typeFilter) return false;
    return true;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-neutral-900">Medical History</h2>
        <p className="text-neutral-600 mt-1">View your complete medical records and consultations</p>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="!py-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Select
              options={[
                { value: 'ALL', label: 'All Types' },
                { value: 'ONLINE', label: 'Online Consultations' },
                { value: 'ONSITE', label: 'On-site Visits' }
              ]}
              value={typeFilter}
              onChange={setTypeFilter}
            />
          </div>
        </CardContent>
      </Card>

      {/* Timeline */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map(i => <CardSkeleton key={i} />)}
        </div>
      ) : filteredHistory.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Activity className="w-8 h-8" />}
            title="No medical history found"
            description="Your medical records will appear here after your appointments."
          />
        </Card>
      ) : (
        <div className="relative">
          {/* Timeline line */}
          <div className="absolute left-6 top-0 bottom-0 w-0.5 bg-neutral-200" />
          
          <div className="space-y-6">
            {filteredHistory.map((record) => (
              <div key={record.id} className="relative pl-16">
                {/* Timeline dot */}
                <div className="absolute left-4 top-6 w-4 h-4 bg-primary-600 rounded-full border-4 border-white shadow-sm" />
                
                <Card className="hover:shadow-md transition-shadow">
                  <CardContent>
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-2">
                          <h4 className="text-neutral-900">{record.diagnosis}</h4>
                          <Badge variant={record.type === 'ONLINE' ? 'info' : 'default'}>
                            {record.type}
                          </Badge>
                        </div>
                        
                        <div className="space-y-1 text-sm">
                          <div className="flex items-center gap-2 text-neutral-600">
                            <Calendar className="w-4 h-4" />
                            <span className="text-neutral-900">
                              {new Date(record.date).toLocaleDateString('en-US', {
                                month: 'long',
                                day: 'numeric',
                                year: 'numeric'
                              })}
                            </span>
                          </div>
                          
                          <div className="flex items-center gap-2 text-neutral-600">
                            <User className="w-4 h-4" />
                            <span>{record.doctorName}</span>
                          </div>
                          
                          <div className="flex items-center gap-2 text-neutral-600">
                            <Building2 className="w-4 h-4" />
                            <span>{record.facilityName}</span>
                          </div>
                        </div>
                      </div>
                    </div>

                    {record.notes && (
                      <div className="mt-3 p-3 bg-neutral-50 rounded-lg">
                        <p className="text-sm text-neutral-700">{record.notes}</p>
                      </div>
                    )}

                    {record.prescriptionId && (
                      <div className="mt-3 pt-3 border-t border-neutral-200">
                        <Button size="sm" variant="secondary">
                          <FileText className="w-4 h-4" />
                          View Prescription
                        </Button>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </div>
            ))}
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
