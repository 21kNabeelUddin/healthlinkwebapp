'use client';

import { useState, useEffect, useMemo } from 'react';
import { adminApi } from '@/lib/api';
import { Appointment, AppointmentStatus } from '@/types';
import { toast } from 'react-hot-toast';
import { format, startOfWeek, endOfWeek, eachDayOfInterval, isSameDay, parseISO } from 'date-fns';
import {
  Calendar as CalendarIcon, List, Clock, Filter, Download, Send, 
  CheckCircle2, XCircle, AlertCircle, CalendarDays, ChevronLeft, ChevronRight
} from 'lucide-react';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { Button } from '@/marketing/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/marketing/ui/select';
import { Card, CardContent, CardHeader, CardTitle } from '@/marketing/ui/card';
import { Badge } from '@/marketing/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/marketing/ui/tabs';

export default function EnhancedAppointmentsPage() {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [viewMode, setViewMode] = useState<'calendar' | 'list' | 'timeline'>('calendar');
  const [currentDate, setCurrentDate] = useState(new Date());
  const [filters, setFilters] = useState({
    status: 'ALL' as string,
    doctor: 'ALL' as string,
    patient: 'ALL' as string,
    clinic: 'ALL' as string,
    dateRange: 'ALL' as string,
  });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadAppointments();
  }, [filters]);

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      const data = await adminApi.getAllAppointments(filters.status !== 'ALL' ? filters.status : undefined);
      setAppointments(data || []);
    } catch (error: any) {
      toast.error('Failed to load appointments');
    } finally {
      setIsLoading(false);
    }
  };

  const getStatusColor = (status: AppointmentStatus) => {
    switch (status) {
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'COMPLETED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'NO_SHOW':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const weekDays = useMemo(() => {
    const start = startOfWeek(currentDate, { weekStartsOn: 1 });
    const end = endOfWeek(currentDate, { weekStartsOn: 1 });
    return eachDayOfInterval({ start, end });
  }, [currentDate]);

  const appointmentsByDate = useMemo(() => {
    const grouped: Record<string, Appointment[]> = {};
    appointments.forEach((apt) => {
      const date = format(parseISO(apt.appointmentDateTime), 'yyyy-MM-dd');
      if (!grouped[date]) grouped[date] = [];
      grouped[date].push(apt);
    });
    return grouped;
  }, [appointments]);

  const sidebarItems = [
    { icon: CalendarIcon, label: 'Appointments', href: '/admin/appointments' },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav userName="Admin" userRole="Admin" showPortalLinks={false} onLogout={() => {}} />
      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/admin/appointments" />
        <div className="flex-1 p-8">
          <div className="mb-8">
            <h1 className="text-4xl font-bold text-gray-800 mb-2">Appointment Management</h1>
            <p className="text-gray-600">Manage all platform appointments</p>
          </div>

          {/* View Mode Toggle */}
          <Card className="mb-6">
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <Tabs value={viewMode} onValueChange={(v) => setViewMode(v as any)}>
                  <TabsList>
                    <TabsTrigger value="calendar">
                      <CalendarIcon className="mr-2" size={16} />
                      Calendar
                    </TabsTrigger>
                    <TabsTrigger value="list">
                      <List className="mr-2" size={16} />
                      List
                    </TabsTrigger>
                    <TabsTrigger value="timeline">
                      <Clock className="mr-2" size={16} />
                      Timeline
                    </TabsTrigger>
                  </TabsList>
                </Tabs>

                <div className="flex gap-2">
                  <Select value={filters.status} onValueChange={(v) => setFilters({ ...filters, status: v })}>
                    <SelectTrigger className="w-[180px]">
                      <SelectValue placeholder="Status" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">All Status</SelectItem>
                      <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
                      <SelectItem value="COMPLETED">Completed</SelectItem>
                      <SelectItem value="CANCELLED">Cancelled</SelectItem>
                      <SelectItem value="NO_SHOW">No Show</SelectItem>
                    </SelectContent>
                  </Select>
                  <Button variant="outline" onClick={() => {}}>
                    <Download className="mr-2" size={16} />
                    Export
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Calendar View */}
          {viewMode === 'calendar' && (
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle>
                    {format(currentDate, 'MMMM yyyy')}
                  </CardTitle>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={() => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1))}>
                      <ChevronLeft size={16} />
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => setCurrentDate(new Date())}>
                      Today
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1))}>
                      <ChevronRight size={16} />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-7 gap-2 mb-4">
                  {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((day) => (
                    <div key={day} className="text-center font-semibold text-gray-600 p-2">
                      {day}
                    </div>
                  ))}
                </div>
                <div className="grid grid-cols-7 gap-2">
                  {weekDays.map((day) => {
                    const dateStr = format(day, 'yyyy-MM-dd');
                    const dayAppointments = appointmentsByDate[dateStr] || [];
                    const isToday = isSameDay(day, new Date());
                    return (
                      <div
                        key={dateStr}
                        className={`min-h-[100px] p-2 border rounded-lg ${
                          isToday ? 'bg-blue-50 border-blue-300' : 'bg-white border-gray-200'
                        }`}
                      >
                        <div className={`text-sm font-semibold mb-1 ${isToday ? 'text-blue-600' : 'text-gray-700'}`}>
                          {format(day, 'd')}
                        </div>
                        <div className="space-y-1">
                          {dayAppointments.slice(0, 3).map((apt) => (
                            <div
                              key={apt.id}
                              className={`text-xs p-1 rounded truncate cursor-pointer ${getStatusColor(apt.status)}`}
                              title={`${apt.patientName} - ${format(parseISO(apt.appointmentDateTime), 'h:mm a')}`}
                            >
                              {format(parseISO(apt.appointmentDateTime), 'h:mm a')} - {apt.patientName}
                            </div>
                          ))}
                          {dayAppointments.length > 3 && (
                            <div className="text-xs text-gray-500">
                              +{dayAppointments.length - 3} more
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </CardContent>
            </Card>
          )}

          {/* List View */}
          {viewMode === 'list' && (
            <Card>
              <CardHeader>
                <CardTitle>Appointments List</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {appointments.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">No appointments found</div>
                  ) : (
                    appointments.map((apt) => (
                      <div
                        key={apt.id}
                        className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50"
                      >
                        <div className="flex-1">
                          <div className="flex items-center gap-3 mb-2">
                            <h3 className="font-semibold">
                              {apt.patientName} → {apt.doctorName}
                            </h3>
                            <Badge className={getStatusColor(apt.status)}>
                              {apt.status}
                            </Badge>
                            <Badge variant="outline">{apt.appointmentType}</Badge>
                          </div>
                          <div className="text-sm text-gray-600 space-y-1">
                            <p>
                              <strong>Date & Time:</strong>{' '}
                              {format(parseISO(apt.appointmentDateTime), 'MMM dd, yyyy h:mm a')}
                            </p>
                            <p><strong>Reason:</strong> {apt.reason}</p>
                            {apt.clinicName && <p><strong>Clinic:</strong> {apt.clinicName}</p>}
                          </div>
                        </div>
                        <div className="flex gap-2">
                          <Button variant="outline" size="sm">
                            <Send className="mr-2" size={16} />
                            Remind
                          </Button>
                          <Button variant="outline" size="sm">
                            Reschedule
                          </Button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Timeline View */}
          {viewMode === 'timeline' && (
            <Card>
              <CardHeader>
                <CardTitle>Appointments Timeline</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-6">
                  {Object.entries(
                    appointments.reduce((acc, apt) => {
                      const date = format(parseISO(apt.appointmentDateTime), 'yyyy-MM-dd');
                      if (!acc[date]) acc[date] = [];
                      acc[date].push(apt);
                      return acc;
                    }, {} as Record<string, Appointment[]>)
                  )
                    .sort(([a], [b]) => a.localeCompare(b))
                    .map(([date, dayAppointments]) => (
                      <div key={date} className="border-l-2 border-gray-300 pl-4">
                        <div className="font-semibold text-lg mb-3">
                          {format(parseISO(date), 'EEEE, MMMM dd, yyyy')}
                        </div>
                        <div className="space-y-3">
                          {dayAppointments
                            .sort((a, b) => a.appointmentDateTime.localeCompare(b.appointmentDateTime))
                            .map((apt) => (
                              <div
                                key={apt.id}
                                className="flex items-start gap-4 p-3 bg-white border rounded-lg"
                              >
                                <div className="flex-shrink-0 w-16 text-sm text-gray-600">
                                  {format(parseISO(apt.appointmentDateTime), 'h:mm a')}
                                </div>
                                <div className="flex-1">
                                  <div className="flex items-center gap-2 mb-1">
                                    <span className="font-semibold">{apt.patientName}</span>
                                    <span className="text-gray-400">→</span>
                                    <span className="font-semibold">{apt.doctorName}</span>
                                    <Badge className={getStatusColor(apt.status)}>{apt.status}</Badge>
                                  </div>
                                  <p className="text-sm text-gray-600">{apt.reason}</p>
                                  {apt.clinicName && (
                                    <p className="text-xs text-gray-500 mt-1">📍 {apt.clinicName}</p>
                                  )}
                                </div>
                              </div>
                            ))}
                        </div>
                      </div>
                    ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}

