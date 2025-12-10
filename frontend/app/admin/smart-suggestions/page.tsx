'use client';

import { useState, useEffect } from 'react';
import { toast } from 'react-hot-toast';
import { Lightbulb, AlertTriangle, TrendingUp, Clock, CheckCircle2, XCircle } from 'lucide-react';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { Button } from '@/marketing/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/marketing/ui/card';
import { Badge } from '@/marketing/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/marketing/ui/tabs';

interface Suggestion {
  id: string;
  type: 'license_renewal' | 'anomaly' | 'peak_hours';
  title: string;
  description: string;
  priority: 'high' | 'medium' | 'low';
  actionRequired: boolean;
  metadata: Record<string, any>;
}

export default function SmartSuggestionsPage() {
  const [suggestions, setSuggestions] = useState<Suggestion[]>([
    {
      id: '1',
      type: 'license_renewal',
      title: '3 Doctors Need License Renewal',
      description: 'Dr. Ahmed, Dr. Khan, and Dr. Ali have licenses expiring within 30 days',
      priority: 'high',
      actionRequired: true,
      metadata: { doctorCount: 3, daysUntilExpiry: 25 },
    },
    {
      id: '2',
      type: 'anomaly',
      title: 'Unusual Appointment Cancellation Rate',
      description: 'Cancellation rate increased by 15% this week compared to last month',
      priority: 'high',
      actionRequired: true,
      metadata: { increasePercent: 15, period: 'week' },
    },
    {
      id: '3',
      type: 'peak_hours',
      title: 'Peak Hours Detected',
      description: 'Most appointments are booked between 10 AM - 12 PM. Consider adding more slots.',
      priority: 'medium',
      actionRequired: false,
      metadata: { peakStart: '10:00', peakEnd: '12:00', utilization: 85 },
    },
    {
      id: '4',
      type: 'license_renewal',
      title: '5 Doctors Approaching License Expiry',
      description: 'Several doctors have licenses expiring in the next 60 days',
      priority: 'medium',
      actionRequired: true,
      metadata: { doctorCount: 5, daysUntilExpiry: 45 },
    },
    {
      id: '5',
      type: 'anomaly',
      title: 'Low Doctor Availability',
      description: 'Average doctor availability dropped to 60% this week',
      priority: 'medium',
      actionRequired: false,
      metadata: { availability: 60, period: 'week' },
    },
  ]);

  const [dismissed, setDismissed] = useState<Set<string>>(new Set());
  const [filter, setFilter] = useState<'all' | 'high' | 'action_required'>('all');

  const filteredSuggestions = suggestions.filter(s => {
    if (dismissed.has(s.id)) return false;
    if (filter === 'high') return s.priority === 'high';
    if (filter === 'action_required') return s.actionRequired;
    return true;
  });

  const handleDismiss = (id: string) => {
    setDismissed(new Set([...dismissed, id]));
    toast.success('Suggestion dismissed');
  };

  const handleAction = (id: string) => {
    toast.success('Action taken on suggestion');
    // Implementation would navigate to relevant page or trigger action
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'high':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'medium':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'low':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'license_renewal':
        return <Clock className="w-5 h-5" />;
      case 'anomaly':
        return <AlertTriangle className="w-5 h-5" />;
      case 'peak_hours':
        return <TrendingUp className="w-5 h-5" />;
      default:
        return <Lightbulb className="w-5 h-5" />;
    }
  };

  const sidebarItems = [
    { icon: Lightbulb, label: 'Smart Suggestions', href: '/admin/smart-suggestions' },
  ];

  const highPriorityCount = suggestions.filter(s => s.priority === 'high' && !dismissed.has(s.id)).length;
  const actionRequiredCount = suggestions.filter(s => s.actionRequired && !dismissed.has(s.id)).length;

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav userName="Admin" userRole="Admin" showPortalLinks={false} onLogout={() => {}} />
      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/admin/smart-suggestions" />
        <main className="flex-1 p-4 sm:px-6 lg:px-8">
          <div className="max-w-7xl mx-auto space-y-8">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h1 className="text-3xl text-slate-900 mb-2">Smart Suggestions</h1>
                <p className="text-slate-600">AI-powered insights and recommendations</p>
              </div>
            </div>

            {/* Summary Cards */}
            <div className="grid sm:grid-cols-3 gap-6">
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>High Priority</CardDescription>
                  <CardTitle className="text-2xl text-red-600">{highPriorityCount}</CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Action Required</CardDescription>
                  <CardTitle className="text-2xl text-yellow-600">{actionRequiredCount}</CardTitle>
                </CardHeader>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardDescription>Total Suggestions</CardDescription>
                  <CardTitle className="text-2xl">{filteredSuggestions.length}</CardTitle>
                </CardHeader>
              </Card>
            </div>

            {/* Filters */}
            <Card>
              <CardContent className="pt-6">
                <Tabs value={filter} onValueChange={(v) => setFilter(v as any)}>
                  <TabsList>
                    <TabsTrigger value="all">All Suggestions</TabsTrigger>
                    <TabsTrigger value="high">High Priority</TabsTrigger>
                    <TabsTrigger value="action_required">Action Required</TabsTrigger>
                  </TabsList>
                </Tabs>
              </CardContent>
            </Card>

            {/* Suggestions List */}
            <div className="space-y-4">
              {filteredSuggestions.length === 0 ? (
                <Card>
                  <CardContent className="py-12 text-center text-slate-500">
                    No suggestions available
                  </CardContent>
                </Card>
              ) : (
                filteredSuggestions.map((suggestion) => (
                  <Card key={suggestion.id} className="border-l-4 border-l-teal-500">
                    <CardHeader>
                      <div className="flex items-start justify-between">
                        <div className="flex items-start gap-3">
                          <div className="mt-1">{getTypeIcon(suggestion.type)}</div>
                          <div className="flex-1">
                            <CardTitle className="text-lg">{suggestion.title}</CardTitle>
                            <CardDescription className="mt-1">
                              {suggestion.description}
                            </CardDescription>
                            <div className="flex gap-2 mt-3">
                              <Badge className={getPriorityColor(suggestion.priority)}>
                                {suggestion.priority.toUpperCase()}
                              </Badge>
                              {suggestion.actionRequired && (
                                <Badge variant="outline" className="border-orange-300 text-orange-700">
                                  Action Required
                                </Badge>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    </CardHeader>
                    <CardContent>
                      <div className="flex gap-2">
                        {suggestion.actionRequired && (
                          <Button
                            onClick={() => handleAction(suggestion.id)}
                            size="sm"
                          >
                            Take Action
                          </Button>
                        )}
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleDismiss(suggestion.id)}
                        >
                          <XCircle className="w-4 h-4 mr-2" />
                          Dismiss
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                ))
              )}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

