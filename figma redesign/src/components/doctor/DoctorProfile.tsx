import React, { useState, useEffect } from 'react';
import { Edit, Save, X, Upload, Building2 } from 'lucide-react';
import { Card, CardHeader, CardContent } from '../ui/Card';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { authApi, doctorApi, facilitiesApi } from '../../utils/mockApi';
import { mockDoctor, type Doctor, type Facility } from '../../utils/mockData';

interface DoctorProfileProps {
  onToast: (type: 'success' | 'error', message: string) => void;
}

export function DoctorProfile({ onToast }: DoctorProfileProps) {
  const [loading, setLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [doctor, setDoctor] = useState<Doctor>(mockDoctor);
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    specialty: '',
    licenseNumber: '',
    pmdcNumber: ''
  });
  const [passwordData, setPasswordData] = useState({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [showPasswordSection, setShowPasswordSection] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [userData, facilitiesData] = await Promise.all([
        authApi.me('doctor'),
        facilitiesApi.listForDoctor('doc-1')
      ]);
      setDoctor(userData as Doctor);
      setFacilities(facilitiesData);
      setFormData({
        name: userData.name,
        email: userData.email,
        phone: userData.phone,
        specialty: (userData as Doctor).specialty,
        licenseNumber: (userData as Doctor).licenseNumber,
        pmdcNumber: (userData as Doctor).pmdcNumber
      });
    } catch (error) {
      console.error('Failed to load profile:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      await doctorApi.updateProfile(formData);
      setIsEditing(false);
      onToast('success', 'Profile updated successfully');
      loadData();
    } catch (error) {
      onToast('error', 'Failed to update profile');
    }
  };

  const handleChangePassword = async () => {
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      onToast('error', 'Passwords do not match');
      return;
    }
    if (passwordData.newPassword.length < 8) {
      onToast('error', 'Password must be at least 8 characters');
      return;
    }

    try {
      await authApi.changePassword(passwordData.oldPassword, passwordData.newPassword);
      setPasswordData({ oldPassword: '', newPassword: '', confirmPassword: '' });
      setShowPasswordSection(false);
      onToast('success', 'Password changed successfully');
    } catch (error) {
      onToast('error', 'Failed to change password');
    }
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-neutral-900">Profile</h2>
          <p className="text-neutral-600 mt-1">Manage your personal and professional information</p>
        </div>
        {!isEditing ? (
          <Button onClick={() => setIsEditing(true)}>
            <Edit className="w-5 h-5" />
            Edit Profile
          </Button>
        ) : (
          <div className="flex gap-2">
            <Button onClick={handleSave}>
              <Save className="w-5 h-5" />
              Save Changes
            </Button>
            <Button variant="secondary" onClick={() => {
              setIsEditing(false);
              setFormData({
                name: doctor.name,
                email: doctor.email,
                phone: doctor.phone,
                specialty: doctor.specialty,
                licenseNumber: doctor.licenseNumber,
                pmdcNumber: doctor.pmdcNumber
              });
            }}>
              <X className="w-5 h-5" />
              Cancel
            </Button>
          </div>
        )}
      </div>

      {/* Avatar */}
      <Card>
        <CardContent>
          <div className="flex items-center gap-6">
            {doctor.avatar ? (
              <img src={doctor.avatar} alt={doctor.name} className="w-24 h-24 rounded-full" />
            ) : (
              <div className="w-24 h-24 rounded-full bg-primary-100 flex items-center justify-center">
                <span className="text-primary-700 text-3xl">
                  {doctor.name.split(' ').map(n => n[0]).join('')}
                </span>
              </div>
            )}
            <div className="flex-1">
              <h3 className="text-neutral-900">{doctor.name}</h3>
              <p className="text-neutral-600">{doctor.specialty}</p>
              {isEditing && (
                <Button size="sm" variant="secondary" className="mt-2">
                  <Upload className="w-4 h-4" />
                  Upload New Photo
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Personal Information */}
      <Card>
        <CardHeader>
          <h3 className="text-neutral-900">Personal Information</h3>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Full Name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              disabled={!isEditing}
            />
            <Input
              label="Email Address"
              type="email"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              disabled={!isEditing}
            />
            <Input
              label="Phone Number"
              type="tel"
              value={formData.phone}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              disabled={!isEditing}
            />
          </div>
        </CardContent>
      </Card>

      {/* Professional Information */}
      <Card>
        <CardHeader>
          <h3 className="text-neutral-900">Professional Information</h3>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Specialty"
              value={formData.specialty}
              onChange={(e) => setFormData({ ...formData, specialty: e.target.value })}
              disabled={!isEditing}
            />
            <Input
              label="License Number"
              value={formData.licenseNumber}
              onChange={(e) => setFormData({ ...formData, licenseNumber: e.target.value })}
              disabled={!isEditing}
            />
            <Input
              label="PMDC Number"
              value={formData.pmdcNumber}
              onChange={(e) => setFormData({ ...formData, pmdcNumber: e.target.value })}
              disabled={!isEditing}
            />
          </div>
        </CardContent>
      </Card>

      {/* Clinics Summary */}
      <Card>
        <CardHeader>
          <h3 className="text-neutral-900">Your Clinics</h3>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {facilities.map((facility) => (
              <div key={facility.id} className="flex items-center justify-between p-3 bg-neutral-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-primary-100 rounded-lg flex items-center justify-center">
                    <Building2 className="w-5 h-5 text-primary-700" />
                  </div>
                  <div>
                    <p className="text-neutral-900">{facility.name}</p>
                    <p className="text-sm text-neutral-600">{facility.city}, {facility.state}</p>
                  </div>
                </div>
                <span className={`px-2.5 py-0.5 rounded-full text-sm ${
                  facility.status === 'ACTIVE' 
                    ? 'bg-success-50 text-success-700' 
                    : 'bg-neutral-100 text-neutral-600'
                }`}>
                  {facility.status}
                </span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Security */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <h3 className="text-neutral-900">Security</h3>
            {!showPasswordSection && (
              <Button variant="secondary" onClick={() => setShowPasswordSection(true)}>
                Change Password
              </Button>
            )}
          </div>
        </CardHeader>
        {showPasswordSection && (
          <CardContent>
            <div className="space-y-4">
              <Input
                label="Current Password"
                type="password"
                value={passwordData.oldPassword}
                onChange={(e) => setPasswordData({ ...passwordData, oldPassword: e.target.value })}
              />
              <Input
                label="New Password"
                type="password"
                value={passwordData.newPassword}
                onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })}
                hint="Must be at least 8 characters"
              />
              <Input
                label="Confirm New Password"
                type="password"
                value={passwordData.confirmPassword}
                onChange={(e) => setPasswordData({ ...passwordData, confirmPassword: e.target.value })}
              />
              <div className="flex gap-2 pt-2">
                <Button onClick={handleChangePassword}>
                  Update Password
                </Button>
                <Button variant="secondary" onClick={() => {
                  setShowPasswordSection(false);
                  setPasswordData({ oldPassword: '', newPassword: '', confirmPassword: '' });
                }}>
                  Cancel
                </Button>
              </div>
            </div>
          </CardContent>
        )}
      </Card>

      {/* Notifications */}
      <Card>
        <CardHeader>
          <h3 className="text-neutral-900">Notification Preferences</h3>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            <label className="flex items-center justify-between p-3 border border-neutral-200 rounded-lg cursor-pointer hover:bg-neutral-50">
              <div>
                <p className="text-neutral-900">Email Notifications</p>
                <p className="text-sm text-neutral-500">Receive appointment reminders via email</p>
              </div>
              <input type="checkbox" defaultChecked className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500" />
            </label>
            <label className="flex items-center justify-between p-3 border border-neutral-200 rounded-lg cursor-pointer hover:bg-neutral-50">
              <div>
                <p className="text-neutral-900">SMS Notifications</p>
                <p className="text-sm text-neutral-500">Receive appointment updates via SMS</p>
              </div>
              <input type="checkbox" defaultChecked className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500" />
            </label>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
