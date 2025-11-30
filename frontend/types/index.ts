// User Types
export type UserType = 'PATIENT' | 'DOCTOR' | 'ADMIN';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  userType: UserType;
  isVerified: boolean;
  createdAt: string;
  dateOfBirth?: string;
  address?: string;
  specialization?: string;
  licenseNumber?: string;
  yearsOfExperience?: number;
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
  token: string;
}

// Signup Request
export interface SignupRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
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
}

// Appointment
export type AppointmentType = 'ONLINE' | 'ONSITE';
export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'REJECTED';

export interface Appointment {
  id: number;
  appointmentDateTime: string;
  reason: string;
  notes?: string;
  status: AppointmentStatus;
  appointmentType: AppointmentType;
  zoomMeetingId?: string;
  zoomMeetingUrl?: string;
  zoomMeetingPassword?: string;
  zoomJoinUrl?: string;
  patientId: number;
  patientName: string;
  patientEmail?: string;
  doctorId: number;
  doctorName: string;
  doctorSpecialization?: string;
  clinicId?: number;
  clinicName?: string;
  clinicAddress?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AppointmentRequest {
  appointmentDateTime: string;
  reason: string;
  notes?: string;
  doctorId: number;
  clinicId?: number;
  appointmentType: AppointmentType;
}

// Medical History
export type MedicalHistoryStatus = 'ACTIVE' | 'RESOLVED' | 'CHRONIC' | 'UNDER_TREATMENT';

export interface MedicalHistory {
  id: number;
  condition: string;
  diagnosisDate: string;
  description: string;
  treatment: string;
  medications: string;
  doctorName: string;
  hospitalName: string;
  status: MedicalHistoryStatus;
  patientId: number;
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
  id: number;
  name: string;
  address: string;
  city: string;
  state: string;
  zipCode: string;
  phoneNumber: string;
  email: string;
  description: string;
  openingTime: string;
  closingTime: string;
  isActive: boolean;
  doctorId: number;
  doctorName: string;
  createdAt: string;
  updatedAt: string;
}

export interface ClinicRequest {
  name: string;
  address: string;
  city: string;
  state: string;
  zipCode: string;
  phoneNumber: string;
  email: string;
  description: string;
  openingTime: string;
  closingTime: string;
}

export interface PatientProfileUpdateRequest {
  phoneNumber: string;
  address: string;
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

