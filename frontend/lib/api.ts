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
          const { accessToken, refreshToken: newRefreshToken } = response.data;
          localStorage.setItem('token', accessToken);
          if (newRefreshToken) {
            localStorage.setItem('refreshToken', newRefreshToken);
          }
          originalRequest.headers = originalRequest.headers || {};
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return api(originalRequest);
        } catch (refreshError) {
          localStorage.removeItem('token');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
          if (typeof window !== 'undefined') {
            window.location.href = '/auth/login';
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
    // Store tokens
    if (payload?.accessToken) {
      localStorage.setItem('token', payload.accessToken);
      if ((payload as any).refreshToken) {
        localStorage.setItem('refreshToken', (payload as any).refreshToken as string);
      }
      if ((payload as any).user) {
        localStorage.setItem('user', JSON.stringify((payload as any).user));
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

  refreshToken: async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) throw new Error('No refresh token');
    const response = await api.post('/api/v1/auth/refresh', null, {
      headers: { Authorization: `Bearer ${refreshToken}` },
    });
    if (response.data.accessToken) {
      localStorage.setItem('token', response.data.accessToken);
      if (response.data.refreshToken) {
        localStorage.setItem('refreshToken', response.data.refreshToken);
      }
    }
    return response.data;
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

  verifyOtp: async (data: OtpVerificationRequest): Promise<ApiResponse<User>> => {
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
    return response.data;
  },

  getById: async (id: string) => {
    const response = await api.get(`/api/v1/appointments/${id}`);
    return response.data;
  },

  create: async (data: any) => {
    const response = await api.post('/api/v1/appointments', data);
    return response.data;
  },

  reschedule: async (appointmentId: string, newStartTime: string) => {
    const response = await api.post('/api/v1/appointments/reschedule', {
      appointmentId,
      newStartTime,
    });
    return response.data;
  },

  cancel: async (id: string, reason?: string) => {
    const params = reason ? { reason } : {};
    const response = await api.post(`/api/v1/appointments/${id}/cancel`, null, { params });
    return response.data;
  },

  checkIn: async (id: string, isPatient: boolean = true) => {
    const endpoint = isPatient
      ? `/api/v1/appointments/${id}/patient-check-in`
      : `/api/v1/appointments/${id}/staff-check-in`;
    const response = await api.post(endpoint);
    return response.data;
  },

  complete: async (id: string) => {
    const response = await api.patch(`/api/v1/appointments/${id}/complete`);
    return response.data;
  },
};

// ==================== PRESCRIPTIONS API ====================

export const prescriptionsApi = {
  create: async (data: any) => {
    const response = await api.post('/api/v1/prescriptions', data);
    return response.data;
  },

  getById: async (id: string) => {
    const response = await api.get(`/api/v1/prescriptions/${id}`);
    return response.data;
  },

  listForPatient: async (patientId: string) => {
    const response = await api.get(`/api/v1/prescriptions/patient/${patientId}`);
    return response.data;
  },

  checkInteractions: async (data: any) => {
    const response = await api.post('/api/v1/prescriptions/interactions', data);
    return response.data;
  },
};

// ==================== PAYMENTS API ====================

export const paymentsApi = {
  initiate: async (data: any) => {
    const response = await api.post('/api/v1/payments/initiate', data);
    return response.data;
  },

  uploadReceipt: async (paymentId: string, receiptUrl: string) => {
    const response = await api.post(`/api/v1/payments/${paymentId}/receipt`, null, {
      params: { receiptUrl },
    });
    return response.data;
  },

  getReceipt: async (paymentId: string) => {
    const response = await api.get(`/api/v1/payments/${paymentId}/receipt`);
    return response.data;
  },

  verify: async (data: any) => {
    const response = await api.post('/api/v1/payments/verify', data);
    return response.data;
  },

  authorize: async (paymentId: string, notes?: string) => {
    const params = notes ? { notes } : {};
    const response = await api.post(`/api/v1/payments/${paymentId}/authorize`, null, { params });
    return response.data;
  },

  capture: async (paymentId: string, notes?: string) => {
    const params = notes ? { notes } : {};
    const response = await api.post(`/api/v1/payments/${paymentId}/capture`, null, { params });
    return response.data;
  },

  list: async (actorId?: string, isDoctorView: boolean = false) => {
    const params: any = { isDoctorView };
    if (actorId) params.actorId = actorId;
    const response = await api.get('/api/v1/payments', { params });
    return response.data;
  },

  requestRefund: async (id: string) => {
    const response = await api.post(`/api/v1/payments/${id}/refund`);
    return response.data;
  },

  completeRefund: async (id: string, notes?: string) => {
    const params = notes ? { notes } : {};
    const response = await api.post(`/api/v1/payments/${id}/refund/complete`, null, { params });
    return response.data;
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
    return response.data;
  },

  listForOrganization: async (organizationId: string) => {
    const response = await api.get(`/api/v1/facilities/organization/${organizationId}`);
    return response.data;
  },

  update: async (id: string, data: ClinicRequest) => {
    const response = await api.put(`/api/v1/facilities/${id}`, data);
    return response.data;
  },

  deactivate: async (id: string) => {
    const response = await api.delete(`/api/v1/facilities/${id}`);
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
    return response.data;
  },

  getById: async (id: string) => {
    const response = await api.get(`/api/v1/medical-records/${id}`);
    return response.data;
  },

  listForPatient: async (patientId: string) => {
    const response = await api.get(`/api/v1/medical-records/patient/${patientId}`);
    return response.data;
  },

  update: async (id: string, data: any) => {
    const response = await api.put(`/api/v1/medical-records/${id}`, data);
    return response.data;
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
  signup: async (data: SignupRequest): Promise<ApiResponse<string>> => {
    return authApi.register(data);
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

