import React, { useState, useEffect } from 'react';
import { Video, Star, FileText, Calendar, Clock, MapPin, DollarSign } from 'lucide-react';
import { Card, CardContent } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { Modal } from '../ui/Modal';
import { EmptyState } from '../ui/EmptyState';
import { CardSkeleton } from '../ui/Skeleton';
import { appointmentsApi, reviewsApi, prescriptionsApi } from '../../utils/mockApi';
import type { Appointment, AppointmentStatus } from '../../utils/mockData';

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

export function PatientAppointments() {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [showRatingModal, setShowRatingModal] = useState(false);
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');

  useEffect(() => {
    loadAppointments();
  }, []);

  const loadAppointments = async () => {
    setLoading(true);
    try {
      const data = await appointmentsApi.listForPatient('pat-1');
      setAppointments(data);
    } catch (error) {
      console.error('Failed to load appointments:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleJoinZoom = (appointment: Appointment) => {
    if (appointment.zoomJoinUrl) {
      window.open(appointment.zoomJoinUrl, '_blank');
    }
  };

  const handleRateAppointment = async () => {
    if (selectedAppointment) {
      await reviewsApi.create(selectedAppointment.id, rating, comment);
      setShowRatingModal(false);
      setRating(5);
      setComment('');
      loadAppointments();
    }
  };

  const upcomingAppointments = appointments.filter(
    apt => ['PENDING_PAYMENT', 'CONFIRMED', 'IN_PROGRESS'].includes(apt.status)
  );

  const pastAppointments = appointments.filter(
    apt => ['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(apt.status)
  );

  const AppointmentCard = ({ appointment }: { appointment: Appointment }) => (
    <Card className="hover:shadow-md transition-shadow">
      <CardContent>
        <div className="flex items-start justify-between mb-3">
          <div>
            <h4 className="text-neutral-900">{appointment.doctorName}</h4>
            <p className="text-sm text-neutral-600">{appointment.facilityName}</p>
            <div className="flex items-center gap-2 mt-2">
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
            <span className="text-neutral-900">
              {new Date(appointment.date).toLocaleDateString('en-US', { 
                weekday: 'short', 
                month: 'short', 
                day: 'numeric',
                year: 'numeric'
              })}
            </span>
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
          {appointment.type === 'ONLINE' && appointment.status === 'CONFIRMED' && (
            <Button
              size="sm"
              onClick={() => handleJoinZoom(appointment)}
            >
              <Video className="w-4 h-4" />
              Join Zoom
            </Button>
          )}
          
          {appointment.status === 'COMPLETED' && !appointment.hasReview && (
            <Button
              size="sm"
              variant="secondary"
              onClick={() => {
                setSelectedAppointment(appointment);
                setShowRatingModal(true);
              }}
            >
              <Star className="w-4 h-4" />
              Rate
            </Button>
          )}
          
          <Button
            size="sm"
            variant="secondary"
          >
            <FileText className="w-4 h-4" />
            Prescription
          </Button>
        </div>
      </CardContent>
    </Card>
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-neutral-900">Appointments</h2>
        <p className="text-neutral-600 mt-1">View and manage your medical appointments</p>
      </div>

      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map(i => <CardSkeleton key={i} />)}
        </div>
      ) : appointments.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Calendar className="w-8 h-8" />}
            title="No appointments found"
            description="You don't have any appointments scheduled."
          />
        </Card>
      ) : (
        <>
          {/* Upcoming Appointments */}
          {upcomingAppointments.length > 0 && (
            <div>
              <h3 className="text-neutral-900 mb-4">Upcoming Appointments</h3>
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                {upcomingAppointments.map(appointment => (
                  <AppointmentCard key={appointment.id} appointment={appointment} />
                ))}
              </div>
            </div>
          )}

          {/* Past Appointments */}
          {pastAppointments.length > 0 && (
            <div>
              <h3 className="text-neutral-900 mb-4">Past Appointments</h3>
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                {pastAppointments.map(appointment => (
                  <AppointmentCard key={appointment.id} appointment={appointment} />
                ))}
              </div>
            </div>
          )}
        </>
      )}

      {/* Rating Modal */}
      <Modal
        isOpen={showRatingModal}
        onClose={() => setShowRatingModal(false)}
        title="Rate Your Appointment"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowRatingModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleRateAppointment}>
              Submit Rating
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <label className="block text-neutral-700 mb-3">Your Rating</label>
            <div className="flex gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  onClick={() => setRating(star)}
                  className="text-3xl transition-colors"
                >
                  <Star
                    className={`w-8 h-8 ${
                      star <= rating ? 'fill-warning-500 text-warning-500' : 'text-neutral-300'
                    }`}
                  />
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-neutral-700 mb-2">Comments (Optional)</label>
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Share your experience..."
              rows={4}
              className="w-full px-4 py-2.5 bg-white border border-neutral-300 rounded-lg focus:border-primary-600 focus:ring-2 focus:ring-primary-100 outline-none"
            />
          </div>
        </div>
      </Modal>
    </div>
  );
}
