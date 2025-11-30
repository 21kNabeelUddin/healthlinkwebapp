'use client';

import { useState, useEffect } from 'react';
import { adminApi } from '@/lib/api';
import { Appointment } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { format } from 'date-fns';

export default function AdminAppointmentsPage() {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadAppointments();
  }, [statusFilter]);

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      const data = await adminApi.getAllAppointments(statusFilter || undefined);
      setAppointments(data || []);
    } catch (error: any) {
      toast.error('Failed to load appointments');
      console.error('Appointments load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (appointmentId: string) => {
    if (!confirm('Are you sure you want to delete this appointment?')) return;

    try {
      await adminApi.deleteAppointment(appointmentId);
      toast.success('Appointment deleted successfully');
      loadAppointments();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to delete appointment');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CONFIRMED':
        return 'bg-green-100 text-green-800';
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      case 'COMPLETED':
        return 'bg-blue-100 text-blue-800';
      case 'REJECTED':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="ADMIN">
        <div className="text-center">Loading...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="ADMIN">
      <div className="mb-8">
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Appointment Management</h1>
        <p className="text-gray-600">Manage all appointments</p>
      </div>

      <div className="mb-6">
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-4 py-2 border rounded-lg"
        >
          <option value="">All Statuses</option>
          <option value="PENDING">Pending</option>
          <option value="CONFIRMED">Confirmed</option>
          <option value="COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>

      <div className="space-y-4">
        {appointments.length === 0 ? (
          <Card>
            <div className="text-center py-8 text-gray-500">
              No appointments found
            </div>
          </Card>
        ) : (
          appointments.map((appointment) => (
            <Card key={appointment.id}>
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="text-xl font-semibold text-gray-800">
                      {appointment.patientName} → {appointment.doctorName}
                    </h3>
                    <span className={`px-2 py-1 rounded text-xs ${getStatusColor(appointment.status)}`}>
                      {appointment.status}
                    </span>
                    <span className="px-2 py-1 rounded text-xs bg-blue-100 text-blue-800">
                      {appointment.appointmentType}
                    </span>
                  </div>

                  <div className="space-y-1 text-gray-600">
                    <p>
                      <strong>Date & Time:</strong>{' '}
                      {format(new Date(appointment.appointmentDateTime), 'MMM dd, yyyy h:mm a')}
                    </p>
                    <p><strong>Reason:</strong> {appointment.reason}</p>
                    {appointment.clinicName && (
                      <p><strong>Clinic:</strong> {appointment.clinicName}</p>
                    )}
                  </div>
                </div>

                <Button
                  variant="danger"
                  onClick={() => handleDelete(appointment.id.toString())}
                >
                  Delete
                </Button>
              </div>
            </Card>
          ))
        )}
      </div>
    </DashboardLayout>
  );
}

