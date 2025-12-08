import {
  mockDoctor,
  mockPatient,
  mockFacilities,
  mockAppointments,
  mockPrescriptions,
  mockTasks,
  mockMedicalHistory,
  type Appointment,
  type Facility,
  type Prescription,
  type Task,
  type MedicalHistory,
  type User
} from './mockData';

// Simulate API delay
const delay = (ms: number = 500) => new Promise(resolve => setTimeout(resolve, ms));

// Auth API
export const authApi = {
  async me(role: 'doctor' | 'patient'): Promise<User> {
    await delay();
    return role === 'doctor' ? mockDoctor : mockPatient;
  },
  
  async changePassword(oldPassword: string, newPassword: string): Promise<{ success: boolean }> {
    await delay();
    return { success: true };
  }
};

// Appointments API
export const appointmentsApi = {
  async listForDoctor(doctorId: string, filters?: any): Promise<Appointment[]> {
    await delay();
    let appointments = mockAppointments.filter(apt => apt.doctorId === doctorId);
    
    if (filters?.status) {
      appointments = appointments.filter(apt => apt.status === filters.status);
    }
    if (filters?.facilityId) {
      appointments = appointments.filter(apt => apt.facilityId === filters.facilityId);
    }
    if (filters?.search) {
      const search = filters.search.toLowerCase();
      appointments = appointments.filter(apt => 
        apt.patientName.toLowerCase().includes(search)
      );
    }
    
    return appointments;
  },
  
  async listForPatient(patientId: string): Promise<Appointment[]> {
    await delay();
    return mockAppointments.filter(apt => apt.patientId === patientId);
  },
  
  async complete(appointmentId: string): Promise<{ success: boolean }> {
    await delay();
    const appointment = mockAppointments.find(apt => apt.id === appointmentId);
    if (appointment) {
      appointment.status = 'COMPLETED';
    }
    return { success: true };
  },
  
  async getZoomJoinUrl(appointmentId: string): Promise<string> {
    await delay();
    const appointment = mockAppointments.find(apt => apt.id === appointmentId);
    return appointment?.zoomJoinUrl || '';
  }
};

// Facilities API
export const facilitiesApi = {
  async listForDoctor(doctorId: string, filters?: any): Promise<Facility[]> {
    await delay();
    let facilities = mockFacilities.filter(fac => fac.doctorId === doctorId);
    
    if (filters?.status) {
      facilities = facilities.filter(fac => fac.status === filters.status);
    }
    if (filters?.state) {
      facilities = facilities.filter(fac => fac.state === filters.state);
    }
    
    return facilities;
  },
  
  async create(facility: Partial<Facility>): Promise<Facility> {
    await delay();
    const newFacility: Facility = {
      id: `fac-${Date.now()}`,
      doctorId: mockDoctor.id,
      name: facility.name || '',
      status: facility.status || 'ACTIVE',
      phone: facility.phone || '',
      state: facility.state || '',
      city: facility.city || '',
      town: facility.town || '',
      address: facility.address || '',
      services: facility.services || [],
      fee: facility.fee || 0,
      hours: facility.hours || ''
    };
    mockFacilities.push(newFacility);
    return newFacility;
  },
  
  async update(facilityId: string, updates: Partial<Facility>): Promise<Facility> {
    await delay();
    const facility = mockFacilities.find(fac => fac.id === facilityId);
    if (facility) {
      Object.assign(facility, updates);
    }
    return facility!;
  },
  
  async toggleStatus(facilityId: string): Promise<Facility> {
    await delay();
    const facility = mockFacilities.find(fac => fac.id === facilityId);
    if (facility) {
      facility.status = facility.status === 'ACTIVE' ? 'DEACTIVATED' : 'ACTIVE';
    }
    return facility!;
  }
};

// Prescriptions API
export const prescriptionsApi = {
  async getByAppointment(appointmentId: string): Promise<Prescription | null> {
    await delay();
    return mockPrescriptions.find(prx => prx.appointmentId === appointmentId) || null;
  },
  
  async listForPatient(patientId: string): Promise<Prescription[]> {
    await delay();
    return mockPrescriptions;
  },
  
  async getDetail(prescriptionId: string): Promise<Prescription | null> {
    await delay();
    return mockPrescriptions.find(prx => prx.id === prescriptionId) || null;
  }
};

// Tasks API
export const tasksApi = {
  async listForDoctor(doctorId: string, filters?: any): Promise<Task[]> {
    await delay();
    let tasks = mockTasks.filter(task => task.doctorId === doctorId);
    
    if (filters?.status) {
      tasks = tasks.filter(task => task.status === filters.status);
    }
    
    return tasks;
  },
  
  async create(task: Partial<Task>): Promise<Task> {
    await delay();
    const newTask: Task = {
      id: `task-${Date.now()}`,
      doctorId: mockDoctor.id,
      title: task.title || '',
      description: task.description || '',
      dueDate: task.dueDate || '',
      status: task.status || 'TODO',
      priority: task.priority || 'MEDIUM',
      relatedAppointmentId: task.relatedAppointmentId,
      relatedPatientName: task.relatedPatientName
    };
    mockTasks.push(newTask);
    return newTask;
  },
  
  async update(taskId: string, updates: Partial<Task>): Promise<Task> {
    await delay();
    const task = mockTasks.find(t => t.id === taskId);
    if (task) {
      Object.assign(task, updates);
    }
    return task!;
  }
};

// Medical History API
export const medicalHistoryApi = {
  async list(patientId: string, filters?: any): Promise<MedicalHistory[]> {
    await delay();
    return mockMedicalHistory.filter(mh => mh.patientId === patientId);
  }
};

// Reviews API
export const reviewsApi = {
  async create(appointmentId: string, rating: number, comment: string): Promise<{ success: boolean }> {
    await delay();
    const appointment = mockAppointments.find(apt => apt.id === appointmentId);
    if (appointment) {
      appointment.hasReview = true;
    }
    return { success: true };
  },
  
  async getByAppointmentId(appointmentId: string): Promise<any> {
    await delay();
    const appointment = mockAppointments.find(apt => apt.id === appointmentId);
    return appointment?.hasReview ? { rating: 5, comment: 'Great service' } : null;
  }
};

// Doctor API
export const doctorApi = {
  async updateProfile(updates: any): Promise<User> {
    await delay();
    Object.assign(mockDoctor, updates);
    return mockDoctor;
  }
};

// Patient API
export const patientApi = {
  async updateProfile(updates: any): Promise<User> {
    await delay();
    Object.assign(mockPatient, updates);
    return mockPatient;
  }
};
