'use client';

import { useState, useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { toast } from 'react-hot-toast';
import { ArrowLeft, Send, Clock, Users, User, Stethoscope, CheckCircle2, Eye, Filter, Calendar, FileText, Save, Search, X, Plus, BookOpen, GraduationCap, Award, AlertCircle } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { adminApi } from '@/lib/api';
import { User as UserType } from '@/types';
import DashboardLayout from '@/components/layout/DashboardLayout';
import { Button } from '@/marketing/ui/button';
import Input from '@/components/ui/Input';
import { Label } from '@/marketing/ui/label';
import { Textarea } from '@/marketing/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/marketing/ui/select';
import { Checkbox } from '@/marketing/ui/checkbox';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/marketing/ui/card';
import { Badge } from '@/marketing/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/marketing/ui/tabs';

interface FilterCriteria {
  // Doctor-specific filters
  yearsOfExperienceMin?: number;
  yearsOfExperienceMax?: number;
  hasPMDCVerified?: boolean;
  specialization?: string;
  approvalStatus?: 'PENDING' | 'APPROVED' | 'REJECTED' | 'ALL';
  isVerified?: boolean;
  // Common filters
  registrationDateFrom?: string;
  registrationDateTo?: string;
  isActive?: boolean;
  // Patient-specific filters
  hasAppointments?: boolean;
}

interface FilterScenario {
  id: string;
  name: string;
  description: string;
  recipientType: 'SELECTED_DOCTORS' | 'SELECTED_USERS';
  criteria: FilterCriteria;
}

const predefinedScenarios: FilterScenario[] = [
  {
    id: 'recent-grad-doctors',
    name: 'Recent Graduate Doctors (< 2 years)',
    description: 'Doctors with less than 2 years of experience who may need HEC attestation',
    recipientType: 'SELECTED_DOCTORS',
    criteria: {
      yearsOfExperienceMax: 2,
      hasPMDCVerified: true,
    },
  },
  {
    id: 'unverified-doctors',
    name: 'Unverified Doctors',
    description: 'Doctors pending verification or approval',
    recipientType: 'SELECTED_DOCTORS',
    criteria: {
      isVerified: false,
      approvalStatus: 'PENDING',
    },
  },
  {
    id: 'inactive-doctors',
    name: 'Inactive Doctors',
    description: 'Doctors who are not currently active',
    recipientType: 'SELECTED_DOCTORS',
    criteria: {
      isActive: false,
    },
  },
];

export default function NewNotificationPage() {
  const router = useRouter();
  const { user } = useAuth();
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

  // Filter state
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [filterCriteria, setFilterCriteria] = useState<FilterCriteria>({});
  const [searchQuery, setSearchQuery] = useState('');
  const [allDoctors, setAllDoctors] = useState<UserType[]>([]);
  const [allPatients, setAllPatients] = useState<UserType[]>([]);
  const [isLoadingRecipients, setIsLoadingRecipients] = useState(false);
  const [selectedScenario, setSelectedScenario] = useState<string>('');

  useEffect(() => {
    if (formData.recipientType === 'SELECTED_DOCTORS' || formData.recipientType === 'INDIVIDUAL_DOCTOR') {
      loadDoctors();
    } else if (formData.recipientType === 'SELECTED_USERS' || formData.recipientType === 'INDIVIDUAL_USER') {
      loadPatients();
    }
  }, [formData.recipientType]);

  const loadDoctors = async () => {
    setIsLoadingRecipients(true);
    try {
      const doctors = await adminApi.getAllDoctors();
      setAllDoctors(doctors || []);
    } catch (error: any) {
      toast.error('Failed to load doctors');
      console.error('Doctors load error:', error);
    } finally {
      setIsLoadingRecipients(false);
    }
  };

  const loadPatients = async () => {
    setIsLoadingRecipients(true);
    try {
      const patients = await adminApi.getAllPatients();
      setAllPatients(patients || []);
    } catch (error: any) {
      toast.error('Failed to load patients');
      console.error('Patients load error:', error);
    } finally {
      setIsLoadingRecipients(false);
    }
  };

  const applyFilterCriteria = (users: UserType[], criteria: FilterCriteria): UserType[] => {
    let filtered = [...users];

    // Years of experience filter (for doctors)
    if (criteria.yearsOfExperienceMin !== undefined) {
      filtered = filtered.filter(u => (u.yearsOfExperience || 0) >= criteria.yearsOfExperienceMin!);
    }
    if (criteria.yearsOfExperienceMax !== undefined) {
      filtered = filtered.filter(u => (u.yearsOfExperience || 0) <= criteria.yearsOfExperienceMax!);
    }

    // PMDC verification
    if (criteria.hasPMDCVerified !== undefined) {
      filtered = filtered.filter(u => {
        const hasPMDC = !!u.pmdcId && u.pmdcId.trim() !== '';
        return criteria.hasPMDCVerified ? hasPMDC : !hasPMDC;
      });
    }

    // Specialization
    if (criteria.specialization) {
      filtered = filtered.filter(u => 
        u.specialization?.toLowerCase().includes(criteria.specialization!.toLowerCase())
      );
    }

    // Approval status
    if (criteria.approvalStatus && criteria.approvalStatus !== 'ALL') {
      filtered = filtered.filter(u => u.approvalStatus === criteria.approvalStatus);
    }

    // Verification status
    if (criteria.isVerified !== undefined) {
      filtered = filtered.filter(u => u.isVerified === criteria.isVerified);
    }

    // Registration date range
    if (criteria.registrationDateFrom) {
      const fromDate = new Date(criteria.registrationDateFrom);
      filtered = filtered.filter(u => {
        if (!u.createdAt) return false;
        return new Date(u.createdAt) >= fromDate;
      });
    }
    if (criteria.registrationDateTo) {
      const toDate = new Date(criteria.registrationDateTo);
      filtered = filtered.filter(u => {
        if (!u.createdAt) return false;
        return new Date(u.createdAt) <= toDate;
      });
    }

    // Active status
    if (criteria.isActive !== undefined) {
      filtered = filtered.filter(u => (u.isActive ?? true) === criteria.isActive);
    }

    return filtered;
  };

  const filteredRecipients = useMemo(() => {
    const users = formData.recipientType === 'SELECTED_DOCTORS' || formData.recipientType === 'INDIVIDUAL_DOCTOR'
      ? allDoctors
      : allPatients;

    let filtered = applyFilterCriteria(users, filterCriteria);

    // Apply search query
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(u =>
        u.firstName?.toLowerCase().includes(query) ||
        u.lastName?.toLowerCase().includes(query) ||
        u.email?.toLowerCase().includes(query) ||
        u.phoneNumber?.includes(query) ||
        u.specialization?.toLowerCase().includes(query)
      );
    }

    return filtered;
  }, [allDoctors, allPatients, filterCriteria, searchQuery, formData.recipientType]);

  const handleApplyScenario = (scenario: FilterScenario) => {
    setFilterCriteria(scenario.criteria);
    setFormData(prev => ({ ...prev, recipientType: scenario.recipientType }));
    setSelectedScenario(scenario.id);
    setShowAdvancedFilters(true);
    toast.success(`Applied filter: ${scenario.name}`);
  };

  const handleSelectAll = () => {
    const allIds = filteredRecipients.map(u => u.id);
    setFormData(prev => ({
      ...prev,
      recipientIds: allIds,
    }));
  };

  const handleDeselectAll = () => {
    setFormData(prev => ({
      ...prev,
      recipientIds: [],
    }));
  };

  const toggleRecipient = (id: string) => {
    setFormData(prev => ({
      ...prev,
      recipientIds: prev.recipientIds.includes(id)
        ? prev.recipientIds.filter(rid => rid !== id)
        : [...prev.recipientIds, id],
    }));
  };

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

    if ((formData.recipientType === 'SELECTED_USERS' || formData.recipientType === 'SELECTED_DOCTORS' ||
         formData.recipientType === 'INDIVIDUAL_USER' || formData.recipientType === 'INDIVIDUAL_DOCTOR') &&
        formData.recipientIds.length === 0) {
      toast.error('Please select at least one recipient');
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
        payload.recipientIds = formData.recipientIds;
      }

      if (formData.scheduledAt) {
        payload.scheduledAt = formData.scheduledAt;
      }

      await adminApi.sendCustomNotification(payload);
      toast.success(`Notification sent successfully to ${formData.recipientIds.length || 'all'} recipient(s)!`);
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
        return `All users (${allPatients.length})`;
      case 'ALL_DOCTORS':
        return `All doctors (${allDoctors.length})`;
      case 'INDIVIDUAL_USER':
      case 'INDIVIDUAL_DOCTOR':
        return formData.recipientIds.length === 1 ? '1 recipient' : `${formData.recipientIds.length} recipients`;
      case 'SELECTED_USERS':
      case 'SELECTED_DOCTORS':
        return `${formData.recipientIds.length} selected out of ${filteredRecipients.length} filtered`;
      default:
        return '0 recipients';
    }
  };

  return (
    <DashboardLayout requiredUserType="ADMIN">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
        <main className="flex-1 p-4 sm:px-6 lg:px-8 relative">
          <div className="max-w-6xl mx-auto space-y-6 relative">
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
                <p className="text-slate-600 mt-1">Create and send targeted notifications to users or doctors</p>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6 relative">
              {/* Notification Details */}
              <Card className="relative overflow-visible">
                <CardHeader>
                  <CardTitle>Notification Details</CardTitle>
                  <CardDescription>Enter the notification title and message</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4 relative overflow-visible">
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
                    <div className="space-y-2 relative z-10">
                      <Label htmlFor="type">Type</Label>
                      <Select
                        value={formData.notificationType}
                        onValueChange={(value: any) => setFormData(prev => ({ ...prev, notificationType: value }))}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent className="z-[9999]">
                          <SelectItem value="INFO">Info</SelectItem>
                          <SelectItem value="WARNING">Warning</SelectItem>
                          <SelectItem value="ALERT">Alert</SelectItem>
                          <SelectItem value="ANNOUNCEMENT">Announcement</SelectItem>
                          <SelectItem value="SYSTEM_UPDATE">System Update</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-2 relative z-10">
                      <Label htmlFor="priority">Priority</Label>
                      <Select
                        value={formData.priority}
                        onValueChange={(value: any) => setFormData(prev => ({ ...prev, priority: value }))}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent className="z-[9999]">
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

              {/* Recipients */}
              <Card className="relative overflow-visible">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle>Recipients</CardTitle>
                      <CardDescription>Select who should receive this notification</CardDescription>
                    </div>
                    <Badge variant="outline" className="text-sm">
                      {getRecipientCount()}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4 relative overflow-visible">
                  <div className="space-y-2 relative z-10">
                    <Label htmlFor="recipientType">Recipient Type</Label>
                    <Select
                      value={formData.recipientType}
                      onValueChange={(value: any) => {
                        setFormData(prev => ({ ...prev, recipientType: value, recipientIds: [] }));
                        setFilterCriteria({});
                        setSelectedScenario('');
                      }}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="z-[9999]">
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
                            <span>Selected Users (with filters)</span>
                          </div>
                        </SelectItem>
                        <SelectItem value="SELECTED_DOCTORS">
                          <div className="flex items-center gap-2">
                            <Stethoscope className="w-4 h-4" />
                            <span>Selected Doctors (with filters)</span>
                          </div>
                        </SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  {/* Predefined Scenarios */}
                  {(formData.recipientType === 'SELECTED_DOCTORS' || formData.recipientType === 'SELECTED_USERS') && (
                    <div className="space-y-2">
                      <Label>Quick Filter Scenarios</Label>
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
                        {predefinedScenarios
                          .filter(s => s.recipientType === formData.recipientType)
                          .map((scenario) => (
                            <Button
                              key={scenario.id}
                              type="button"
                              variant={selectedScenario === scenario.id ? 'default' : 'outline'}
                              className="h-auto py-3 px-4 flex flex-col items-start gap-1"
                              onClick={() => handleApplyScenario(scenario)}
                            >
                              <div className="flex items-center gap-2 w-full">
                                <GraduationCap className="w-4 h-4" />
                                <span className="font-semibold text-sm">{scenario.name}</span>
                              </div>
                              <span className="text-xs text-left text-slate-500">{scenario.description}</span>
                            </Button>
                          ))}
                      </div>
                    </div>
                  )}

                  {/* Advanced Filters */}
                  {(formData.recipientType === 'SELECTED_DOCTORS' || formData.recipientType === 'SELECTED_USERS') && (
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
                        >
                          <Filter className="w-4 h-4 mr-2" />
                          {showAdvancedFilters ? 'Hide' : 'Show'} Advanced Filters
                        </Button>
                        {showAdvancedFilters && (
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              setFilterCriteria({});
                              setSelectedScenario('');
                            }}
                          >
                            <X className="w-4 h-4 mr-2" />
                            Clear Filters
                          </Button>
                        )}
                      </div>

                      {showAdvancedFilters && (
                        <div className="border rounded-lg p-4 space-y-4 bg-slate-50">
                          {(formData.recipientType === 'SELECTED_DOCTORS') && (
                            <>
                              <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                  <Label>Min Years of Experience</Label>
                                  <Input
                                    type="number"
                                    min="0"
                                    value={filterCriteria.yearsOfExperienceMin || ''}
                                    onChange={(e) => setFilterCriteria(prev => ({
                                      ...prev,
                                      yearsOfExperienceMin: e.target.value ? parseInt(e.target.value) : undefined,
                                    }))}
                                    placeholder="e.g., 0"
                                  />
                                </div>
                                <div className="space-y-2">
                                  <Label>Max Years of Experience</Label>
                                  <Input
                                    type="number"
                                    min="0"
                                    value={filterCriteria.yearsOfExperienceMax || ''}
                                    onChange={(e) => setFilterCriteria(prev => ({
                                      ...prev,
                                      yearsOfExperienceMax: e.target.value ? parseInt(e.target.value) : undefined,
                                    }))}
                                    placeholder="e.g., 2"
                                  />
                                </div>
                              </div>

                              <div className="space-y-2">
                                <Label>Specialization</Label>
                                <Input
                                  value={filterCriteria.specialization || ''}
                                  onChange={(e) => setFilterCriteria(prev => ({
                                    ...prev,
                                    specialization: e.target.value || undefined,
                                  }))}
                                  placeholder="e.g., Cardiology"
                                />
                              </div>

                              <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                  <Label>PMDC Verified</Label>
                                  <Select
                                    value={filterCriteria.hasPMDCVerified === undefined ? 'ALL' : filterCriteria.hasPMDCVerified ? 'YES' : 'NO'}
                                    onValueChange={(value) => setFilterCriteria(prev => ({
                                      ...prev,
                                      hasPMDCVerified: value === 'ALL' ? undefined : value === 'YES',
                                    }))}
                                  >
                                    <SelectTrigger>
                                      <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent className="z-[9999]">
                                      <SelectItem value="ALL">All</SelectItem>
                                      <SelectItem value="YES">Yes</SelectItem>
                                      <SelectItem value="NO">No</SelectItem>
                                    </SelectContent>
                                  </Select>
                                </div>

                                <div className="space-y-2">
                                  <Label>Approval Status</Label>
                                  <Select
                                    value={filterCriteria.approvalStatus || 'ALL'}
                                    onValueChange={(value: any) => setFilterCriteria(prev => ({
                                      ...prev,
                                      approvalStatus: value === 'ALL' ? undefined : value,
                                    }))}
                                  >
                                    <SelectTrigger>
                                      <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent className="z-[9999]">
                                      <SelectItem value="ALL">All</SelectItem>
                                      <SelectItem value="PENDING">Pending</SelectItem>
                                      <SelectItem value="APPROVED">Approved</SelectItem>
                                      <SelectItem value="REJECTED">Rejected</SelectItem>
                                    </SelectContent>
                                  </Select>
                                </div>
                              </div>

                              <div className="space-y-2">
                                <Label>Email Verified</Label>
                                <Select
                                  value={filterCriteria.isVerified === undefined ? 'ALL' : filterCriteria.isVerified ? 'YES' : 'NO'}
                                  onValueChange={(value) => setFilterCriteria(prev => ({
                                    ...prev,
                                    isVerified: value === 'ALL' ? undefined : value === 'YES',
                                  }))}
                                >
                                  <SelectTrigger>
                                    <SelectValue />
                                  </SelectTrigger>
                                  <SelectContent className="z-[9999]">
                                    <SelectItem value="ALL">All</SelectItem>
                                    <SelectItem value="YES">Yes</SelectItem>
                                    <SelectItem value="NO">No</SelectItem>
                                  </SelectContent>
                                </Select>
                              </div>
                            </>
                          )}

                          <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                              <Label>Registration Date From</Label>
                              <Input
                                type="date"
                                value={filterCriteria.registrationDateFrom || ''}
                                onChange={(e) => setFilterCriteria(prev => ({
                                  ...prev,
                                  registrationDateFrom: e.target.value || undefined,
                                }))}
                              />
                            </div>
                            <div className="space-y-2">
                              <Label>Registration Date To</Label>
                              <Input
                                type="date"
                                value={filterCriteria.registrationDateTo || ''}
                                onChange={(e) => setFilterCriteria(prev => ({
                                  ...prev,
                                  registrationDateTo: e.target.value || undefined,
                                }))}
                              />
                            </div>
                          </div>

                          <div className="space-y-2">
                            <Label>Active Status</Label>
                            <Select
                              value={filterCriteria.isActive === undefined ? 'ALL' : filterCriteria.isActive ? 'ACTIVE' : 'INACTIVE'}
                              onValueChange={(value) => setFilterCriteria(prev => ({
                                ...prev,
                                isActive: value === 'ALL' ? undefined : value === 'ACTIVE',
                              }))}
                            >
                              <SelectTrigger>
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent className="z-[9999]">
                                <SelectItem value="ALL">All</SelectItem>
                                <SelectItem value="ACTIVE">Active</SelectItem>
                                <SelectItem value="INACTIVE">Inactive</SelectItem>
                              </SelectContent>
                            </Select>
                          </div>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Recipient Selection */}
                  {(formData.recipientType === 'SELECTED_USERS' || formData.recipientType === 'SELECTED_DOCTORS' ||
                    formData.recipientType === 'INDIVIDUAL_USER' || formData.recipientType === 'INDIVIDUAL_DOCTOR') && (
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <Label>Select Recipients</Label>
                        <div className="flex gap-2">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={handleSelectAll}
                          >
                            Select All ({filteredRecipients.length})
                          </Button>
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={handleDeselectAll}
                          >
                            Deselect All
                          </Button>
                        </div>
                      </div>

                      <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                        <Input
                          placeholder="Search by name, email, phone, or specialization..."
                          value={searchQuery}
                          onChange={(e) => setSearchQuery(e.target.value)}
                          className="pl-10"
                        />
                      </div>

                      {isLoadingRecipients ? (
                        <div className="border rounded-lg p-8 text-center text-slate-500">
                          Loading recipients...
                        </div>
                      ) : (
                        <div className="border rounded-lg p-4 max-h-96 overflow-y-auto bg-white">
                          {filteredRecipients.length === 0 ? (
                            <div className="text-center py-8 text-slate-500">
                              No recipients found matching your criteria
                            </div>
                          ) : (
                            <div className="space-y-2">
                              {filteredRecipients.map((recipient) => (
                                <div
                                  key={recipient.id}
                                  className="flex items-center gap-3 p-2 rounded hover:bg-slate-50"
                                >
                                  <Checkbox
                                    checked={formData.recipientIds.includes(recipient.id)}
                                    onCheckedChange={(checked) => {
                                      if (checked) {
                                        setFormData(prev => ({
                                          ...prev,
                                          recipientIds: [...prev.recipientIds, recipient.id],
                                        }));
                                      } else {
                                        setFormData(prev => ({
                                          ...prev,
                                          recipientIds: prev.recipientIds.filter(id => id !== recipient.id),
                                        }));
                                      }
                                    }}
                                  />
                                  <div className="flex-1 min-w-0">
                                    <div className="font-medium text-sm">
                                      {recipient.firstName} {recipient.lastName}
                                    </div>
                                    <div className="text-xs text-slate-500 truncate">
                                      {recipient.email}
                                      {recipient.specialization && ` • ${recipient.specialization}`}
                                      {recipient.yearsOfExperience !== undefined && ` • ${recipient.yearsOfExperience} years exp.`}
                                    </div>
                                  </div>
                                  {recipient.pmdcId && (
                                    <Badge variant="outline" className="text-xs">
                                      PMDC: {recipient.pmdcId}
                                    </Badge>
                                  )}
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      )}

                      <div className="text-sm text-slate-600">
                        {formData.recipientIds.length} of {filteredRecipients.length} recipients selected
                      </div>
                    </div>
                  )}

                  {/* Individual recipient input */}
                  {(formData.recipientType === 'INDIVIDUAL_USER' || formData.recipientType === 'INDIVIDUAL_DOCTOR') && (
                    <div className="p-4 bg-slate-50 rounded-lg">
                      <Label className="mb-2 block">Enter Recipient ID(s)</Label>
                      <Input
                        placeholder="Enter recipient ID(s), separated by commas"
                        value={formData.recipientIds.join(', ')}
                        onChange={(e) => setFormData(prev => ({
                          ...prev,
                          recipientIds: e.target.value.split(',').map(id => id.trim()).filter(id => id),
                        }))}
                      />
                      <p className="text-xs text-slate-500 mt-2">
                        You can enter multiple IDs separated by commas
                      </p>
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Delivery Channels */}
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

              {/* Schedule */}
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

              {/* Submit */}
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
                      Send Notification ({formData.recipientIds.length || 'All'})
                    </>
                  )}
                </Button>
              </div>
            </form>
          </div>
        </main>
      </div>
    </DashboardLayout>
  );
}
