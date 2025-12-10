// User Types
export type UserType = 'PATIENT' | 'DOCTOR' | 'ADMIN';

export interface User {
  // Backend sends UUID string; normalize to string on the frontend
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  // Normalized role information from backend's `role` field
  // We store both for backwards compatibility with existing components
  userType: UserType;
  role?: UserType;
  isVerified: boolean;
  createdAt: string;
  dateOfBirth?: string;
  address?: string;
  specialization?: string;
  licenseNumber?: string;
  yearsOfExperience?: number;
  experienceYears?: number;
  availabilityNote?: string;
  clinicCount?: number;
  activeClinicCount?: number;
  updatedAt?: string;
  isActive?: boolean;
  approvalStatus?: 'PENDING' | 'APPROVED' | 'REJECTED';
  pmdcId?: string;
  averageRating?: number;
  totalRevenue?: number;
  totalReviews?: number;
}

// API Response Types
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}

export interface LoginResponse {
  message: string;
  user: User;
  token?: string;
  accessToken?: string;
  refreshToken?: string;
}

// Signup Request
export interface SignupRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  role?: 'PATIENT' | 'DOCTOR' | 'ADMIN' | 'ORGANIZATION';
  dateOfBirth?: string;
  address?: string;
  specialization?: string;
  licenseNumber?: string;
  yearsOfExperience?: number;
}

// Login Request
export interface LoginRequest {
  email: string;
  password: string;
}

// OTP Verification
export interface OtpVerificationRequest {
  email: string;
  otp: string;
  userType: UserType;
}

// Doctor
export interface Doctor extends User {
  specialization: string;
  licenseNumber: string;
  yearsOfExperience: number;
  averageRating?: number;
  totalReviews?: number;
  consultationFee?: number;
}

// Appointment
export type AppointmentType = 'ONLINE' | 'ONSITE';
export type AppointmentStatus = 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface Appointment {
  id: number;
  appointmentDateTime: string;
  endTime?: string; // Appointment end time
  reason: string;
  notes?: string;
  status: AppointmentStatus;
  appointmentType: AppointmentType;
  zoomMeetingId?: string;
  zoomMeetingUrl?: string;
  zoomMeetingPassword?: string;
  zoomJoinUrl?: string;
  zoomStartUrl?: string; // For doctors to start the meeting
  patientId: string; // UUID string
  patientName: string;
  consultationFee?: number;
  patientEmail?: string;
  doctorId: string; // UUID string
  doctorName: string;
  doctorSpecialization?: string;
  clinicId?: string; // UUID from backend facilityId
  clinicName?: string;
  clinicAddress?: string;
  createdAt: string;
  updatedAt: string;
  isEmergency?: boolean;
}

export interface AppointmentRequest {
  appointmentDateTime: string;
  reason: string;
  notes?: string;
  doctorId: number;
  clinicId?: string; // UUID from backend facilityId
  appointmentType: AppointmentType;
  isEmergency?: boolean;
}

// Medical History
export type MedicalHistoryStatus = 'ACTIVE' | 'RESOLVED' | 'CHRONIC' | 'UNDER_TREATMENT';

export interface MedicalHistory {
  id: string; // Changed from number to string to match backend UUIDs
  condition: string;
  diagnosisDate: string;
  description: string;
  treatment: string;
  medications: string;
  doctorName: string;
  hospitalName: string;
  status: MedicalHistoryStatus;
  patientId: string; // Changed from number to string to match backend UUIDs
  patientName: string;
  createdAt: string;
  updatedAt: string;
}

export interface MedicalHistoryRequest {
  condition: string;
  diagnosisDate: string;
  description: string;
  treatment: string;
  medications: string;
  doctorName: string;
  hospitalName: string;
  status: MedicalHistoryStatus;
}

// Clinic
export interface Clinic {
  id: string;
  name: string;
  address: string;
  town?: string;
  city: string;
  state: string;
  zipCode: string;
  phoneNumber: string;
  email: string;
  description: string;
  openingTime: string;
  closingTime: string;
  active: boolean;
  doctorId: string;
  doctorName: string;
  createdAt: string;
  updatedAt: string;
  latitude?: number;
  longitude?: number;
  consultationFee?: number;
  servicesOffered?: string; // e.g., "ONLINE,ONSITE" or "ONSITE"
}

export interface ClinicRequest {
  name: string;
  address: string;
  town?: string;
  city: string;
  state: string;
  zipCode: string;
  phoneNumber: string;
  email: string;
  description: string;
  openingTime: string;
  closingTime: string;
  consultationFee: number;
  latitude?: number;
  longitude?: number;
  servicesOffered?: string; // e.g., "ONLINE,ONSITE" or "ONSITE"
}

export interface PatientProfileUpdateRequest {
  phoneNumber: string;
  address: string;
}

// Appointment Slots
export interface SlotResponse {
  startTime: string;
  endTime: string;
  status: 'AVAILABLE' | 'BOOKED';
}

// Zoom Meeting
export interface ZoomMeeting {
  meetingId: string;
  meetingUrl: string;
  password: string;
  joinUrl: string;
  appointmentDateTime: string;
  patientName: string;
  doctorName: string;
}

// Emergency Patient Types
export interface CreateEmergencyPatientRequest {
  patientName: string;
  phoneNumber?: string;
}

export interface EmergencyPatientResponse {
  patientId: string;
  email: string;
  patientName: string;
  phoneNumber?: string;
}

export interface CreateEmergencyPatientAndAppointmentRequest {
  patientName: string;
  phoneNumber?: string;
  appointmentRequest: {
    doctorId: string;
    facilityId: string;
    serviceOfferingId?: string;
    appointmentTime: string;
    reasonForVisit: string;
    isEmergency: boolean;
  };
}

export interface EmergencyPatientAndAppointmentResponse {
  patient: EmergencyPatientResponse;
  appointment: Appointment;
}

// Admin Dashboard
export interface AdminDashboard {
  totalPatients: number;
  totalDoctors: number;
  totalAdmins: number;
  totalAppointments: number;
  totalClinics: number;
  totalMedicalHistories: number;
  pendingAppointments: number;
  confirmedAppointments: number;
  completedAppointments: number;
}

