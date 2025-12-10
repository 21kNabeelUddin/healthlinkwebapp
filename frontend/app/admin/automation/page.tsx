'use client';

import { useState } from 'react';
import { toast } from 'react-hot-toast';
import { Zap, UserCheck, Ban, Bell, Save, Plus, Trash2 } from 'lucide-react';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { Button } from '@/marketing/ui/button';
import { Input } from '@/marketing/ui/input';
import { Label } from '@/marketing/ui/label';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/marketing/ui/card';
import { Switch } from '@/marketing/ui/switch';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/marketing/ui/select';
import { Textarea } from '@/marketing/ui/textarea';

interface AutomationRule {
  id: string;
  name: string;
  type: 'auto_approve_doctor' | 'auto_suspend_user' | 'auto_send_reminder';
  enabled: boolean;
  conditions: Record<string, any>;
  actions: Record<string, any>;
}

export default function AutomationRulesPage() {
  const [rules, setRules] = useState<AutomationRule[]>([
    {
      id: '1',
      name: 'Auto-Approve Experienced Doctors',
      type: 'auto_approve_doctor',
      enabled: true,
      conditions: {
        minYearsExperience: 5,
        requirePMDC: true,
        requireSpecialization: true,
      },
      actions: {
        autoApprove: true,
        sendNotification: true,
      },
    },
    {
      id: '2',
      name: 'Auto-Suspend Inactive Users',
      type: 'auto_suspend_user',
      enabled: false,
      conditions: {
        inactiveDays: 90,
        noAppointments: true,
      },
      actions: {
        suspendAccount: true,
        sendEmail: true,
      },
    },
    {
      id: '3',
      name: 'Appointment Reminders',
      type: 'auto_send_reminder',
      enabled: true,
      conditions: {
        reminderTime: '24',
        reminderType: 'email',
      },
      actions: {
        sendEmail: true,
        sendSMS: false,
      },
    },
  ]);

  const [showNewRule, setShowNewRule] = useState(false);
  const [newRule, setNewRule] = useState<Partial<AutomationRule>>({
    name: '',
    type: 'auto_approve_doctor',
    enabled: true,
    conditions: {},
    actions: {},
  });

  const toggleRule = (id: string) => {
    setRules(rules.map(r => r.id === id ? { ...r, enabled: !r.enabled } : r));
    toast.success('Rule updated');
  };

  const deleteRule = (id: string) => {
    if (confirm('Are you sure you want to delete this rule?')) {
      setRules(rules.filter(r => r.id !== id));
      toast.success('Rule deleted');
    }
  };

  const saveNewRule = () => {
    if (!newRule.name) {
      toast.error('Please enter a rule name');
      return;
    }
    const rule: AutomationRule = {
      id: Date.now().toString(),
      name: newRule.name!,
      type: newRule.type!,
      enabled: newRule.enabled ?? true,
      conditions: newRule.conditions || {},
      actions: newRule.actions || {},
    };
    setRules([...rules, rule]);
    setNewRule({ name: '', type: 'auto_approve_doctor', enabled: true, conditions: {}, actions: {} });
    setShowNewRule(false);
    toast.success('Rule created');
  };

  const sidebarItems = [
    { icon: Zap, label: 'Automation', href: '/admin/automation' },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav userName="Admin" userRole="Admin" showPortalLinks={false} onLogout={() => {}} />
      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/admin/automation" />
        <main className="flex-1 p-4 sm:px-6 lg:px-8">
          <div className="max-w-7xl mx-auto space-y-8">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h1 className="text-3xl text-slate-900 mb-2">Automation Rules</h1>
                <p className="text-slate-600">Configure automated actions and workflows</p>
              </div>
              <Button onClick={() => setShowNewRule(true)}>
                <Plus className="w-4 h-4 mr-2" />
                New Rule
              </Button>
            </div>

            {/* Auto-Approve Doctors */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2">
                      <UserCheck className="w-5 h-5" />
                      Auto-Approve Doctors
                    </CardTitle>
                    <CardDescription>Automatically approve doctors meeting criteria</CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {rules
                  .filter(r => r.type === 'auto_approve_doctor')
                  .map((rule) => (
                    <div key={rule.id} className="border rounded-lg p-4">
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-3">
                          <Switch
                            checked={rule.enabled}
                            onCheckedChange={() => toggleRule(rule.id)}
                          />
                          <Label className="font-medium">{rule.name}</Label>
                        </div>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => deleteRule(rule.id)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                      <div className="grid md:grid-cols-3 gap-4 text-sm">
                        <div>
                          <Label>Min Years Experience</Label>
                          <Input
                            type="number"
                            value={rule.conditions.minYearsExperience || 0}
                            disabled={!rule.enabled}
                          />
                        </div>
                        <div className="flex items-center gap-2 pt-6">
                          <Switch
                            checked={rule.conditions.requirePMDC || false}
                            disabled={!rule.enabled}
                          />
                          <Label>Require PMDC</Label>
                        </div>
                        <div className="flex items-center gap-2 pt-6">
                          <Switch
                            checked={rule.conditions.requireSpecialization || false}
                            disabled={!rule.enabled}
                          />
                          <Label>Require Specialization</Label>
                        </div>
                      </div>
                    </div>
                  ))}
              </CardContent>
            </Card>

            {/* Auto-Suspend Users */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2">
                      <Ban className="w-5 h-5" />
                      Auto-Suspend Users
                    </CardTitle>
                    <CardDescription>Automatically suspend inactive or problematic users</CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {rules
                  .filter(r => r.type === 'auto_suspend_user')
                  .map((rule) => (
                    <div key={rule.id} className="border rounded-lg p-4">
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-3">
                          <Switch
                            checked={rule.enabled}
                            onCheckedChange={() => toggleRule(rule.id)}
                          />
                          <Label className="font-medium">{rule.name}</Label>
                        </div>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => deleteRule(rule.id)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                      <div className="grid md:grid-cols-2 gap-4 text-sm">
                        <div>
                          <Label>Inactive Days</Label>
                          <Input
                            type="number"
                            value={rule.conditions.inactiveDays || 0}
                            disabled={!rule.enabled}
                          />
                        </div>
                        <div className="flex items-center gap-2 pt-6">
                          <Switch
                            checked={rule.conditions.noAppointments || false}
                            disabled={!rule.enabled}
                          />
                          <Label>Require No Appointments</Label>
                        </div>
                      </div>
                    </div>
                  ))}
              </CardContent>
            </Card>

            {/* Auto-Send Reminders */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2">
                      <Bell className="w-5 h-5" />
                      Auto-Send Reminders
                    </CardTitle>
                    <CardDescription>Automatically send appointment reminders</CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {rules
                  .filter(r => r.type === 'auto_send_reminder')
                  .map((rule) => (
                    <div key={rule.id} className="border rounded-lg p-4">
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-3">
                          <Switch
                            checked={rule.enabled}
                            onCheckedChange={() => toggleRule(rule.id)}
                          />
                          <Label className="font-medium">{rule.name}</Label>
                        </div>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => deleteRule(rule.id)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                      <div className="grid md:grid-cols-3 gap-4 text-sm">
                        <div>
                          <Label>Reminder Time (hours before)</Label>
                          <Input
                            type="number"
                            value={rule.conditions.reminderTime || 24}
                            disabled={!rule.enabled}
                          />
                        </div>
                        <div className="flex items-center gap-2 pt-6">
                          <Switch
                            checked={rule.actions.sendEmail || false}
                            disabled={!rule.enabled}
                          />
                          <Label>Send Email</Label>
                        </div>
                        <div className="flex items-center gap-2 pt-6">
                          <Switch
                            checked={rule.actions.sendSMS || false}
                            disabled={!rule.enabled}
                          />
                          <Label>Send SMS</Label>
                        </div>
                      </div>
                    </div>
                  ))}
              </CardContent>
            </Card>

            {/* New Rule Modal */}
            {showNewRule && (
              <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                <Card className="w-full max-w-md">
                  <CardHeader>
                    <CardTitle>Create New Rule</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div>
                      <Label>Rule Name</Label>
                      <Input
                        value={newRule.name}
                        onChange={(e) => setNewRule({ ...newRule, name: e.target.value })}
                        placeholder="Enter rule name"
                      />
                    </div>
                    <div>
                      <Label>Rule Type</Label>
                      <Select
                        value={newRule.type}
                        onValueChange={(v) => setNewRule({ ...newRule, type: v as any })}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="auto_approve_doctor">Auto-Approve Doctor</SelectItem>
                          <SelectItem value="auto_suspend_user">Auto-Suspend User</SelectItem>
                          <SelectItem value="auto_send_reminder">Auto-Send Reminder</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={newRule.enabled ?? true}
                        onCheckedChange={(checked) => setNewRule({ ...newRule, enabled: checked })}
                      />
                      <Label>Enabled</Label>
                    </div>
                    <div className="flex gap-2">
                      <Button onClick={saveNewRule} className="flex-1">
                        <Save className="w-4 h-4 mr-2" />
                        Save
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() => {
                          setShowNewRule(false);
                          setNewRule({ name: '', type: 'auto_approve_doctor', enabled: true, conditions: {}, actions: {} });
                        }}
                      >
                        Cancel
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}

