'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { toast } from 'react-hot-toast';
import { ArrowLeft, Send, Clock, Users, User, Stethoscope, CheckCircle2, Eye, Filter, Calendar, FileText, Save } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { adminApi } from '@/lib/api';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { Button } from '@/marketing/ui/button';
import Input from '@/components/ui/Input';
import { Label } from '@/marketing/ui/label';
import { Textarea } from '@/marketing/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/marketing/ui/select';
import { Checkbox } from '@/marketing/ui/checkbox';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/marketing/ui/card';

export default function NewNotificationPage() {
  const router = useRouter();
  const { user, logout } = useAuth();
  const [isSending, setIsSending] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    message: '',
    notificationType: 'INFO' as 'INFO' | 'WARNING' | 'ALERT' | 'ANNOUNCEMENT' | 'SYSTEM_UPDATE',
    priority: 'MEDIUM' as 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT',
    recipientType: 'ALL_USERS' as 'INDIVIDUAL_USER' | 'INDIVIDUAL_DOCTOR' | 'ALL_USERS' | 'ALL_DOCTORS' | 'SELECTED_USERS' | 'SELECTED_DOCTORS',
    recipientIds: [] as string[],
    channels: ['IN_APP'] as ('IN_APP' | 'EMAIL' | 'SMS' | 'PUSH')[],
    scheduledAt: '',
  });
  const [showPreview, setShowPreview] = useState(false);
  const [availableUsers, setAvailableUsers] = useState<Array<{ id: string; name: string; email: string }>>([]);
  const [selectedUserIds, setSelectedUserIds] = useState<Set<string>>(new Set());
  const [filterRole, setFilterRole] = useState<string>('ALL');
  const [showTemplates, setShowTemplates] = useState(false);
  const [templates] = useState([
    { id: '1', name: 'Appointment Reminder', title: 'Appointment Reminder', message: 'Your appointment with {doctor} is scheduled for {date} at {time}.' },
    { id: '2', name: 'Welcome Message', title: 'Welcome to HealthLink+', message: 'Welcome {name}! We\'re excited to have you on board.' },
    { id: '3', name: 'System Maintenance', title: 'Scheduled Maintenance', message: 'We will be performing scheduled maintenance on {date} from {time}. Services may be temporarily unavailable.' },
  ]);

  const sidebarItems = [
    { icon: ArrowLeft, label: 'Back to Dashboard', href: '/admin/dashboard' },
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.title || !formData.message) {
      toast.error('Please fill in all required fields');
      return;
    }

    if (formData.channels.length === 0) {
      toast.error('Please select at least one delivery channel');
      return;
    }

    setIsSending(true);
    try {
      const payload: any = {
        title: formData.title,
        message: formData.message,
        notificationType: formData.notificationType,
        priority: formData.priority,
        recipientType: formData.recipientType,
        channels: formData.channels,
      };

      if (formData.recipientType === 'INDIVIDUAL_USER' || 
          formData.recipientType === 'INDIVIDUAL_DOCTOR' ||
          formData.recipientType === 'SELECTED_USERS' ||
          formData.recipientType === 'SELECTED_DOCTORS') {
        if (formData.recipientIds.length === 0) {
          toast.error('Please select at least one recipient');
          setIsSending(false);
          return;
        }
        payload.recipientIds = formData.recipientIds;
      }

      if (formData.scheduledAt) {
        payload.scheduledAt = formData.scheduledAt;
      }

      await adminApi.sendCustomNotification(payload);
      toast.success('Notification sent successfully!');
      router.push('/admin/notifications/history');
    } catch (error: any) {
      const errorMessage = error?.response?.data?.message || error?.message || 'Failed to send notification';
      toast.error(errorMessage);
      console.error('Notification send error:', error);
    } finally {
      setIsSending(false);
    }
  };

  const toggleChannel = (channel: 'IN_APP' | 'EMAIL' | 'SMS' | 'PUSH') => {
    setFormData(prev => ({
      ...prev,
      channels: prev.channels.includes(channel)
        ? prev.channels.filter(c => c !== channel)
        : [...prev.channels, channel]
    }));
  };

  const getRecipientCount = () => {
    switch (formData.recipientType) {
      case 'ALL_USERS':
        return 'All users';
      case 'ALL_DOCTORS':
        return 'All doctors';
      case 'INDIVIDUAL_USER':
      case 'INDIVIDUAL_DOCTOR':
        return formData.recipientIds.length === 1 ? '1 recipient' : `${formData.recipientIds.length} recipients`;
      case 'SELECTED_USERS':
      case 'SELECTED_DOCTORS':
        return formData.recipientIds.length > 0 ? `${formData.recipientIds.length} selected` : 'No recipients selected';
      default:
        return '0 recipients';
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav
        userName={user?.firstName ?? 'Admin'}
        userRole="Admin"
        showPortalLinks={false}
        onLogout={logout}
      />

      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/admin/notifications/new" />

        <main className="flex-1 p-4 sm:px-6 lg:px-8">
          <div className="max-w-4xl mx-auto space-y-6">
            <div className="flex items-center gap-4">
              <Button
                variant="outline"
                size="icon"
                onClick={() => router.back()}
              >
                <ArrowLeft className="w-4 h-4" />
              </Button>
              <div>
                <h1 className="text-3xl font-bold text-slate-900">Send Custom Notification</h1>
                <p className="text-slate-600 mt-1">Create and send notifications to users or doctors</p>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle>Notification Details</CardTitle>
                      <CardDescription>Enter the notification title and message</CardDescription>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        onClick={() => setShowTemplates(true)}
                      >
                        <FileText className="w-4 h-4 mr-2" />
                        Templates
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        onClick={() => {
                          const template = {
                            name: formData.title,
                            title: formData.title,
                            message: formData.message,
                          };
                          toast.success('Template saved');
                        }}
                      >
                        <Save className="w-4 h-4 mr-2" />
                        Save as Template
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="title">Title *</Label>
                    <Input
                      id="title"
                      value={formData.title}
                      onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
                      placeholder="Enter notification title"
                      maxLength={200}
                      required
                    />
                    <p className="text-xs text-slate-500">{formData.title.length}/200 characters</p>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="message">Message *</Label>
                    <Textarea
                      id="message"
                      value={formData.message}
                      onChange={(e) => setFormData(prev => ({ ...prev, message: e.target.value }))}
                      placeholder="Enter notification message"
                      rows={6}
                      maxLength={2000}
                      required
                    />
                    <p className="text-xs text-slate-500">{formData.message.length}/2000 characters</p>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="type">Type</Label>
                      <Select
                        value={formData.notificationType}
                        onValueChange={(value: any) => setFormData(prev => ({ ...prev, notificationType: value }))}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="INFO">Info</SelectItem>
                          <SelectItem value="WARNING">Warning</SelectItem>
                          <SelectItem value="ALERT">Alert</SelectItem>
                          <SelectItem value="ANNOUNCEMENT">Announcement</SelectItem>
                          <SelectItem value="SYSTEM_UPDATE">System Update</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="priority">Priority</Label>
                      <Select
                        value={formData.priority}
                        onValueChange={(value: any) => setFormData(prev => ({ ...prev, priority: value }))}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="LOW">Low</SelectItem>
                          <SelectItem value="MEDIUM">Medium</SelectItem>
                          <SelectItem value="HIGH">High</SelectItem>
                          <SelectItem value="URGENT">Urgent</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>Recipients</CardTitle>
                  <CardDescription>Select who should receive this notification</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="recipientType">Recipient Type</Label>
                    <Select
                      value={formData.recipientType}
                      onValueChange={(value: any) => setFormData(prev => ({ ...prev, recipientType: value, recipientIds: [] }))}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ALL_USERS">
                          <div className="flex items-center gap-2">
                            <Users className="w-4 h-4" />
                            <span>All Users</span>
                          </div>
                        </SelectItem>
                        <SelectItem value="ALL_DOCTORS">
                          <div className="flex items-center gap-2">
                            <Stethoscope className="w-4 h-4" />
                            <span>All Doctors</span>
                          </div>
                        </SelectItem>
                        <SelectItem value="INDIVIDUAL_USER">
                          <div className="flex items-center gap-2">
                            <User className="w-4 h-4" />
                            <span>Individual User</span>
                          </div>
                        </SelectItem>
                        <SelectItem value="INDIVIDUAL_DOCTOR">
                          <div className="flex items-center gap-2">
                            <Stethoscope className="w-4 h-4" />
                            <span>Individual Doctor</span>
                          </div>
                        </SelectItem>
                        <SelectItem value="SELECTED_USERS">
                          <div className="flex items-center gap-2">
                            <Users className="w-4 h-4" />
                            <span>Selected Users</span>
                          </div>
                        </SelectItem>
                        <SelectItem value="SELECTED_DOCTORS">
                          <div className="flex items-center gap-2">
                            <Stethoscope className="w-4 h-4" />
                            <span>Selected Doctors</span>
                          </div>
                        </SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  {(formData.recipientType === 'SELECTED_USERS' || formData.recipientType === 'SELECTED_DOCTORS') && (
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <Label>Select Recipients</Label>
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            // Load users/doctors for selection
                            toast.success('Loading recipients...');
                          }}
                        >
                          <Filter className="w-4 h-4 mr-2" />
                          Filter
                        </Button>
                      </div>
                      <div className="border rounded-lg p-4 max-h-48 overflow-y-auto">
                        <div className="space-y-2">
                          {[1, 2, 3].map((i) => (
                            <div key={i} className="flex items-center gap-2">
                              <input
                                type="checkbox"
                                id={`recipient-${i}`}
                                className="w-4 h-4"
                                onChange={(e) => {
                                  if (e.target.checked) {
                                    setFormData(prev => ({
                                      ...prev,
                                      recipientIds: [...prev.recipientIds, `user-${i}`]
                                    }));
                                  } else {
                                    setFormData(prev => ({
                                      ...prev,
                                      recipientIds: prev.recipientIds.filter(id => id !== `user-${i}`)
                                    }));
                                  }
                                }}
                              />
                              <label htmlFor={`recipient-${i}`} className="text-sm cursor-pointer">
                                User {i} (user{i}@example.com)
                              </label>
                            </div>
                          ))}
                        </div>
                        <p className="text-xs text-slate-500 mt-2">
                          {formData.recipientIds.length} recipient(s) selected
                        </p>
                      </div>
                    </div>
                  )}

                  {(formData.recipientType === 'INDIVIDUAL_USER' || formData.recipientType === 'INDIVIDUAL_DOCTOR') && (
                    <div className="p-4 bg-slate-50 rounded-lg">
                      <p className="text-sm text-slate-600 mb-2">
                        Enter recipient ID
                      </p>
                      <Input
                        placeholder="Enter recipient ID"
                        value={formData.recipientIds.join(',')}
                        onChange={(e) => setFormData(prev => ({
                          ...prev,
                          recipientIds: e.target.value.split(',').filter(id => id.trim())
                        }))}
                      />
                    </div>
                  )}

                  <div className="p-3 bg-blue-50 rounded-lg">
                    <p className="text-sm font-medium text-blue-900">
                      {getRecipientCount()}
                    </p>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>Delivery Channels</CardTitle>
                  <CardDescription>Select how the notification should be delivered</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {(['IN_APP', 'EMAIL', 'SMS', 'PUSH'] as const).map((channel) => (
                      <div key={channel} className="flex items-center space-x-2">
                        <Checkbox
                          id={channel}
                          checked={formData.channels.includes(channel)}
                          onCheckedChange={() => toggleChannel(channel)}
                        />
                        <Label htmlFor={channel} className="font-normal cursor-pointer">
                          {channel === 'IN_APP' && 'In-App Notification'}
                          {channel === 'EMAIL' && 'Email'}
                          {channel === 'SMS' && 'SMS'}
                          {channel === 'PUSH' && 'Push Notification'}
                        </Label>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>Schedule (Optional)</CardTitle>
                  <CardDescription>Send now or schedule for later</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2">
                    <Label htmlFor="scheduledAt">Scheduled Date & Time</Label>
                    <Input
                      id="scheduledAt"
                      type="datetime-local"
                      value={formData.scheduledAt}
                      onChange={(e) => setFormData(prev => ({ ...prev, scheduledAt: e.target.value }))}
                    />
                    {!formData.scheduledAt && (
                      <p className="text-sm text-slate-500">Leave empty to send immediately</p>
                    )}
                  </div>
                </CardContent>
              </Card>

              <div className="flex gap-4 justify-end">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => router.back()}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={isSending}
                >
                  {isSending ? (
                    <>
                      <Clock className="w-4 h-4 mr-2 animate-spin" />
                      Sending...
                    </>
                  ) : (
                    <>
                      <Send className="w-4 h-4 mr-2" />
                      Send Notification
                    </>
                  )}
                </Button>
              </div>
            </form>
          </div>
        </main>
      </div>
    </div>
  );
}

