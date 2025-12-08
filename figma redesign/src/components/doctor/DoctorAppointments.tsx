import React, { useState, useEffect } from 'react';
import { Search, Filter, Video, FileText, CheckCircle, Eye, Calendar, DollarSign, MapPin, Clock } from 'lucide-react';
import { Card, CardContent } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Modal } from '../ui/Modal';
import { EmptyState } from '../ui/EmptyState';
import { CardSkeleton } from '../ui/Skeleton';
import { appointmentsApi, facilitiesApi, prescriptionsApi } from '../../utils/mockApi';
import type { Appointment, Facility, AppointmentStatus } from '../../utils/mockData';

const statusOptions: { value: AppointmentStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'All Statuses' },
  { value: 'PENDING_PAYMENT', label: 'Pending Payment' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'NO_SHOW', label: 'No Show' }
];

const getStatusBadgeVariant = (status: AppointmentStatus) => {
  const variants: Record<AppointmentStatus, any> = {
    PENDING_PAYMENT: 'pending',
    CONFIRMED: 'confirmed',
    IN_PROGRESS: 'in-progress',
    COMPLETED: 'completed',
    CANCELLED: 'cancelled',
    NO_SHOW: 'no-show'
  };
  return variants[status];
};

const getStatusLabel = (status: AppointmentStatus) => {
  return status.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ');
};

const canStartZoom = (date: string, time: string): boolean => {
  // In a real app, check if current time is within 5 minutes of appointment
  return true; // Mock implementation
};

export function DoctorAppointments() {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<AppointmentStatus | 'ALL'>('ALL');
  const [facilityFilter, setFacilityFilter] = useState('ALL');
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);

  useEffect(() => {
    loadData();
  }, [statusFilter, facilityFilter, search]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [aptsData, facsData] = await Promise.all([
        appointmentsApi.listForDoctor('doc-1', {
          status: statusFilter === 'ALL' ? undefined : statusFilter,
          facilityId: facilityFilter === 'ALL' ? undefined : facilityFilter,
          search
        }),
        facilitiesApi.listForDoctor('doc-1')
      ]);
      setAppointments(aptsData);
      setFacilities(facsData);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleStartZoom = (appointment: Appointment) => {
    if (appointment.zoomStartUrl) {
      window.open(appointment.zoomStartUrl, '_blank');
    }
  };

  const handleConcludeAppointment = async (appointment: Appointment) => {
    if (confirm('Mark this appointment as completed?')) {
      await appointmentsApi.complete(appointment.id);
      loadData();
    }
  };

  const groupedAppointments = appointments.reduce((acc, apt) => {
    const facilityName = apt.facilityName;
    if (!acc[facilityName]) {
      acc[facilityName] = [];
    }
    acc[facilityName].push(apt);
    return acc;
  }, {} as Record<string, Appointment[]>);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-neutral-900">Appointments</h2>
          <p className="text-neutral-600 mt-1">Manage your patient appointments</p>
        </div>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="!py-4">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="relative md:col-span-2">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-neutral-400" />
              <input
                type="text"
                placeholder="Search patient name..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 bg-white border border-neutral-300 rounded-lg focus:border-primary-600 focus:ring-2 focus:ring-primary-100 outline-none"
              />
            </div>
            <Select
              options={statusOptions}
              value={statusFilter}
              onChange={(value) => setStatusFilter(value as AppointmentStatus | 'ALL')}
            />
            <Select
              options={[
                { value: 'ALL', label: 'All Clinics' },
                ...facilities.map(f => ({ value: f.id, label: f.name }))
              ]}
              value={facilityFilter}
              onChange={setFacilityFilter}
            />
          </div>
        </CardContent>
      </Card>

      {/* Appointments List */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map(i => <CardSkeleton key={i} />)}
        </div>
      ) : appointments.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Calendar className="w-8 h-8" />}
            title="No appointments found"
            description="You don't have any appointments matching the current filters."
          />
        </Card>
      ) : (
        <div className="space-y-6">
          {Object.entries(groupedAppointments).map(([facilityName, facilityAppointments]) => (
            <div key={facilityName}>
              <div className="flex items-center gap-2 mb-3">
                <Building2 className="w-5 h-5 text-primary-600" />
                <h3 className="text-neutral-900">{facilityName}</h3>
                <span className="text-neutral-500">({facilityAppointments.length})</span>
              </div>
              
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                {facilityAppointments.map((appointment) => (
                  <Card key={appointment.id} className="hover:shadow-md transition-shadow">
                    <CardContent>
                      <div className="flex items-start justify-between mb-3">
                        <div>
                          <h4 className="text-neutral-900">{appointment.patientName}</h4>
                          <div className="flex items-center gap-2 mt-1">
                            <Badge variant={appointment.type === 'ONLINE' ? 'info' : 'default'}>
                              {appointment.type === 'ONLINE' ? 'ONLINE' : 'ON-SITE'}
                            </Badge>
                            <Badge variant={getStatusBadgeVariant(appointment.status)}>
                              {getStatusLabel(appointment.status)}
                            </Badge>
                          </div>
                        </div>
                      </div>

                      <div className="space-y-2 mb-4 text-sm">
                        <div className="flex items-center gap-2 text-neutral-600">
                          <Calendar className="w-4 h-4" />
                          <span className="text-neutral-900">{new Date(appointment.date).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })}</span>
                        </div>
                        <div className="flex items-center gap-2 text-neutral-600">
                          <Clock className="w-4 h-4" />
                          <span className="text-neutral-900">{appointment.time}</span>
                        </div>
                        <div className="flex items-center gap-2 text-neutral-600">
                          <DollarSign className="w-4 h-4" />
                          <span className="text-neutral-900">PKR {appointment.fee.toLocaleString()}</span>
                        </div>
                      </div>

                      <div className="flex flex-wrap gap-2">
                        {appointment.type === 'ONLINE' && appointment.status === 'CONFIRMED' && canStartZoom(appointment.date, appointment.time) && (
                          <Button
                            size="sm"
                            onClick={() => handleStartZoom(appointment)}
                          >
                            <Video className="w-4 h-4" />
                            Start Zoom
                          </Button>
                        )}
                        
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => {
                            setSelectedAppointment(appointment);
                            setShowDetailsModal(true);
                          }}
                        >
                          <Eye className="w-4 h-4" />
                          Details
                        </Button>

                        {appointment.status === 'IN_PROGRESS' && (
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={() => handleConcludeAppointment(appointment)}
                          >
                            <CheckCircle className="w-4 h-4" />
                            Conclude
                          </Button>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Details Modal */}
      <Modal
        isOpen={showDetailsModal}
        onClose={() => setShowDetailsModal(false)}
        title="Appointment Details"
      >
        {selectedAppointment && (
          <div className="space-y-4">
            <div>
              <label className="text-sm text-neutral-600">Patient</label>
              <p className="text-neutral-900">{selectedAppointment.patientName}</p>
            </div>
            <div>
              <label className="text-sm text-neutral-600">Clinic</label>
              <p className="text-neutral-900">{selectedAppointment.facilityName}</p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm text-neutral-600">Date</label>
                <p className="text-neutral-900">{new Date(selectedAppointment.date).toLocaleDateString()}</p>
              </div>
              <div>
                <label className="text-sm text-neutral-600">Time</label>
                <p className="text-neutral-900">{selectedAppointment.time}</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm text-neutral-600">Type</label>
                <p className="text-neutral-900">{selectedAppointment.type}</p>
              </div>
              <div>
                <label className="text-sm text-neutral-600">Status</label>
                <p className="text-neutral-900">{getStatusLabel(selectedAppointment.status)}</p>
              </div>
            </div>
            <div>
              <label className="text-sm text-neutral-600">Fee</label>
              <p className="text-neutral-900">PKR {selectedAppointment.fee.toLocaleString()}</p>
            </div>
            {selectedAppointment.notes && (
              <div>
                <label className="text-sm text-neutral-600">Notes</label>
                <p className="text-neutral-900">{selectedAppointment.notes}</p>
              </div>
            )}
          </div>
        )}
      </Modal>
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
