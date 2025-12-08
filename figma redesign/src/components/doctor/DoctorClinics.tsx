import React, { useState, useEffect } from 'react';
import { Plus, MapPin, DollarSign, Clock, Power, Edit, Building2 } from 'lucide-react';
import { Card, CardContent } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { Select } from '../ui/Select';
import { EmptyState } from '../ui/EmptyState';
import { CardSkeleton } from '../ui/Skeleton';
import { facilitiesApi } from '../../utils/mockApi';
import { locationData, type Facility } from '../../utils/mockData';

interface DoctorClinicsProps {
  onCreateClinic: () => void;
  onEditClinic: (facility: Facility) => void;
}

export function DoctorClinics({ onCreateClinic, onEditClinic }: DoctorClinicsProps) {
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [loading, setLoading] = useState(true);
  const [stateFilter, setStateFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  useEffect(() => {
    loadFacilities();
  }, [stateFilter, statusFilter]);

  const loadFacilities = async () => {
    setLoading(true);
    try {
      const data = await facilitiesApi.listForDoctor('doc-1', {
        state: stateFilter === 'ALL' ? undefined : stateFilter,
        status: statusFilter === 'ALL' ? undefined : statusFilter
      });
      setFacilities(data);
    } catch (error) {
      console.error('Failed to load facilities:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async (facility: Facility) => {
    const action = facility.status === 'ACTIVE' ? 'deactivate' : 'activate';
    if (confirm(`Are you sure you want to ${action} this clinic?`)) {
      await facilitiesApi.toggleStatus(facility.id);
      loadFacilities();
    }
  };

  const filteredFacilities = facilities.filter(fac => {
    if (stateFilter !== 'ALL' && fac.state !== stateFilter) return false;
    if (statusFilter !== 'ALL' && fac.status !== statusFilter) return false;
    return true;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-neutral-900">Clinics</h2>
          <p className="text-neutral-600 mt-1">Manage your clinic locations and services</p>
        </div>
        <Button onClick={onCreateClinic}>
          <Plus className="w-5 h-5" />
          Add Clinic
        </Button>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="!py-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Select
              options={[
                { value: 'ALL', label: 'All States' },
                ...locationData.states
              ]}
              value={stateFilter}
              onChange={setStateFilter}
            />
            <Select
              options={[
                { value: 'ALL', label: 'All Statuses' },
                { value: 'ACTIVE', label: 'Active' },
                { value: 'DEACTIVATED', label: 'Deactivated' }
              ]}
              value={statusFilter}
              onChange={setStatusFilter}
            />
          </div>
        </CardContent>
      </Card>

      {/* Clinics List */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2].map(i => <CardSkeleton key={i} />)}
        </div>
      ) : filteredFacilities.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Building2 className="w-8 h-8" />}
            title="No clinics found"
            description="Create your first clinic to start accepting appointments."
            action={
              <Button onClick={onCreateClinic}>
                <Plus className="w-5 h-5" />
                Add Clinic
              </Button>
            }
          />
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {filteredFacilities.map((facility) => (
            <Card key={facility.id} className="hover:shadow-md transition-shadow">
              <CardContent>
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1">
                    <h4 className="text-neutral-900 mb-1">{facility.name}</h4>
                    <Badge variant={facility.status === 'ACTIVE' ? 'success' : 'default'}>
                      {facility.status === 'ACTIVE' ? 'Active' : 'Deactivated'}
                    </Badge>
                  </div>
                </div>

                <div className="space-y-2 mb-4 text-sm">
                  <div className="flex items-start gap-2 text-neutral-600">
                    <MapPin className="w-4 h-4 mt-0.5 flex-shrink-0" />
                    <div>
                      <p className="text-neutral-900">{facility.address}</p>
                      <p className="text-neutral-600">{facility.town}, {facility.city}, {facility.state}</p>
                    </div>
                  </div>
                  
                  <div className="flex items-center gap-2 text-neutral-600">
                    <DollarSign className="w-4 h-4" />
                    <span className="text-neutral-900">PKR {facility.fee.toLocaleString()} consultation fee</span>
                  </div>

                  <div className="flex items-center gap-2 text-neutral-600">
                    <Clock className="w-4 h-4" />
                    <span className="text-neutral-900">{facility.hours}</span>
                  </div>
                </div>

                <div className="flex flex-wrap gap-2 mb-4">
                  {facility.services.map((service) => (
                    <Badge key={service} variant="info">
                      {service === 'ONLINE' ? 'Online Consultation' : 'On-site Visit'}
                    </Badge>
                  ))}
                </div>

                <div className="flex gap-2 pt-3 border-t border-neutral-200">
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => onEditClinic(facility)}
                  >
                    <Edit className="w-4 h-4" />
                    Edit
                  </Button>
                  <Button
                    size="sm"
                    variant="text"
                    onClick={() => handleToggleStatus(facility)}
                  >
                    <Power className="w-4 h-4" />
                    {facility.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
