'use client';

import { useState, useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { patientApi, facilitiesApi } from '@/lib/api';
import { Doctor, Clinic } from '@/types';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import { Button } from '@/marketing/ui/button';
import { Badge } from '@/marketing/ui/badge';
import {
  Search,
  Filter,
  Star,
  MapPin,
  Clock,
  Stethoscope,
  ChevronDown,
  ChevronUp,
  X,
  Grid3x3,
  List,
  SlidersHorizontal,
} from 'lucide-react';
import Link from 'next/link';

interface DoctorWithClinics extends Doctor {
  clinics?: Clinic[];
  primaryClinic?: Clinic;
  minFee?: number;
  maxFee?: number;
}

type SortOption = 'rating' | 'fee-low' | 'fee-high' | 'experience' | 'name';
type ViewMode = 'grid' | 'list';

export default function DoctorsPage() {
  const router = useRouter();
  const [doctors, setDoctors] = useState<DoctorWithClinics[]>([]);
  const [filteredDoctors, setFilteredDoctors] = useState<DoctorWithClinics[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [sortBy, setSortBy] = useState<SortOption>('rating');
  const [showFilters, setShowFilters] = useState(true);

  // Search and filters
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSpecialty, setSelectedSpecialty] = useState<string>('');
  const [selectedCity, setSelectedCity] = useState<string>('');
  const [minRating, setMinRating] = useState<number>(0);
  const [minFee, setMinFee] = useState<number>(0);
  const [maxFee, setMaxFee] = useState<number>(50000);
  const [availableToday, setAvailableToday] = useState(false);
  const [consultationType, setConsultationType] = useState<'all' | 'online' | 'onsite'>('all');

  useEffect(() => {
    loadDoctors();
  }, []);

  useEffect(() => {
    applyFilters();
  }, [doctors, searchQuery, selectedSpecialty, selectedCity, minRating, minFee, maxFee, availableToday, consultationType, sortBy]);

  const loadDoctors = async () => {
    setIsLoading(true);
    try {
      const response = await patientApi.getDoctors();
      let doctorsList: Doctor[] = [];
      if (Array.isArray(response)) {
        doctorsList = response;
      } else if (response && typeof response === 'object' && 'data' in response) {
        const data = (response as any).data;
        if (Array.isArray(data)) {
          doctorsList = data;
        }
      }

      // Load clinics for each doctor
      const doctorsWithClinics = await Promise.all(
        doctorsList.map(async (doctor) => {
          try {
            const clinics = await facilitiesApi.listForDoctor(doctor.id.toString());
            const activeClinics = clinics.filter((c) => c.active);
            const fees = activeClinics
              .map((c) => c.consultationFee || 0)
              .filter((f) => f > 0);
            
            return {
              ...doctor,
              clinics: activeClinics,
              primaryClinic: activeClinics[0],
              minFee: fees.length > 0 ? Math.min(...fees) : undefined,
              maxFee: fees.length > 0 ? Math.max(...fees) : undefined,
            } as DoctorWithClinics;
          } catch {
            return { ...doctor, clinics: [], primaryClinic: undefined } as DoctorWithClinics;
          }
        })
      );

      setDoctors(doctorsWithClinics);
    } catch (error: any) {
      toast.error('Failed to load doctors');
      console.error('Error loading doctors:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const specialties = useMemo(() => {
    const unique = Array.from(new Set(doctors.map((d) => d.specialization).filter(Boolean)));
    return unique.sort();
  }, [doctors]);

  const cities = useMemo(() => {
    const allCities = doctors
      .flatMap((d) => d.clinics?.map((c) => c.city).filter(Boolean) || [])
      .filter(Boolean);
    return Array.from(new Set(allCities)).sort();
  }, [doctors]);

  const applyFilters = () => {
    let filtered = [...doctors];

    // Search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (doctor) =>
          `${doctor.firstName} ${doctor.lastName}`.toLowerCase().includes(query) ||
          doctor.specialization?.toLowerCase().includes(query) ||
          doctor.clinics?.some((c) => c.name.toLowerCase().includes(query) || c.address.toLowerCase().includes(query))
      );
    }

    // Specialty filter
    if (selectedSpecialty) {
      filtered = filtered.filter((doctor) => doctor.specialization === selectedSpecialty);
    }

    // City filter - Only show doctors who have clinics in the selected city
    if (selectedCity) {
      filtered = filtered.filter((doctor) =>
        doctor.clinics && doctor.clinics.length > 0 && doctor.clinics.some((c) => c.city === selectedCity)
      );
    }

    // Rating filter
    if (minRating > 0) {
      filtered = filtered.filter((doctor) => (doctor as any).averageRating >= minRating);
    }

    // Fee filter
    filtered = filtered.filter((doctor) => {
      if (doctor.minFee === undefined) return true;
      return doctor.minFee >= minFee && (doctor.maxFee || doctor.minFee) <= maxFee;
    });

    // Consultation type filter (simplified - checks if has clinics for onsite, or any for online)
    if (consultationType === 'onsite') {
      filtered = filtered.filter((doctor) => doctor.clinics && doctor.clinics.length > 0);
    }
    // For 'online', we'd need to check if doctor offers online consultations
    // For now, we'll show all if 'online' is selected

    // Sort
    filtered.sort((a, b) => {
      switch (sortBy) {
        case 'rating':
          return ((b as any).averageRating || 0) - ((a as any).averageRating || 0);
        case 'fee-low':
          return (a.minFee || Infinity) - (b.minFee || Infinity);
        case 'fee-high':
          return (b.maxFee || 0) - (a.maxFee || 0);
        case 'experience':
          return (b.yearsOfExperience || 0) - (a.yearsOfExperience || 0);
        case 'name':
          return `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`);
        default:
          return 0;
      }
    });

    setFilteredDoctors(filtered);
  };

  const clearFilters = () => {
    setSearchQuery('');
    setSelectedSpecialty('');
    setSelectedCity('');
    setMinRating(0);
    setMinFee(0);
    setMaxFee(50000);
    setAvailableToday(false);
    setConsultationType('all');
  };

  const activeFiltersCount = useMemo(() => {
    let count = 0;
    if (searchQuery) count++;
    if (selectedSpecialty) count++;
    if (selectedCity) count++;
    if (minRating > 0) count++;
    if (minFee > 0 || maxFee < 50000) count++;
    if (availableToday) count++;
    if (consultationType !== 'all') count++;
    return count;
  }, [searchQuery, selectedSpecialty, selectedCity, minRating, minFee, maxFee, availableToday, consultationType]);

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="PATIENT">
        <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="text-center py-20">
              <div className="inline-block animate-spin rounded-full h-12 w-12 border-4 border-teal-500 border-t-transparent"></div>
              <p className="mt-4 text-slate-600">Loading doctors...</p>
            </div>
          </div>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* Header */}
          <div className="text-center mb-8">
            <h1 className="text-4xl sm:text-5xl font-bold text-slate-900 mb-3">
              Find Your Doctor
            </h1>
            <p className="text-lg text-slate-600 max-w-2xl mx-auto">
              Search from verified healthcare professionals in Pakistan
            </p>
          </div>

          {/* Search Bar */}
          <div className="mb-6">
            <div className="relative max-w-3xl mx-auto">
              <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-slate-400 z-10" />
              <input
                type="text"
                placeholder="Search by name, specialty, or location..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-12 pr-12 h-14 text-lg border-2 border-slate-200 focus:border-teal-500 rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-teal-500/20"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-4 top-1/2 transform -translate-y-1/2 text-slate-400 hover:text-slate-600 z-10"
                >
                  <X className="w-5 h-5" />
                </button>
              )}
            </div>
          </div>

          {/* Advanced Filters */}
          <div className="mb-6">
            <div className="bg-white rounded-2xl border border-slate-200 shadow-lg overflow-hidden">
              <button
                onClick={() => setShowFilters(!showFilters)}
                className="w-full px-6 py-4 flex items-center justify-between hover:bg-slate-50 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <SlidersHorizontal className="w-5 h-5 text-slate-600" />
                  <span className="font-semibold text-slate-900">Advanced Filters</span>
                  {activeFiltersCount > 0 && (
                    <Badge variant="default" className="ml-2">
                      {activeFiltersCount} active
                    </Badge>
                  )}
                </div>
                {showFilters ? (
                  <ChevronUp className="w-5 h-5 text-slate-600" />
                ) : (
                  <ChevronDown className="w-5 h-5 text-slate-600" />
                )}
              </button>

              {showFilters && (
                <div className="px-6 pb-6 border-t border-slate-200">
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 pt-6">
                    {/* Specialty */}
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Specialty
                      </label>
                      <select
                        value={selectedSpecialty}
                        onChange={(e) => setSelectedSpecialty(e.target.value)}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                      >
                        <option value="">All Specialties</option>
                        {specialties.map((spec) => (
                          <option key={spec} value={spec}>
                            {spec}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* City */}
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Location
                      </label>
                      <select
                        value={selectedCity}
                        onChange={(e) => setSelectedCity(e.target.value)}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                      >
                        <option value="">All Cities</option>
                        {cities.map((city) => (
                          <option key={city} value={city}>
                            {city}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Rating */}
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Minimum Rating
                      </label>
                      <select
                        value={minRating}
                        onChange={(e) => setMinRating(Number(e.target.value))}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                      >
                        <option value={0}>Any Rating</option>
                        <option value={4}>4.0+ Stars</option>
                        <option value={4.5}>4.5+ Stars</option>
                      </select>
                    </div>

                    {/* Consultation Type */}
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Consultation Type
                      </label>
                      <select
                        value={consultationType}
                        onChange={(e) => setConsultationType(e.target.value as any)}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                      >
                        <option value="all">All Types</option>
                        <option value="online">Online</option>
                        <option value="onsite">On-site</option>
                      </select>
                    </div>

                    {/* Fee Range */}
                    <div className="md:col-span-2">
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Consultation Fee Range (PKR)
                      </label>
                      <div className="flex items-center gap-4">
                        <input
                          type="number"
                          placeholder="Min"
                          value={minFee || ''}
                          onChange={(e) => setMinFee(Number(e.target.value) || 0)}
                          className="flex-1 px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                        />
                        <span className="text-slate-500">to</span>
                        <input
                          type="number"
                          placeholder="Max"
                          value={maxFee || ''}
                          onChange={(e) => setMaxFee(Number(e.target.value) || 50000)}
                          className="flex-1 px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Filter Actions */}
                  <div className="flex items-center justify-between mt-6 pt-6 border-t border-slate-200">
                    <Button
                      variant="ghost"
                      onClick={clearFilters}
                      className="text-slate-600 hover:text-slate-900"
                    >
                      <X className="w-4 h-4 mr-2" />
                      Clear All Filters
                    </Button>
                    <div className="text-sm text-slate-600">
                      {filteredDoctors.length} doctor{filteredDoctors.length !== 1 ? 's' : ''} found
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Results Header */}
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-4">
              <h2 className="text-xl font-semibold text-slate-900">
                {filteredDoctors.length} Doctor{filteredDoctors.length !== 1 ? 's' : ''} Found
              </h2>
            </div>
            <div className="flex items-center gap-3">
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as SortOption)}
                className="px-4 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
              >
                <option value="rating">Sort by Rating</option>
                <option value="fee-low">Fee: Low to High</option>
                <option value="fee-high">Fee: High to Low</option>
                <option value="experience">Experience</option>
                <option value="name">Name A-Z</option>
              </select>
              <div className="flex items-center gap-1 border border-slate-300 rounded-lg p-1">
                <button
                  onClick={() => setViewMode('grid')}
                  className={`p-2 rounded ${viewMode === 'grid' ? 'bg-teal-500 text-white' : 'text-slate-600 hover:bg-slate-100'}`}
                >
                  <Grid3x3 className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setViewMode('list')}
                  className={`p-2 rounded ${viewMode === 'list' ? 'bg-teal-500 text-white' : 'text-slate-600 hover:bg-slate-100'}`}
                >
                  <List className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>

          {/* Doctor Cards Grid */}
          {filteredDoctors.length === 0 ? (
            <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center">
              <Stethoscope className="w-16 h-16 text-slate-300 mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-slate-900 mb-2">No doctors found</h3>
              <p className="text-slate-600 mb-6">
                Try adjusting your search or filters to find more results.
              </p>
              <Button onClick={clearFilters} variant="outline">
                Clear All Filters
              </Button>
            </div>
          ) : (
            <div
              className={
                viewMode === 'grid'
                  ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6'
                  : 'space-y-4'
              }
            >
              {filteredDoctors.map((doctor) => (
                <DoctorCard key={doctor.id} doctor={doctor} viewMode={viewMode} />
              ))}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}

// Doctor Card Component
function DoctorCard({
  doctor,
  viewMode,
}: {
  doctor: DoctorWithClinics;
  viewMode: ViewMode;
}) {
  const router = useRouter();
  const rating = (doctor as any).averageRating || 0;
  const reviewCount = (doctor as any).totalReviews || 0;
  const primaryClinic = doctor.primaryClinic;
  const consultationFee = doctor.minFee || primaryClinic?.consultationFee || 0;

  const handleBookAppointment = () => {
    router.push(`/patient/doctors/${doctor.id}`);
  };

  if (viewMode === 'list') {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6 hover:shadow-xl transition-all duration-300">
        <div className="flex items-start gap-6">
          {/* Avatar */}
          <div className="w-20 h-20 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center flex-shrink-0">
            <Stethoscope className="w-10 h-10 text-white" />
          </div>

          {/* Content */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between mb-3">
              <div>
                <h3 className="text-xl font-bold text-slate-900 mb-1">
                  Dr. {doctor.firstName} {doctor.lastName}
                </h3>
                <div className="flex items-center gap-2 mb-2">
                  <Badge variant="secondary" className="text-xs">
                    {doctor.specialization}
                  </Badge>
                  {rating > 0 && (
                    <div className="flex items-center gap-1 text-sm text-slate-600">
                      <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
                      <span className="font-semibold">{rating.toFixed(1)}</span>
                      {reviewCount > 0 && (
                        <span className="text-slate-500">({reviewCount})</span>
                      )}
                    </div>
                  )}
                </div>
              </div>

              {/* Fee Box */}
              {consultationFee > 0 && (
                <div className="bg-gradient-to-br from-blue-500 to-cyan-500 text-white px-4 py-2 rounded-lg text-center flex-shrink-0">
                  <div className="text-xs opacity-90">Consultation Fee</div>
                  <div className="text-xl font-bold">PKR {consultationFee.toLocaleString()}</div>
                </div>
              )}
            </div>

            {/* Show ALL clinics */}
            {doctor.clinics && doctor.clinics.length > 0 && (
              <div className="mb-3 space-y-2">
                {doctor.clinics.map((clinic) => (
                  <div key={clinic.id} className="flex items-start gap-2 text-sm text-slate-600">
                    <MapPin className="w-4 h-4 mt-0.5 flex-shrink-0 text-red-500" />
                    <div className="flex-1">
                      <div className="font-semibold text-slate-900">{clinic.name}</div>
                      <div>{clinic.address}, {clinic.city}</div>
                      <div className="flex items-center gap-1 mt-1">
                        <Clock className="w-3 h-3" />
                        <span>Opens {clinic.openingTime} • Closes {clinic.closingTime}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Experience - Highlighted */}
            {doctor.yearsOfExperience > 0 && (
              <div className="mb-3 p-2 bg-gradient-to-br from-teal-50 to-violet-50 rounded border border-teal-200 inline-block">
                <div className="flex items-center gap-2 text-xs">
                  <Stethoscope className="w-4 h-4 text-teal-600" />
                  <span className="font-semibold text-slate-900">Experience:</span>
                  <span className="text-slate-700 font-bold">{doctor.yearsOfExperience} years</span>
                </div>
              </div>
            )}

            <div className="flex items-center gap-3">
              <Button
                size="sm"
                onClick={handleBookAppointment}
                className="bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700 text-white"
              >
                View Profile
              </Button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Grid View - Matching design mockup exactly
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6 hover:shadow-xl transition-all duration-300 flex flex-col">
      {/* Header with Avatar and Rating */}
      <div className="flex items-start gap-4 mb-4">
        <div className="w-20 h-20 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center flex-shrink-0 shadow-md">
          <Stethoscope className="w-10 h-10 text-white" />
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="text-xl font-bold text-slate-900 mb-1">
            Dr. {doctor.firstName} {doctor.lastName}
          </h3>
          <Badge variant="secondary" className="text-xs mb-2">
            {doctor.specialization}
          </Badge>
          {rating > 0 ? (
            <div className="flex items-center gap-1 text-sm">
              <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
              <span className="font-semibold text-slate-900">{rating.toFixed(1)}</span>
              {reviewCount > 0 && (
                <span className="text-slate-500">({reviewCount} reviews)</span>
              )}
            </div>
          ) : (
            <div className="flex items-center gap-1 text-sm text-slate-500">
              <Star className="w-4 h-4 text-slate-300" />
              <span>No ratings yet</span>
            </div>
          )}
        </div>
      </div>

      {/* Clinic Location and Hours - Show ALL clinics */}
      {doctor.clinics && doctor.clinics.length > 0 && (
        <div className="mb-4 pb-4 border-b border-slate-100">
          {doctor.clinics.map((clinic, idx) => (
            <div key={clinic.id} className={idx > 0 ? 'mt-3 pt-3 border-t border-slate-100' : ''}>
              <div className="flex items-start gap-2 text-sm text-slate-600 mb-2">
                <MapPin className="w-4 h-4 mt-0.5 flex-shrink-0 text-red-500" />
                <div className="flex-1 min-w-0">
                  <div className="font-semibold text-slate-900 mb-1">{clinic.name}</div>
                  <div className="text-slate-600">{clinic.address}, {clinic.city}</div>
                </div>
              </div>
              <div className="flex items-center gap-1 text-sm text-slate-600 ml-6">
                <Clock className="w-4 h-4 text-slate-500" />
                <span>Opens {clinic.openingTime} • Closes {clinic.closingTime}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Brief Description */}
      <div className="mb-4">
        <p className="text-sm text-slate-600 leading-relaxed line-clamp-3">
          {doctor.yearsOfExperience > 0 
            ? `Experienced ${doctor.specialization.toLowerCase()} with ${doctor.yearsOfExperience}+ years of practice. Dedicated to providing quality healthcare services.`
            : `Qualified ${doctor.specialization.toLowerCase()} committed to providing excellent patient care and medical services.`
          }
        </p>
      </div>

      {/* Additional Details Box - Only Availability */}
      <div className="mb-4 p-3 bg-slate-50 rounded-lg border border-slate-200">
        <div className="flex items-center gap-2 text-xs text-slate-700">
          <span className="text-green-500">✅</span>
          <span className="font-medium">Available Today</span>
        </div>
      </div>

      {/* Experience - Explicitly Highlighted */}
      {doctor.yearsOfExperience > 0 && (
        <div className="mb-4 p-3 bg-gradient-to-br from-teal-50 to-violet-50 rounded-lg border border-teal-200">
          <div className="flex items-center gap-2 text-sm">
            <Stethoscope className="w-5 h-5 text-teal-600" />
            <span className="font-semibold text-slate-900">Experience:</span>
            <span className="text-slate-700 font-bold">{doctor.yearsOfExperience} years</span>
          </div>
        </div>
      )}

      {/* Consultation Fee - Explicitly Highlighted */}
      {consultationFee > 0 && (
        <div className="mb-4 bg-gradient-to-br from-blue-500 via-blue-600 to-cyan-500 text-white p-4 rounded-lg shadow-lg">
          <div className="text-xs font-medium opacity-90 mb-1 uppercase tracking-wide">Consultation Fee</div>
          <div className="text-3xl font-bold">PKR {consultationFee.toLocaleString()}</div>
        </div>
      )}

      {/* Actions - Only View Profile */}
      <div className="mt-auto pt-4">
        <Button
          size="sm"
          onClick={handleBookAppointment}
          className="w-full bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700 text-white shadow-md hover:shadow-lg transition-all"
        >
          View Profile
        </Button>
      </div>
    </div>
  );
}
