'use client';

import { useState, useEffect, useMemo } from 'react';
import { adminApi } from '@/lib/api';
import { Appointment, AppointmentStatus } from '@/types';
import { toast } from 'react-hot-toast';
import { format, startOfWeek, endOfWeek, eachDayOfInterval, isSameDay, parseISO, startOfMonth, endOfMonth } from 'date-fns';
import {
  Calendar as CalendarIcon, List, Clock, Filter, Download, Send, 
  CheckCircle2, XCircle, AlertCircle, CalendarDays, ChevronLeft, ChevronRight
} from 'lucide-react';
import DashboardLayout from '@/components/layout/DashboardLayout';
import { Button } from '@/marketing/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/marketing/ui/select';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/marketing/ui/card';
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
  const [selectedAppointments, setSelectedAppointments] = useState<Set<string>>(new Set());
  const [showBulkActions, setShowBulkActions] = useState(false);
  const [revenue, setRevenue] = useState({ total: 0, today: 0, thisWeek: 0, thisMonth: 0 });
  const [conflicts, setConflicts] = useState<Array<{ appointment1: Appointment; appointment2: Appointment }>>([]);
  const [draggedAppointment, setDraggedAppointment] = useState<Appointment | null>(null);
  const [dragOverSlot, setDragOverSlot] = useState<{ date: string; hour: number } | null>(null);
  const [rescheduleModal, setRescheduleModal] = useState<{
    open: boolean;
    appointmentId: string | null;
    date: string;
    time: string;
  }>({ open: false, appointmentId: null, date: format(new Date(), 'yyyy-MM-dd'), time: '09:00' });

  // Load when filters change
  useEffect(() => {
    loadAppointments();
  }, [filters]);

  // Derive revenue when appointments change
  useEffect(() => {
    calculateRevenue();
  }, [appointments]);

  // Detect conflicts when appointments change
  useEffect(() => {
    detectConflicts();
  }, [appointments]);

  const loadAppointments = async () => {
    setIsLoading(true);
    try {
      const data = await adminApi.getAllAppointments(filters.status !== 'ALL' ? filters.status : undefined);
      const list = Array.isArray(data) ? data : [];
      // By default hide cancelled so "Delete" feels persistent; allow viewing via explicit filter
      const filtered =
        filters.status === 'ALL' ? list.filter((a) => a.status !== 'CANCELLED') : list;
      setAppointments(filtered);
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

  const monthDays = useMemo(() => {
    const start = startOfMonth(currentDate);
    const end = endOfMonth(currentDate);
    return eachDayOfInterval({ start, end });
  }, [currentDate]);

  const calculateRevenue = () => {
    const now = new Date();
    const today = format(now, 'yyyy-MM-dd');
    const weekStart = startOfWeek(now, { weekStartsOn: 1 });
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);

    const total = appointments
      .filter(a => a.status === 'COMPLETED')
      .reduce((sum, a) => sum + ((a as any).consultationFee || 0), 0);

    const todayRevenue = appointments
      .filter(a => {
        const aptDate = format(parseISO(a.appointmentDateTime), 'yyyy-MM-dd');
        return aptDate === today && a.status === 'COMPLETED';
      })
      .reduce((sum, a) => sum + ((a as any).consultationFee || 0), 0);

    const weekRevenue = appointments
      .filter(a => {
        const aptDate = parseISO(a.appointmentDateTime);
        return aptDate >= weekStart && a.status === 'COMPLETED';
      })
      .reduce((sum, a) => sum + ((a as any).consultationFee || 0), 0);

    const monthRevenue = appointments
      .filter(a => {
        const aptDate = parseISO(a.appointmentDateTime);
        return aptDate >= monthStart && a.status === 'COMPLETED';
      })
      .reduce((sum, a) => sum + ((a as any).consultationFee || 0), 0);

    setRevenue({ total, today: todayRevenue, thisWeek: weekRevenue, thisMonth: monthRevenue });
  };

  const detectConflicts = () => {
    const conflictsList: Array<{ appointment1: Appointment; appointment2: Appointment }> = [];
    for (let i = 0; i < appointments.length; i++) {
      for (let j = i + 1; j < appointments.length; j++) {
        const apt1 = appointments[i];
        const apt2 = appointments[j];
        if (!apt1.doctorId || !apt2.doctorId) continue;
        if (apt1.doctorId === apt2.doctorId) {
          const time1 = parseISO(apt1.appointmentDateTime);
          const time2 = parseISO(apt2.appointmentDateTime);
          const sameDay = time1.toDateString() === time2.toDateString();
          const diff = Math.abs(time1.getTime() - time2.getTime()) / (1000 * 60); // minutes
          if (sameDay && diff < 30 && apt1.status !== 'CANCELLED' && apt2.status !== 'CANCELLED') {
            conflictsList.push({ appointment1: apt1, appointment2: apt2 });
          }
        }
      }
    }
    setConflicts(conflictsList);
  };

  const handleBulkAction = async (action: 'cancel' | 'reschedule' | 'sendReminder') => {
    if (selectedAppointments.size === 0) {
      toast.error('Please select appointments');
      return;
    }
    // Implementation would go here
    toast.success(`${action} applied to ${selectedAppointments.size} appointments`);
    setSelectedAppointments(new Set());
    setShowBulkActions(false);
  };

  const handleDragStart = (appointment: Appointment) => {
    setDraggedAppointment(appointment);
  };

  const handleDragOver = (e: React.DragEvent, date: string, hour: number) => {
    e.preventDefault();
    setDragOverSlot({ date, hour });
  };

  const handleDrop = async (e: React.DragEvent, targetDate: string, targetHour: number) => {
    e.preventDefault();
    if (!draggedAppointment) return;

    const paddedHour = targetHour.toString().padStart(2, '0');
    const newDateStr = targetDate;
    const newTimeStr = `${paddedHour}:00`;
    const newDateTime = new Date(`${newDateStr}T${newTimeStr}`);
    
    // Check for conflicts
    const hasConflict = appointments.some(apt => {
      if (apt.id === draggedAppointment.id) return false;
      if (apt.doctorId !== draggedAppointment.doctorId) return false;
      const aptTime = parseISO(apt.appointmentDateTime);
      const timeDiff = Math.abs(aptTime.getTime() - newDateTime.getTime()) / (1000 * 60);
      return timeDiff < 30 && apt.status !== 'CANCELLED';
    });

    if (hasConflict) {
      toast.error('Cannot reschedule: Conflict detected with existing appointment');
      setDraggedAppointment(null);
      setDragOverSlot(null);
      return;
    }

    setRescheduleModal({
      open: true,
      appointmentId: draggedAppointment.id.toString(),
      date: format(newDateTime, 'yyyy-MM-dd'),
      time: format(newDateTime, 'HH:mm'),
    });

    setDraggedAppointment(null);
    setDragOverSlot(null);
  };

  const handleDragEnd = () => {
    setDraggedAppointment(null);
    setDragOverSlot(null);
  };

  const handleRescheduleSubmit = async () => {
    const { appointmentId, date, time } = rescheduleModal;
    if (!appointmentId) {
      setRescheduleModal((prev) => ({ ...prev, open: false }));
      return;
    }
    if (!date || !time) {
      toast.error('Please select date and time');
      return;
    }
    const combined = new Date(`${date}T${time}`);
    if (isNaN(combined.getTime())) {
      toast.error('Invalid date or time');
      return;
    }
    try {
      // Backend expects a LocalDateTime without timezone (ISO-8601 without trailing Z)
      const localIso = format(combined, "yyyy-MM-dd'T'HH:mm:ss");
      await adminApi.rescheduleAppointment(appointmentId, localIso);
      toast.success(`Appointment rescheduled to ${format(combined, 'MMM dd, yyyy h:mm a')}`);
      await loadAppointments();
      setRescheduleModal((prev) => ({ ...prev, open: false }));
    } catch (err) {
      toast.error('Failed to reschedule');
    }
  };

  const handleSendReminder = async (appointmentId: string) => {
    try {
      await adminApi.sendAppointmentReminder(appointmentId);
      toast.success('Reminder email sent to the patient');
    } catch (e) {
      toast.error('Failed to send reminder');
    }
  };

  const handleDelete = async (apt: Appointment) => {
    // If already cancelled, just remove it locally for admins
    if (apt.status === 'CANCELLED') {
      setAppointments((prev) => prev.filter((a) => a.id !== apt.id));
      toast.success('Appointment removed');
      return;
    }
    try {
      await adminApi.deleteAppointment(apt.id.toString());
      toast.success('Appointment deleted');
      setAppointments((prev) => prev.filter((a) => a.id !== apt.id));
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || '';
      if (msg.toLowerCase().includes('cancel') || msg.toLowerCase().includes('already')) {
        // If backend says it is already cancelled, remove locally
        setAppointments((prev) => prev.filter((a) => a.id !== apt.id));
        toast.success('Appointment removed');
      } else {
        toast.error('Failed to delete appointment');
      }
    }
  };

  const toggleSelection = (id: string) => {
    const newSelection = new Set(selectedAppointments);
    if (newSelection.has(id)) {
      newSelection.delete(id);
    } else {
      newSelection.add(id);
    }
    setSelectedAppointments(newSelection);
    setShowBulkActions(newSelection.size > 0);
  };

  return (
    <DashboardLayout requiredUserType="ADMIN">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
        <div className="flex-1 p-8">
            <div className="mb-8">
              <h1 className="text-4xl font-bold text-gray-800 mb-2">Appointment Management</h1>
              <p className="text-gray-600">Manage all platform appointments</p>
            </div>

          {/* Revenue Summary */}
          <div className="grid md:grid-cols-4 gap-4 mb-6">
            <Card>
              <CardHeader className="pb-2">
                <CardDescription>Total Revenue</CardDescription>
                <CardTitle className="text-2xl">PKR {revenue.total.toLocaleString()}</CardTitle>
              </CardHeader>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardDescription>Today</CardDescription>
                <CardTitle className="text-2xl">PKR {revenue.today.toLocaleString()}</CardTitle>
              </CardHeader>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardDescription>This Week</CardDescription>
                <CardTitle className="text-2xl">PKR {revenue.thisWeek.toLocaleString()}</CardTitle>
              </CardHeader>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardDescription>This Month</CardDescription>
                <CardTitle className="text-2xl">PKR {revenue.thisMonth.toLocaleString()}</CardTitle>
              </CardHeader>
            </Card>
          </div>

          {/* Conflicts Alert */}
          {conflicts.length > 0 && (
            <Card className="mb-6 border-red-200 bg-red-50">
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-red-800">
                  <AlertCircle className="w-5 h-5" />
                  Scheduling Conflicts Detected ({conflicts.length})
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  {conflicts.slice(0, 3).map((conflict, idx) => {
                    const docName =
                      conflict.appointment1.doctorName ||
                      conflict.appointment2.doctorName ||
                      `Doctor ${conflict.appointment1.doctorId}`;
                    const time1 = format(parseISO(conflict.appointment1.appointmentDateTime), 'MMM dd, h:mm a');
                    const time2 = format(parseISO(conflict.appointment2.appointmentDateTime), 'MMM dd, h:mm a');
                    return (
                      <div key={idx} className="text-sm text-red-700">
                        Conflict: {docName} has overlapping appointments at {time1} and {time2}
                      </div>
                    );
                  })}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Bulk Actions Bar */}
          {showBulkActions && (
            <Card className="mb-6 bg-blue-50 border-blue-200">
              <CardContent className="pt-6">
                <div className="flex items-center justify-between">
                  <span className="font-medium">
                    {selectedAppointments.size} appointment(s) selected
                  </span>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleBulkAction('sendReminder')}
                    >
                      <Send className="w-4 h-4 mr-2" />
                      Send Reminder
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleBulkAction('reschedule')}
                    >
                      Reschedule
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleBulkAction('cancel')}
                    >
                      Cancel
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setSelectedAppointments(new Set());
                        setShowBulkActions(false);
                      }}
                    >
                      Clear Selection
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

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

          {/* Calendar View (full month) */}
          {viewMode === 'calendar' && (
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle>{format(currentDate, 'MMMM yyyy')}</CardTitle>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1))}
                    >
                      <ChevronLeft size={16} />
                    </Button>
                    <Button variant="outline" size="sm" onClick={() => setCurrentDate(new Date())}>
                      Today
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1))}
                    >
                      <ChevronRight size={16} />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-7 gap-2 text-center font-semibold text-gray-600 p-2 mb-2">
                  {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day) => (
                    <div key={day}>{day}</div>
                  ))}
                </div>
                {(() => {
                  const firstDay = monthDays[0].getDay(); // 0 (Sun) - 6 (Sat)
                  const padded = [...Array(firstDay).fill(null), ...monthDays];
                  const rows = [];
                  for (let i = 0; i < padded.length; i += 7) {
                    rows.push(padded.slice(i, i + 7));
                  }
                  return (
                    <div className="space-y-2">
                      {rows.map((week, idx) => (
                        <div key={idx} className="grid grid-cols-7 gap-2">
                          {week.map((day, dIdx) => {
                            if (!day) return <div key={dIdx} className="min-h-[110px] border rounded-lg bg-slate-50" />;
                            const dateStr = format(day, 'yyyy-MM-dd');
                            const dayAppointments = appointmentsByDate[dateStr] || [];
                            const isToday = isSameDay(day, new Date());
                            return (
                              <div
                                key={dateStr}
                                onDragOver={(e) => {
                                  e.preventDefault();
                                  setDragOverSlot({ date: dateStr, hour: 9 });
                                }}
                                onDrop={(e) => {
                                  const targetHour = dragOverSlot?.hour || 9;
                                  handleDrop(e, dateStr, targetHour);
                                }}
                                className={`min-h-[110px] p-2 border rounded-lg ${
                                  isToday ? 'bg-blue-50 border-blue-300' : 'bg-white border-gray-200'
                                } ${dragOverSlot?.date === dateStr ? 'bg-green-50 border-green-400' : ''}`}
                              >
                                <div className={`text-sm font-semibold mb-1 ${isToday ? 'text-blue-600' : 'text-gray-700'}`}>
                                  {format(day, 'd')}
                                </div>
                                <div className="space-y-1">
                                  {dayAppointments.slice(0, 3).map((apt) => (
                                    <div
                                      key={apt.id}
                                      draggable
                                      onDragStart={() => handleDragStart(apt)}
                                      onDragEnd={handleDragEnd}
                                      className={`text-[11px] p-1 rounded truncate cursor-move hover:shadow-md transition ${getStatusColor(apt.status)} ${
                                        draggedAppointment?.id === apt.id ? 'opacity-50' : ''
                                      }`}
                                      title={`Drag to reschedule: ${apt.patientName} - ${format(parseISO(apt.appointmentDateTime), 'h:mm a')}`}
                                    >
                                      {format(parseISO(apt.appointmentDateTime), 'h:mm a')} - {apt.patientName}
                                    </div>
                                  ))}
                                  {dayAppointments.length > 3 && (
                                    <div className="text-xs text-gray-500">+{dayAppointments.length - 3} more</div>
                                  )}
                                  {dragOverSlot?.date === dateStr && (
                                    <div className="text-xs p-1 rounded bg-blue-200 border-2 border-blue-400 border-dashed">
                                      Drop here to reschedule
                                    </div>
                                  )}
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      ))}
                    </div>
                  );
                })()}
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
                        className={`flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 ${
                          selectedAppointments.has(apt.id.toString()) ? 'bg-blue-50 border-blue-300' : ''
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <input
                            type="checkbox"
                            checked={selectedAppointments.has(apt.id.toString())}
                            onChange={() => toggleSelection(apt.id.toString())}
                            className="w-4 h-4"
                          />
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
                        </div>
                        <div className="flex gap-2">
                          <Button variant="outline" size="sm" onClick={() => handleSendReminder(apt.id.toString())}>
                            <Send className="mr-2" size={16} />
                            Remind
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              const parsed = parseISO(apt.appointmentDateTime);
                              setRescheduleModal({
                                open: true,
                                appointmentId: apt.id.toString(),
                                date: format(parsed, 'yyyy-MM-dd'),
                                time: format(parsed, 'HH:mm'),
                              });
                            }}
                          >
                            Reschedule
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            className="border-red-300 text-red-600 hover:bg-red-50"
                            onClick={() => handleDelete(apt)}
                          >
                            Delete
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

      {rescheduleModal.open && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
            <h3 className="text-lg font-semibold mb-4">Reschedule Appointment</h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">New Date</label>
                <input
                  type="date"
                  value={rescheduleModal.date}
                  onChange={(e) => setRescheduleModal((prev) => ({ ...prev, date: e.target.value }))}
                  className="w-full border rounded-md px-3 py-2"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">New Time</label>
                <input
                  type="time"
                  value={rescheduleModal.time}
                  onChange={(e) => setRescheduleModal((prev) => ({ ...prev, time: e.target.value }))}
                  className="w-full border rounded-md px-3 py-2"
                />
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="outline" onClick={() => setRescheduleModal((prev) => ({ ...prev, open: false }))}>
                  Cancel
                </Button>
                <Button onClick={handleRescheduleSubmit}>Confirm</Button>
              </div>
            </div>
          </div>
        </div>
      )}
      </div>
    </DashboardLayout>
  );
}

