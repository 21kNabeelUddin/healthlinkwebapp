import axios from 'axios';
import type {
  ApiResponse,
  User,
  LoginRequest,
  LoginResponse,
  SignupRequest,
  OtpVerificationRequest,
  Doctor,
  Appointment,
  AppointmentRequest,
  MedicalHistory,
  MedicalHistoryRequest,
  Clinic,
  ClinicRequest,
  ZoomMeeting,
  PatientProfileUpdateRequest,
  CreateEmergencyPatientRequest,
  EmergencyPatientResponse,
  CreateEmergencyPatientAndAppointmentRequest,
  EmergencyPatientAndAppointmentResponse,
} from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token if available
api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

// Helper to normalize API error messages so the UI doesn't show raw "Request failed with status code 403"
export function getUserFriendlyError(error: any, fallback: string): string {
  const status = error?.response?.status as number | undefined;
  const backendMessage: string | undefined =
    error?.response?.data?.message || error?.response?.data?.error || error?.response?.data?.detail;

  if (backendMessage) {
    return backendMessage;
  }

  switch (status) {
    case 400:
      return 'Some of the data you entered is not valid. Please fix the highlighted fields and try again.';
    case 401:
      return 'You need to log in again to continue.';
    case 403:
      return 'You are not allowed to perform this action. Your account may not be verified or approved yet.';
    case 404:
      return 'The requested resource could not be found.';
    case 409:
      return 'This action conflicts with existing data (for example, the record already exists).';
    case 429:
      return 'Too many requests. Please wait a moment and try again.';
    case 500:
      return 'The server encountered an error. Please try again in a moment.';
    default:
      return fallback;
  }
}

// Helper to unwrap our backend's ResponseEnvelope<T> if present
function unwrapResponse<T = any>(raw: any): T {
  if (raw && typeof raw === 'object' && 'data' in raw) {
    // Most of our backend responses are shaped as { success, data, message }
    return (raw as any).data ?? raw;
  }
  return raw;
}

// Helper to transform backend AppointmentResponse to frontend Appointment format
function transformAppointment(backendAppointment: any): Appointment {
  if (!backendAppointment) {
    throw new Error('Invalid appointment data');
  }

  // Handle date - backend returns startTime as ISO string
  let appointmentDateTime = '';
  if (backendAppointment.startTime) {
    appointmentDateTime = backendAppointment.startTime;
  } else if (backendAppointment.appointmentTime) {
    appointmentDateTime = backendAppointment.appointmentTime;
  } else if (backendAppointment.appointmentDateTime) {
    appointmentDateTime = backendAppointment.appointmentDateTime;
  }

  // Validate date is valid before using
  if (appointmentDateTime) {
    const date = new Date(appointmentDateTime);
    if (isNaN(date.getTime())) {
      console.warn('Invalid date in appointment:', appointmentDateTime);
      appointmentDateTime = new Date().toISOString(); // Fallback to current date
    }
  } else {
    appointmentDateTime = new Date().toISOString(); // Fallback if no date provided
  }

  // Extract notes and appointment type
  let notes = backendAppointment.notes || '';
  let appointmentType: AppointmentType = 'ONSITE';
  
  // Extract appointment type from notes if stored there (format: "APPT_TYPE:ONLINE|actual notes")
  if (notes.startsWith('APPT_TYPE:')) {
    const parts = notes.split('|');
    const typePart = parts[0].replace('APPT_TYPE:', '');
    appointmentType = (typePart === 'ONLINE' ? 'ONLINE' : 'ONSITE') as AppointmentType;
    notes = parts.length > 1 ? parts.slice(1).join('|') : ''; // Rest of notes after type
  } else if (backendAppointment.type) {
    appointmentType = (backendAppointment.type === 'ONLINE' ? 'ONLINE' : 'ONSITE') as AppointmentType;
  } else if (backendAppointment.appointmentType) {
    appointmentType = (backendAppointment.appointmentType === 'ONLINE' ? 'ONLINE' : 'ONSITE') as AppointmentType;
  } else if (!backendAppointment.facilityId) {
    // If no facility, assume online
    appointmentType = 'ONLINE';
  }

  return {
    id: backendAppointment.id?.toString() || String(backendAppointment.id) || '',
    appointmentDateTime: appointmentDateTime,
    reason: backendAppointment.notes || backendAppointment.reasonForVisit || backendAppointment.reason || '',
    notes: notes, // Clean notes without the type prefix
    status: (backendAppointment.status || 'PENDING') as AppointmentStatus,
    appointmentType: appointmentType,
    zoomMeetingId: backendAppointment.zoomMeetingId,
    zoomMeetingUrl: backendAppointment.zoomMeetingUrl,
    zoomMeetingPassword: backendAppointment.zoomMeetingPassword,
    zoomJoinUrl: backendAppointment.zoomJoinUrl,
    patientId: backendAppointment.patientId ? (typeof backendAppointment.patientId === 'string' ? parseInt(backendAppointment.patientId) : backendAppointment.patientId) : 0,
    patientName: backendAppointment.patientName || '',
    patientEmail: backendAppointment.patientEmail,
    doctorId: backendAppointment.doctorId ? (typeof backendAppointment.doctorId === 'string' ? parseInt(backendAppointment.doctorId) : backendAppointment.doctorId) : 0,
    doctorName: backendAppointment.doctorName || '',
    doctorSpecialization: backendAppointment.doctorSpecialization,
    // Keep facilityId as string (UUID) - don't convert to number
    clinicId: backendAppointment.facilityId ? String(backendAppointment.facilityId) : undefined,
    clinicName: backendAppointment.clinicName || backendAppointment.facilityName,
    clinicAddress: backendAppointment.clinicAddress || backendAppointment.facilityAddress,
    createdAt: backendAppointment.createdAt || new Date().toISOString(),
    updatedAt: backendAppointment.updatedAt || new Date().toISOString(),
    isEmergency: backendAppointment.isEmergency || false,
  };
}

// Response interceptor for token refresh and attaching user-friendly messages
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config || {};

    // Attach a friendly message for the UI to use
    (error as any).userMessage = getUserFriendlyError(
      error,
      error?.message || 'Something went wrong while talking to the server.'
    );

    // Auto-refresh on 401 once
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      const refreshToken = typeof window !== 'undefined' ? localStorage.getItem('refreshToken') : null;
      if (refreshToken) {
        try {
          const response = await api.post('/api/v1/auth/refresh', null, {
            headers: { Authorization: `Bearer ${refreshToken}` },
          });
          const payload = unwrapResponse<{ accessToken: string; refreshToken?: string }>(response.data);
          if (payload.accessToken) {
            localStorage.setItem('token', payload.accessToken);
            if (payload.refreshToken) {
              localStorage.setItem('refreshToken', payload.refreshToken);
            }
            originalRequest.headers = originalRequest.headers || {};
            originalRequest.headers.Authorization = `Bearer ${payload.accessToken}`;
            return api(originalRequest);
          }
        } catch (refreshError: any) {
          console.error('Token refresh failed:', refreshError);
          // Only redirect if it's not a "token not found" error (might be a timing issue)
          const errorMessage = refreshError?.response?.data?.message || refreshError?.message || '';
          if (errorMessage.includes('not found') || errorMessage.includes('Invalid refresh token')) {
            // Token might not be ready yet, just clear and let user retry login
            console.warn('Refresh token validation failed, clearing auth state');
          }
          localStorage.removeItem('token');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
          if (typeof window !== 'undefined') {
            // Small delay to prevent immediate redirect on page load
            setTimeout(() => {
              window.location.href = '/auth/login';
            }, 100);
          }
        }
      }
    }

    return Promise.reject(error);
  }
);

// ==================== AUTHENTICATION API (Unified) ====================

export const authApi = {
  register: async (data: SignupRequest) => {
    const response = await api.post('/api/v1/auth/register', data);
    return unwrapResponse(response.data);
  },

  login: async (data: LoginRequest) => {
    const response = await api.post('/api/v1/auth/login', data);
    const payload = unwrapResponse<LoginResponse & { user?: User }>(response.data);

    // Normalize user shape (backend sends `role` and UUID id)
    if ((payload as any).user) {
      const rawUser: any = (payload as any).user;
      const normalizedRole = (rawUser.role || rawUser.userType) as any;
      const normalizedUser: any = {
        ...rawUser,
        id: String(rawUser.id),
        userType: normalizedRole,
        role: normalizedRole,
      };
      (payload as any).user = normalizedUser as User;

      // Store user in localStorage
      localStorage.setItem('user', JSON.stringify(normalizedUser));
    }

    // Store tokens
    if (payload?.accessToken) {
      localStorage.setItem('token', payload.accessToken);
      if ((payload as any).refreshToken) {
        localStorage.setItem('refreshToken', (payload as any).refreshToken as string);
      }
    }

    return payload;
  },

  sendOtp: async (email: string) => {
    const response = await api.post('/api/v1/auth/otp/send', { email });
    return unwrapResponse(response.data);
  },

  verifyEmail: async (email: string, otp: string) => {
    const response = await api.post('/api/v1/auth/email/verify', { email, otp });
    return unwrapResponse(response.data);
  },

  forgotPassword: async (email: string) => {
    const response = await api.post('/api/v1/auth/forgot-password', { email });
    return unwrapResponse(response.data);
  },

  resetPassword: async (email: string, otp: string, newPassword: string) => {
    const response = await api.post('/api/v1/auth/reset-password', {
      email,
      otp,
      newPassword,
    });
    return unwrapResponse(response.data);
  },

  refreshToken: async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) throw new Error('No refresh token');
    const response = await api.post('/api/v1/auth/refresh', null, {
      headers: { Authorization: `Bearer ${refreshToken}` },
    });
    const payload = unwrapResponse<{ accessToken: string; refreshToken?: string }>(response.data);
    if (payload.accessToken) {
      localStorage.setItem('token', payload.accessToken);
      if (payload.refreshToken) {
        localStorage.setItem('refreshToken', payload.refreshToken);
      }
    }
    return payload;
  },

  logout: async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    try {
      await api.post('/api/v1/auth/logout', { refreshToken }, {
        headers: refreshToken ? { 'X-Refresh-Token': refreshToken } : {},
      });
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  },
};

// ==================== PATIENT API (Legacy - for backward compatibility) ====================

export const patientApi = {
  signup: async (data: SignupRequest): Promise<ApiResponse<string>> => {
    return authApi.register(data);
  },

  // For doctors we only care if the backend verified the email; endpoint returns a plain string
  verifyOtp: async (data: OtpVerificationRequest): Promise<string> => {
    return authApi.verifyEmail(data.email, data.otp);
  },

  login: async (data: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
    return authApi.login(data);
  },

  getDoctors: async (specialization?: string): Promise<ApiResponse<Doctor[]>> => {
    const params = specialization ? { specialization } : {};
    const response = await api.get('/api/v1/search/doctors', { params });
    return response.data;
  },

  getDoctorById: async (doctorId: string): Promise<ApiResponse<Doctor>> => {
    const response = await api.get(`/api/v1/search/doctors/${doctorId}`);
    return response.data;
  },

  // Medical History (using medical records API)
  getMedicalHistories: async (patientId: string): Promise<MedicalHistory[]> => {
    return medicalRecordsApi.listForPatient(patientId);
  },

  getMedicalHistoryById: async (patientId: string, historyId: string): Promise<MedicalHistory> => {
    return medicalRecordsApi.getById(historyId);
  },

  createMedicalHistory: async (patientId: string, data: MedicalHistoryRequest): Promise<MedicalHistory> => {
    return medicalRecordsApi.create({ ...data, patientId });
  },

  updateMedicalHistory: async (patientId: string, historyId: string, data: MedicalHistoryRequest): Promise<MedicalHistory> => {
    return medicalRecordsApi.update(historyId, data);
  },

  deleteMedicalHistory: async (patientId: string, historyId: string): Promise<void> => {
    return medicalRecordsApi.delete(historyId);
  },

  // Appointments (using new unified endpoints)
  getAppointments: async (status?: string): Promise<Appointment[]> => {
    const params = status ? { status } : {};
    const response = await api.get('/api/v1/appointments', { params });
    return response.data;
  },

  getAppointmentById: async (appointmentId: string): Promise<Appointment> => {
    const response = await api.get(`/api/v1/appointments/${appointmentId}`);
    return response.data;
  },

  createAppointment: async (data: AppointmentRequest): Promise<Appointment> => {
    const response = await api.post('/api/v1/appointments', data);
    return response.data;
  },

  rescheduleAppointment: async (appointmentId: string, newStartTime: string): Promise<Appointment> => {
    const response = await api.post('/api/v1/appointments/reschedule', {
      appointmentId,
      newStartTime,
    });
    return response.data;
  },

  cancelAppointment: async (appointmentId: string, reason?: string): Promise<Appointment> => {
    const params = reason ? { reason } : {};
    const response = await api.post(`/api/v1/appointments/${appointmentId}/cancel`, null, { params });
    return response.data;
  },

  checkIn: async (appointmentId: string): Promise<Appointment> => {
    const response = await api.post(`/api/v1/appointments/${appointmentId}/patient-check-in`);
    return response.data;
  },

  getZoomMeeting: async (appointmentId: string): Promise<ZoomMeeting> => {
    const response = await api.get(`/api/v1/video-calls/${appointmentId}/session`);
    return response.data;
  },

  updateProfile: async (data: PatientProfileUpdateRequest): Promise<User> => {
    const response = await api.put('/api/v1/users/me', data);
    return response.data;
  },
};

// ==================== APPOINTMENTS API ====================

export const appointmentsApi = {
  list: async (status?: string) => {
    const params = status ? { status } : {};
    const response = await api.get('/api/v1/appointments', { params });
    const data = unwrapResponse<any[]>(response.data);
    // Ensure we always return an array and transform each appointment
    if (!Array.isArray(data)) return [];
    return data.map(transformAppointment);
  },

  getById: async (id: string) => {
    const response = await api.get(`/api/v1/appointments/${id}`);
    const data = unwrapResponse<any>(response.data);
    return transformAppointment(data);
  },

  create: async (data: any) => {
    const response = await api.post('/api/v1/appointments', data);
    const result = unwrapResponse<any>(response.data);
    return transformAppointment(result);
  },

  reschedule: async (appointmentId: string, newStartTime: string) => {
    const response = await api.post('/api/v1/appointments/reschedule', {
      appointmentId,
      newStartTime,
    });
    const result = unwrapResponse<any>(response.data);
    return transformAppointment(result);
  },

  cancel: async (id: string, reason?: string) => {
    const params = reason ? { reason } : {};
    const response = await api.post(`/api/v1/appointments/${id}/cancel`, null, { params });
    const result = unwrapResponse<any>(response.data);
    return transformAppointment(result);
  },

  checkIn: async (id: string, isPatient: boolean = true) => {
    const endpoint = isPatient
      ? `/api/v1/appointments/${id}/patient-check-in`
      : `/api/v1/appointments/${id}/staff-check-in`;
    const response = await api.post(endpoint);
    const result = unwrapResponse<any>(response.data);
    return transformAppointment(result);
  },

  complete: async (id: string) => {
    const response = await api.patch(`/api/v1/appointments/${id}/complete`);
    const result = unwrapResponse<any>(response.data);
    return transformAppointment(result);
  },
};

// ==================== PRESCRIPTIONS API ====================

export const prescriptionsApi = {
  create: async (data: any) => {
    const response = await api.post('/api/v1/prescriptions', data);
    return unwrapResponse(response.data);
  },

  getById: async (id: string) => {
    const response = await api.get(`/api/v1/prescriptions/${id}`);
    return unwrapResponse(response.data);
  },

  listForPatient: async (patientId: string) => {
    const response = await api.get(`/api/v1/prescriptions/patient/${patientId}`);
    const data = unwrapResponse<any[]>(response.data);
    // Ensure we always return an array
    return Array.isArray(data) ? data : [];
  },

  checkInteractions: async (data: any) => {
    const response = await api.post('/api/v1/prescriptions/interactions', data);
    return unwrapResponse(response.data);
  },
};

// ==================== PAYMENTS API ====================

export const paymentsApi = {
  initiate: async (data: any) => {
    const response = await api.post('/api/v1/payments/initiate', data);
    return unwrapResponse(response.data);
  },

  uploadReceipt: async (paymentId: string, receiptUrl: string) => {
    const response = await api.post(`/api/v1/payments/${paymentId}/receipt`, null, {
      params: { receiptUrl },
    });
    return unwrapResponse(response.data);
  },

  getReceipt: async (paymentId: string) => {
    const response = await api.get(`/api/v1/payments/${paymentId}/receipt`);
    return unwrapResponse(response.data);
  },

  verify: async (data: any) => {
    const response = await api.post('/api/v1/payments/verify', data);
    return unwrapResponse(response.data);
  },

  authorize: async (paymentId: string, notes?: string) => {
    const params = notes ? { notes } : {};
    const response = await api.post(`/api/v1/payments/${paymentId}/authorize`, null, { params });
    return unwrapResponse(response.data);
  },

  capture: async (paymentId: string, notes?: string) => {
    const params = notes ? { notes } : {};
    const response = await api.post(`/api/v1/payments/${paymentId}/capture`, null, { params });
    return unwrapResponse(response.data);
  },

  list: async (actorId?: string, isDoctorView: boolean = false) => {
    const params: any = { isDoctorView };
    if (actorId) params.actorId = actorId;
    const response = await api.get('/api/v1/payments', { params });
    const data = unwrapResponse<any[]>(response.data);
    // Ensure we always return an array
    return Array.isArray(data) ? data : [];
  },

  requestRefund: async (id: string) => {
    const response = await api.post(`/api/v1/payments/${id}/refund`);
    return unwrapResponse(response.data);
  },

  completeRefund: async (id: string, notes?: string) => {
    const params = notes ? { notes } : {};
    const response = await api.post(`/api/v1/payments/${id}/refund/complete`, null, { params });
    return unwrapResponse(response.data);
  },
};

// ==================== FACILITIES API ====================

export const facilitiesApi = {
  createForDoctor: async (doctorId: string, data: ClinicRequest) => {
    const response = await api.post(`/api/v1/facilities/doctor/${doctorId}`, data);
    return response.data;
  },

  createForOrganization: async (organizationId: string, data: ClinicRequest) => {
    const response = await api.post(`/api/v1/facilities/organization/${organizationId}`, data);
    return response.data;
  },

  listForDoctor: async (doctorId: string) => {
    const response = await api.get(`/api/v1/facilities/doctor/${doctorId}`);
    const data = unwrapResponse<Clinic[]>(response.data);
    // Ensure we always return an array
    return Array.isArray(data) ? data : [];
  },

  listForOrganization: async (organizationId: string) => {
    const response = await api.get(`/api/v1/facilities/organization/${organizationId}`);
    const data = unwrapResponse<Clinic[]>(response.data);
    // Ensure we always return an array
    return Array.isArray(data) ? data : [];
  },

  update: async (id: string, data: ClinicRequest) => {
    const response = await api.put(`/api/v1/facilities/${id}`, data);
    return response.data;
  },

  deactivate: async (id: string) => {
    const response = await api.delete(`/api/v1/facilities/${id}`);
    return response.data;
  },

  activate: async (id: string) => {
    const response = await api.post(`/api/v1/facilities/${id}/activate`);
    return response.data;
  },
};

// ==================== ANALYTICS API ====================

export const analyticsApi = {
  getDoctorAnalytics: async (doctorId?: string) => {
    const endpoint = doctorId ? `/api/v1/analytics/doctor/${doctorId}` : '/api/v1/analytics/doctor/me';
    const response = await api.get(endpoint);
    return response.data;
  },

  getPatientAnalytics: async (patientId?: string) => {
    const endpoint = patientId ? `/api/v1/analytics/patient/${patientId}` : '/api/v1/analytics/patient/me';
    const response = await api.get(endpoint);
    return response.data;
  },

  getOrganizationAnalytics: async (organizationId?: string) => {
    const endpoint = organizationId
      ? `/api/v1/analytics/organization/${organizationId}`
      : '/api/v1/analytics/organization/me';
    const response = await api.get(endpoint);
    return response.data;
  },
};

// ==================== MEDICAL RECORDS API ====================

export const medicalRecordsApi = {
  create: async (data: any) => {
    const response = await api.post('/api/v1/medical-records', data);
    return unwrapResponse<MedicalHistory>(response.data);
  },

  getById: async (id: string) => {
    const response = await api.get(`/api/v1/medical-records/${id}`);
    return unwrapResponse<MedicalHistory>(response.data);
  },

  listForPatient: async (patientId: string) => {
    const response = await api.get(`/api/v1/medical-records/patient/${patientId}`);
    const data = unwrapResponse<MedicalHistory[]>(response.data);
    // Ensure we always return an array
    return Array.isArray(data) ? data : [];
  },

  update: async (id: string, data: any) => {
    const response = await api.put(`/api/v1/medical-records/${id}`, data);
    return unwrapResponse<MedicalHistory>(response.data);
  },

  delete: async (id: string) => {
    await api.delete(`/api/v1/medical-records/${id}`);
  },

  exportPdf: async (id: string): Promise<Blob> => {
    const response = await api.get(`/api/v1/medical-records/${id}/export`, {
      responseType: 'blob',
    });
    return response.data;
  },
};

// ==================== VIDEO CALLS API ====================

export const videoCallsApi = {
  initiate: async (data: any) => {
    const response = await api.post('/api/v1/video-calls/initiate', data);
    return response.data;
  },

  getWebRTCToken: async (sessionId: string, userId: string) => {
    const response = await api.get('/api/v1/video-calls/webrtc-token', {
      params: { sessionId, userId },
    });
    return response.data;
  },

  getSession: async (appointmentId: string) => {
    const response = await api.get(`/api/v1/video-calls/${appointmentId}/session`);
    return response.data;
  },

  end: async (appointmentId: string) => {
    const response = await api.post(`/api/v1/video-calls/${appointmentId}/end`);
    return response.data;
  },
};

// ==================== DOCTOR API ====================

export const doctorApi = {
  signup: async (data: any): Promise<ApiResponse<string>> => {
    // Map frontend doctor signup fields to backend RegisterRequest
    const payload: any = {
      email: data.email,
      password: data.password,
      firstName: data.firstName,
      lastName: data.lastName,
      phoneNumber: data.phoneNumber,
      role: 'DOCTOR',
      specialization: data.specialization,
      // Backend expects `pmdcId` for doctors; map from our licenseNumber field
      pmdcId: data.licenseNumber || data.pmdcId,
    };
    return authApi.register(payload);
  },

  verifyOtp: async (data: OtpVerificationRequest): Promise<ApiResponse<User>> => {
    return authApi.verifyEmail(data.email, data.otp);
  },

  login: async (data: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
    return authApi.login(data);
  },

  getDashboard: async (doctorId: string) => {
    const response = await api.get(`/api/v1/doctors/${doctorId}/dashboard`);
    return response.data;
  },

  getRefundPolicy: async (doctorId: string) => {
    const response = await api.get(`/api/v1/doctors/${doctorId}/refund-policy`);
    return response.data;
  },

  // Clinics (using facilities API)
  getClinics: async (doctorId: string): Promise<Clinic[]> => {
    return facilitiesApi.listForDoctor(doctorId);
  },

  getClinicById: async (doctorId: string, clinicId: string): Promise<Clinic> => {
    const clinics = await facilitiesApi.listForDoctor(doctorId);
    return clinics.find((c: any) => c.id === clinicId) as Clinic;
  },

  createClinic: async (doctorId: string, data: ClinicRequest): Promise<Clinic> => {
    return facilitiesApi.createForDoctor(doctorId, data);
  },

  updateClinic: async (doctorId: string, clinicId: string, data: ClinicRequest): Promise<Clinic> => {
    return facilitiesApi.update(clinicId, data);
  },

  deleteClinic: async (doctorId: string, clinicId: string): Promise<void> => {
    return facilitiesApi.deactivate(clinicId);
  },

  toggleClinicStatus: async (doctorId: string, clinicId: string): Promise<Clinic> => {
    // Deactivate/reactivate by deleting/creating
    await facilitiesApi.deactivate(clinicId);
    const clinic = await doctorApi.getClinicById(doctorId, clinicId);
    return clinic;
  },

  // Appointments (using unified appointments API)
  getAppointments: async (status?: string): Promise<Appointment[]> => {
    return appointmentsApi.list(status);
  },

  confirmAppointment: async (appointmentId: string): Promise<Appointment> => {
    // In new backend, appointments are auto-confirmed after payment
    return appointmentsApi.getById(appointmentId);
  },

  rejectAppointment: async (appointmentId: string, reason?: string): Promise<Appointment> => {
    return appointmentsApi.cancel(appointmentId, reason || 'Rejected by doctor');
  },

  completeAppointment: async (appointmentId: string): Promise<Appointment> => {
    return appointmentsApi.complete(appointmentId);
  },

  checkIn: async (appointmentId: string): Promise<Appointment> => {
    return appointmentsApi.checkIn(appointmentId, false);
  },

  getZoomMeeting: async (appointmentId: string): Promise<ZoomMeeting> => {
    return videoCallsApi.getSession(appointmentId);
  },

  // Emergency Patient Management
  createEmergencyPatient: async (
    doctorId: string,
    data: CreateEmergencyPatientRequest,
  ): Promise<EmergencyPatientResponse> => {
    const response = await api.post(`/api/v1/doctors/${doctorId}/emergency/patient`, data);
    return response.data;
  },

  createEmergencyPatientAndAppointment: async (
    doctorId: string,
    data: CreateEmergencyPatientAndAppointmentRequest,
  ): Promise<EmergencyPatientAndAppointmentResponse> => {
    const response = await api.post(`/api/v1/doctors/${doctorId}/emergency/patient-and-appointment`, data);
    return response.data;
  },
};

// ==================== ADMIN API ====================

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

export const adminApi = {
  signup: async (data: SignupRequest): Promise<ApiResponse<string>> => {
    return authApi.register(data);
  },

  verifyOtp: async (data: OtpVerificationRequest): Promise<ApiResponse<User>> => {
    return authApi.verifyEmail(data.email, data.otp);
  },

  login: async (data: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
    return authApi.login(data);
  },

  getDashboard: async (): Promise<AdminDashboard> => {
    // Using analytics API for admin dashboard
    const response = await api.get('/api/v1/analytics/admin');
    return response.data;
  },

  // Patient Management
  getAllPatients: async (): Promise<User[]> => {
    const response = await api.get('/api/v1/users?role=PATIENT');
    return response.data;
  },

  getPatientById: async (patientId: string): Promise<User> => {
    const response = await api.get(`/api/v1/users/${patientId}`);
    return response.data;
  },

  deletePatient: async (patientId: string): Promise<void> => {
    await api.delete(`/api/v1/users/${patientId}`);
  },

  // Doctor Management
  getAllDoctors: async (): Promise<User[]> => {
    const response = await api.get('/api/v1/users?role=DOCTOR');
    return response.data;
  },

  getDoctorById: async (doctorId: string): Promise<User> => {
    const response = await api.get(`/api/v1/users/${doctorId}`);
    return response.data;
  },

  deleteDoctor: async (doctorId: string): Promise<void> => {
    await api.delete(`/api/v1/users/${doctorId}`);
  },

  // Admin Management
  getAllAdmins: async (): Promise<User[]> => {
    const response = await api.get('/api/v1/users?role=ADMIN');
    return response.data;
  },

  getAdminById: async (adminId: string): Promise<User> => {
    const response = await api.get(`/api/v1/users/${adminId}`);
    return response.data;
  },

  deleteAdmin: async (adminId: string): Promise<void> => {
    await api.delete(`/api/v1/users/${adminId}`);
  },

  // Appointment Management (using unified appointments API)
  getAllAppointments: async (status?: string): Promise<Appointment[]> => {
    return appointmentsApi.list(status);
  },

  getAppointmentById: async (appointmentId: string): Promise<Appointment> => {
    return appointmentsApi.getById(appointmentId);
  },

  deleteAppointment: async (appointmentId: string): Promise<void> => {
    await appointmentsApi.cancel(appointmentId, 'Cancelled by admin');
  },

  updateAppointmentStatus: async (appointmentId: string, status: string): Promise<Appointment> => {
    if (status === 'COMPLETED') {
      return appointmentsApi.complete(appointmentId);
    }
    return appointmentsApi.getById(appointmentId);
  },

  // Clinic Management (using facilities API)
  getAllClinics: async (doctorId?: string): Promise<Clinic[]> => {
    if (doctorId) {
      return facilitiesApi.listForDoctor(doctorId);
    }
    // Admin can list all facilities - would need a new endpoint
    const response = await api.get('/api/v1/facilities');
    return response.data;
  },

  getClinicById: async (clinicId: string): Promise<Clinic> => {
    const response = await api.get(`/api/v1/facilities/${clinicId}`);
    return response.data;
  },

  deleteClinic: async (clinicId: string): Promise<void> => {
    return facilitiesApi.deactivate(clinicId);
  },

  // Medical History Management (using medical records API)
  getAllMedicalHistories: async (patientId?: string): Promise<MedicalHistory[]> => {
    if (patientId) {
      return medicalRecordsApi.listForPatient(patientId);
    }
    // Admin can list all - would need a new endpoint or iterate through patients
    const response = await api.get('/api/v1/medical-records');
    return response.data;
  },

  getMedicalHistoryById: async (historyId: string): Promise<MedicalHistory> => {
    return medicalRecordsApi.getById(historyId);
  },

  deleteMedicalHistory: async (historyId: string): Promise<void> => {
    return medicalRecordsApi.delete(historyId);
  },

  // Admin Approval
  approveDoctor: async (doctorId: string) => {
    const response = await api.post(`/api/v1/admin/approvals/doctors/${doctorId}/approve`);
    return response.data;
  },

  rejectDoctor: async (doctorId: string, reason: string) => {
    const response = await api.post(`/api/v1/admin/approvals/doctors/${doctorId}/reject`, { reason });
    return response.data;
  },

  approveOrganization: async (organizationId: string) => {
    const response = await api.post(`/api/v1/admin/approvals/organizations/${organizationId}/approve`);
    return response.data;
  },

  rejectOrganization: async (organizationId: string, reason: string) => {
    const response = await api.post(`/api/v1/admin/approvals/organizations/${organizationId}/reject`, { reason });
    return response.data;
  },
};

export default api;

