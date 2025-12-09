'use client';

import { useState } from 'react';
import { toast } from 'react-hot-toast';
import { Settings, Globe, Shield, Mail, Bell, Zap, FileCheck, Save } from 'lucide-react';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { Button } from '@/marketing/ui/button';
import { Input } from '@/marketing/ui/input';
import { Label } from '@/marketing/ui/label';
import { Textarea } from '@/marketing/ui/textarea';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/marketing/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/marketing/ui/tabs';
import { Switch } from '@/marketing/ui/switch';

export default function AdminSettingsPage() {
  const [settings, setSettings] = useState({
    general: {
      platformName: 'HealthLink+',
      logo: '',
      timezone: 'Asia/Karachi',
      language: 'en',
    },
    security: {
      passwordMinLength: 8,
      requireUppercase: true,
      requireLowercase: true,
      requireNumbers: true,
      requireSpecialChars: true,
      twoFactorRequired: false,
      sessionTimeout: 1440, // minutes
    },
    integrations: {
      emailProvider: 'Gmail',
      smsProvider: 'Twilio',
      paymentGateway: 'EasyPaisa',
    },
    notifications: {
      emailEnabled: true,
      smsEnabled: false,
      pushEnabled: true,
    },
    features: {
      telemedicine: true,
      prescriptions: true,
      payments: true,
      analytics: true,
    },
    compliance: {
      hipaaEnabled: true,
      dataRetentionDays: 2555, // 7 years
      auditLogRetentionDays: 365,
    },
  });

  const handleSave = async (section: string) => {
    toast.success(`${section} settings saved successfully`);
  };

  const sidebarItems = [
    { icon: Settings, label: 'Settings', href: '/admin/settings' },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav userName="Admin" userRole="Admin" showPortalLinks={false} onLogout={() => {}} />
      <div className="flex">
        <Sidebar items={sidebarItems} currentPath="/admin/settings" />
        <div className="flex-1 p-8">
          <div className="mb-8">
            <h1 className="text-4xl font-bold text-gray-800 mb-2">System Settings</h1>
            <p className="text-gray-600">Configure platform settings and preferences</p>
          </div>

          <Tabs defaultValue="general" className="space-y-6">
            <TabsList>
              <TabsTrigger value="general">
                <Globe className="mr-2" size={16} />
                General
              </TabsTrigger>
              <TabsTrigger value="security">
                <Shield className="mr-2" size={16} />
                Security
              </TabsTrigger>
              <TabsTrigger value="integrations">
                <Zap className="mr-2" size={16} />
                Integrations
              </TabsTrigger>
              <TabsTrigger value="notifications">
                <Bell className="mr-2" size={16} />
                Notifications
              </TabsTrigger>
              <TabsTrigger value="features">
                <Zap className="mr-2" size={16} />
                Features
              </TabsTrigger>
              <TabsTrigger value="compliance">
                <FileCheck className="mr-2" size={16} />
                Compliance
              </TabsTrigger>
            </TabsList>

            {/* General Settings */}
            <TabsContent value="general">
              <Card>
                <CardHeader>
                  <CardTitle>General Settings</CardTitle>
                  <CardDescription>Platform name, logo, timezone, and language</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <Label htmlFor="platformName">Platform Name</Label>
                    <Input
                      id="platformName"
                      value={settings.general.platformName}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          general: { ...settings.general, platformName: e.target.value },
                        })
                      }
                    />
                  </div>
                  <div>
                    <Label htmlFor="timezone">Timezone</Label>
                    <Input
                      id="timezone"
                      value={settings.general.timezone}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          general: { ...settings.general, timezone: e.target.value },
                        })
                      }
                    />
                  </div>
                  <div>
                    <Label htmlFor="language">Language</Label>
                    <Input
                      id="language"
                      value={settings.general.language}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          general: { ...settings.general, language: e.target.value },
                        })
                      }
                    />
                  </div>
                  <Button onClick={() => handleSave('General')}>
                    <Save className="mr-2" size={16} />
                    Save General Settings
                  </Button>
                </CardContent>
              </Card>
            </TabsContent>

            {/* Security Settings */}
            <TabsContent value="security">
              <Card>
                <CardHeader>
                  <CardTitle>Security Settings</CardTitle>
                  <CardDescription>Password policies, 2FA requirements, session timeout</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <Label htmlFor="passwordMinLength">Minimum Password Length</Label>
                    <Input
                      id="passwordMinLength"
                      type="number"
                      value={settings.security.passwordMinLength}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          security: { ...settings.security, passwordMinLength: parseInt(e.target.value) },
                        })
                      }
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="requireUppercase">Require Uppercase Letters</Label>
                    <Switch
                      id="requireUppercase"
                      checked={settings.security.requireUppercase}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          security: { ...settings.security, requireUppercase: checked },
                        })
                      }
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="requireLowercase">Require Lowercase Letters</Label>
                    <Switch
                      id="requireLowercase"
                      checked={settings.security.requireLowercase}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          security: { ...settings.security, requireLowercase: checked },
                        })
                      }
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="requireNumbers">Require Numbers</Label>
                    <Switch
                      id="requireNumbers"
                      checked={settings.security.requireNumbers}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          security: { ...settings.security, requireNumbers: checked },
                        })
                      }
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="requireSpecialChars">Require Special Characters</Label>
                    <Switch
                      id="requireSpecialChars"
                      checked={settings.security.requireSpecialChars}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          security: { ...settings.security, requireSpecialChars: checked },
                        })
                      }
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="twoFactorRequired">Require 2FA</Label>
                    <Switch
                      id="twoFactorRequired"
                      checked={settings.security.twoFactorRequired}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          security: { ...settings.security, twoFactorRequired: checked },
                        })
                      }
                    />
                  </div>
                  <div>
                    <Label htmlFor="sessionTimeout">Session Timeout (minutes)</Label>
                    <Input
                      id="sessionTimeout"
                      type="number"
                      value={settings.security.sessionTimeout}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          security: { ...settings.security, sessionTimeout: parseInt(e.target.value) },
                        })
                      }
                    />
                  </div>
                  <Button onClick={() => handleSave('Security')}>
                    <Save className="mr-2" size={16} />
                    Save Security Settings
                  </Button>
                </CardContent>
              </Card>
            </TabsContent>

            {/* Integrations */}
            <TabsContent value="integrations">
              <Card>
                <CardHeader>
                  <CardTitle>Integrations</CardTitle>
                  <CardDescription>Payment gateways, SMS providers, Email services</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <Label htmlFor="emailProvider">Email Provider</Label>
                    <Input
                      id="emailProvider"
                      value={settings.integrations.emailProvider}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          integrations: { ...settings.integrations, emailProvider: e.target.value },
                        })
                      }
                    />
                    <Button variant="outline" className="mt-2" onClick={() => toast.success('Testing email connection...')}>
                      Test Connection
                    </Button>
                  </div>
                  <div>
                    <Label htmlFor="smsProvider">SMS Provider</Label>
                    <Input
                      id="smsProvider"
                      value={settings.integrations.smsProvider}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          integrations: { ...settings.integrations, smsProvider: e.target.value },
                        })
                      }
                    />
                    <Button variant="outline" className="mt-2" onClick={() => toast.success('Testing SMS connection...')}>
                      Test Connection
                    </Button>
                  </div>
                  <div>
                    <Label htmlFor="paymentGateway">Payment Gateway</Label>
                    <Input
                      id="paymentGateway"
                      value={settings.integrations.paymentGateway}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          integrations: { ...settings.integrations, paymentGateway: e.target.value },
                        })
                      }
                    />
                    <Button variant="outline" className="mt-2" onClick={() => toast.success('Testing payment gateway...')}>
                      Test Connection
                    </Button>
                  </div>
                  <Button onClick={() => handleSave('Integrations')}>
                    <Save className="mr-2" size={16} />
                    Save Integration Settings
                  </Button>
                </CardContent>
              </Card>
            </TabsContent>

            {/* Notifications */}
            <TabsContent value="notifications">
              <Card>
                <CardHeader>
                  <CardTitle>Notification Settings</CardTitle>
                  <CardDescription>Email templates, SMS templates, Push notification settings</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="emailEnabled">Email Notifications</Label>
                    <Switch
                      id="emailEnabled"
                      checked={settings.notifications.emailEnabled}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          notifications: { ...settings.notifications, emailEnabled: checked },
                        })
                      }
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="smsEnabled">SMS Notifications</Label>
                    <Switch
                      id="smsEnabled"
                      checked={settings.notifications.smsEnabled}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          notifications: { ...settings.notifications, smsEnabled: checked },
                        })
                      }
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="pushEnabled">Push Notifications</Label>
                    <Switch
                      id="pushEnabled"
                      checked={settings.notifications.pushEnabled}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          notifications: { ...settings.notifications, pushEnabled: checked },
                        })
                      }
                    />
                  </div>
                  <Button onClick={() => handleSave('Notifications')}>
                    <Save className="mr-2" size={16} />
                    Save Notification Settings
                  </Button>
                </CardContent>
              </Card>
            </TabsContent>

            {/* Features */}
            <TabsContent value="features">
              <Card>
                <CardHeader>
                  <CardTitle>Feature Toggles</CardTitle>
                  <CardDescription>Enable/disable platform features</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="telemedicine">Telemedicine</Label>
                    <Switch
                      id="telemedicine"
                      checked={settings.features.telemedicine}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          features: { ...settings.features, telemedicine: checked },
                        })
                      }
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="prescriptions">Prescriptions</Label>
                    <Switch
                      id="prescriptions"
                      checked={settings.features.prescriptions}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          features: { ...settings.features, prescriptions: checked },
                        })
                      }
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="payments">Payments</Label>
                    <Switch
                      id="payments"
                      checked={settings.features.payments}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          features: { ...settings.features, payments: checked },
                        })
                      }
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <Label htmlFor="analytics">Analytics</Label>
                    <Switch
                      id="analytics"
                      checked={settings.features.analytics}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          features: { ...settings.features, analytics: checked },
                        })
                      }
                    />
                  </div>
                  <Button onClick={() => handleSave('Features')}>
                    <Save className="mr-2" size={16} />
                    Save Feature Settings
                  </Button>
                </CardContent>
              </Card>
            </TabsContent>

            {/* Compliance */}
            <TabsContent value="compliance">
              <Card>
                <CardHeader>
                  <CardTitle>Compliance Settings</CardTitle>
                  <CardDescription>HIPAA settings, Data retention, Audit log retention</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="hipaaEnabled">HIPAA Compliance</Label>
                    <Switch
                      id="hipaaEnabled"
                      checked={settings.compliance.hipaaEnabled}
                      onCheckedChange={(checked) =>
                        setSettings({
                          ...settings,
                          compliance: { ...settings.compliance, hipaaEnabled: checked },
                        })
                      }
                    />
                  </div>
                  <div>
                    <Label htmlFor="dataRetentionDays">Data Retention (days)</Label>
                    <Input
                      id="dataRetentionDays"
                      type="number"
                      value={settings.compliance.dataRetentionDays}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          compliance: { ...settings.compliance, dataRetentionDays: parseInt(e.target.value) },
                        })
                      }
                    />
                  </div>
                  <div>
                    <Label htmlFor="auditLogRetentionDays">Audit Log Retention (days)</Label>
                    <Input
                      id="auditLogRetentionDays"
                      type="number"
                      value={settings.compliance.auditLogRetentionDays}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          compliance: { ...settings.compliance, auditLogRetentionDays: parseInt(e.target.value) },
                        })
                      }
                    />
                  </div>
                  <Button onClick={() => handleSave('Compliance')}>
                    <Save className="mr-2" size={16} />
                    Save Compliance Settings
                  </Button>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}

