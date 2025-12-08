import React, { useState, useEffect } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Card, CardContent } from '../ui/Card';
import { CascadingSelect } from '../ui/CascadingSelect';
import { facilitiesApi } from '../../utils/mockApi';
import { locationData, type Facility, type AppointmentType } from '../../utils/mockData';

interface DoctorClinicFormProps {
  facility?: Facility;
  onBack: () => void;
  onSuccess: () => void;
  onToast: (type: 'success' | 'error', message: string) => void;
}

export function DoctorClinicForm({ facility, onBack, onSuccess, onToast }: DoctorClinicFormProps) {
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    phone: '',
    state: '',
    city: '',
    town: '',
    address: '',
    fee: '',
    hours: '',
    services: [] as AppointmentType[],
    status: 'ACTIVE' as 'ACTIVE' | 'DEACTIVATED'
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (facility) {
      setFormData({
        name: facility.name,
        phone: facility.phone,
        state: facility.state,
        city: facility.city,
        town: facility.town,
        address: facility.address,
        fee: facility.fee.toString(),
        hours: facility.hours,
        services: facility.services,
        status: facility.status
      });
    }
  }, [facility]);

  const getCityOptions = () => {
    if (!formData.state) return [];
    return locationData.cities[formData.state as keyof typeof locationData.cities] || [];
  };

  const getTownOptions = () => {
    if (!formData.city) return [];
    return locationData.towns[formData.city as keyof typeof locationData.towns] || [];
  };

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.name) newErrors.name = 'Clinic name is required';
    if (!formData.phone) newErrors.phone = 'Phone number is required';
    if (!formData.state) newErrors.state = 'State is required';
    if (!formData.city) newErrors.city = 'City is required';
    if (!formData.town) newErrors.town = 'Town is required';
    if (!formData.address) newErrors.address = 'Address is required';
    if (!formData.fee) newErrors.fee = 'Consultation fee is required';
    if (!formData.hours) newErrors.hours = 'Working hours are required';
    if (formData.services.length === 0) newErrors.services = 'Select at least one service';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      onToast('error', 'Please fill in all required fields');
      return;
    }

    setLoading(true);
    try {
      const data = {
        ...formData,
        fee: parseFloat(formData.fee)
      };

      if (facility) {
        await facilitiesApi.update(facility.id, data);
        onToast('success', 'Clinic updated successfully');
      } else {
        await facilitiesApi.create(data);
        onToast('success', 'Clinic created successfully');
      }
      onSuccess();
    } catch (error) {
      onToast('error', 'Failed to save clinic');
    } finally {
      setLoading(false);
    }
  };

  const handleServiceToggle = (service: AppointmentType) => {
    setFormData(prev => ({
      ...prev,
      services: prev.services.includes(service)
        ? prev.services.filter(s => s !== service)
        : [...prev.services, service]
    }));
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Button variant="text" onClick={onBack}>
          <ArrowLeft className="w-5 h-5" />
        </Button>
        <div>
          <h2 className="text-neutral-900">{facility ? 'Edit Clinic' : 'Add New Clinic'}</h2>
          <p className="text-neutral-600 mt-1">
            {facility ? 'Update clinic information' : 'Create a new clinic location'}
          </p>
        </div>
      </div>

      {/* Form */}
      <Card>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Basic Information */}
            <div>
              <h3 className="text-neutral-900 mb-4">Basic Information</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="Clinic Name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  error={errors.name}
                  placeholder="e.g., Heart Care Clinic"
                />
                <Input
                  label="Phone Number"
                  type="tel"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  error={errors.phone}
                  placeholder="+92 21 1234567"
                />
              </div>
            </div>

            {/* Location */}
            <div>
              <h3 className="text-neutral-900 mb-4">Location</h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                <CascadingSelect
                  label="State"
                  options={locationData.states}
                  value={formData.state}
                  onChange={(value) => setFormData({ ...formData, state: value, city: '', town: '' })}
                  error={errors.state}
                  placeholder="Select state"
                />
                <CascadingSelect
                  label="City"
                  options={getCityOptions()}
                  value={formData.city}
                  onChange={(value) => setFormData({ ...formData, city: value, town: '' })}
                  error={errors.city}
                  placeholder="Select city"
                  disabled={!formData.state}
                />
                <CascadingSelect
                  label="Town"
                  options={getTownOptions()}
                  value={formData.town}
                  onChange={(value) => setFormData({ ...formData, town: value })}
                  error={errors.town}
                  placeholder="Select town"
                  disabled={!formData.city}
                />
              </div>
              <Input
                label="Street Address"
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                error={errors.address}
                placeholder="Plot 123, Block 5, Main Boulevard"
              />
            </div>

            {/* Services & Fees */}
            <div>
              <h3 className="text-neutral-900 mb-4">Services & Fees</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <Input
                  label="Consultation Fee (PKR)"
                  type="number"
                  value={formData.fee}
                  onChange={(e) => setFormData({ ...formData, fee: e.target.value })}
                  error={errors.fee}
                  placeholder="2500"
                />
                <Input
                  label="Working Hours"
                  value={formData.hours}
                  onChange={(e) => setFormData({ ...formData, hours: e.target.value })}
                  error={errors.hours}
                  placeholder="Mon-Fri: 9AM-5PM"
                />
              </div>
              
              <div>
                <label className="block text-neutral-700 mb-3">Services Offered</label>
                <div className="space-y-2">
                  <label className="flex items-center gap-3 p-3 border border-neutral-300 rounded-lg cursor-pointer hover:bg-neutral-50">
                    <input
                      type="checkbox"
                      checked={formData.services.includes('ONLINE')}
                      onChange={() => handleServiceToggle('ONLINE')}
                      className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
                    />
                    <div>
                      <p className="text-neutral-900">Online Consultation</p>
                      <p className="text-sm text-neutral-500">Video consultations via Zoom</p>
                    </div>
                  </label>
                  
                  <label className="flex items-center gap-3 p-3 border border-neutral-300 rounded-lg cursor-pointer hover:bg-neutral-50">
                    <input
                      type="checkbox"
                      checked={formData.services.includes('ONSITE')}
                      onChange={() => handleServiceToggle('ONSITE')}
                      className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
                    />
                    <div>
                      <p className="text-neutral-900">On-site Visit</p>
                      <p className="text-sm text-neutral-500">In-person consultations at clinic</p>
                    </div>
                  </label>
                </div>
                {errors.services && (
                  <p className="mt-1.5 text-sm text-danger-600">{errors.services}</p>
                )}
              </div>
            </div>

            {/* Status */}
            <div>
              <label className="flex items-center gap-3">
                <input
                  type="checkbox"
                  checked={formData.status === 'ACTIVE'}
                  onChange={(e) => setFormData({ ...formData, status: e.target.checked ? 'ACTIVE' : 'DEACTIVATED' })}
                  className="w-4 h-4 text-primary-600 border-neutral-300 rounded focus:ring-primary-500"
                />
                <div>
                  <p className="text-neutral-900">Active</p>
                  <p className="text-sm text-neutral-500">Clinic is accepting appointments</p>
                </div>
              </label>
            </div>

            {/* Actions */}
            <div className="flex gap-3 pt-4 border-t border-neutral-200">
              <Button type="submit" disabled={loading}>
                {loading ? 'Saving...' : facility ? 'Update Clinic' : 'Create Clinic'}
              </Button>
              <Button type="button" variant="secondary" onClick={onBack}>
                Cancel
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
