export type AppointmentStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
export type AppointmentType = 'ONLINE' | 'ONSITE';
export type FacilityStatus = 'ACTIVE' | 'DEACTIVATED';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface User {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: 'doctor' | 'patient';
  avatar?: string;
}

export interface Doctor extends User {
  specialty: string;
  licenseNumber: string;
  pmdcNumber: string;
}

export interface Patient extends User {
  emergencyContact: {
    name: string;
    phone: string;
    relationship: string;
  };
}

export interface Facility {
  id: string;
  doctorId: string;
  name: string;
  status: FacilityStatus;
  phone: string;
  state: string;
  city: string;
  town: string;
  address: string;
  services: AppointmentType[];
  fee: number;
  hours: string;
}

export interface Appointment {
  id: string;
  doctorId: string;
  doctorName: string;
  patientId: string;
  patientName: string;
  facilityId: string;
  facilityName: string;
  type: AppointmentType;
  status: AppointmentStatus;
  date: string;
  time: string;
  fee: number;
  zoomStartUrl?: string;
  zoomJoinUrl?: string;
  notes?: string;
  hasReview?: boolean;
}

export interface Prescription {
  id: string;
  appointmentId: string;
  doctorName: string;
  facilityName: string;
  patientName: string;
  date: string;
  medications: Array<{
    name: string;
    dosage: string;
    frequency: string;
    duration: string;
  }>;
  diagnosis: string;
  notes?: string;
  warnings?: string[];
}

export interface Task {
  id: string;
  doctorId: string;
  title: string;
  description: string;
  dueDate: string;
  status: TaskStatus;
  priority: TaskPriority;
  relatedAppointmentId?: string;
  relatedPatientName?: string;
}

export interface MedicalHistory {
  id: string;
  patientId: string;
  appointmentId: string;
  date: string;
  doctorName: string;
  facilityName: string;
  type: AppointmentType;
  diagnosis: string;
  notes: string;
  prescriptionId?: string;
}

// Mock current user
export const mockDoctor: Doctor = {
  id: 'doc-1',
  name: 'Dr. Sarah Mitchell',
  email: 'sarah.mitchell@healthlink.com',
  phone: '+92 300 1234567',
  role: 'doctor',
  specialty: 'Cardiology',
  licenseNumber: 'MD-12345',
  pmdcNumber: 'PMDC-67890',
  avatar: 'https://i.pravatar.cc/150?img=5'
};

export const mockPatient: Patient = {
  id: 'pat-1',
  name: 'Ahmed Khan',
  email: 'ahmed.khan@email.com',
  phone: '+92 301 9876543',
  role: 'patient',
  avatar: 'https://i.pravatar.cc/150?img=12',
  emergencyContact: {
    name: 'Fatima Khan',
    phone: '+92 302 1234567',
    relationship: 'Spouse'
  }
};

// Mock facilities
export const mockFacilities: Facility[] = [
  {
    id: 'fac-1',
    doctorId: 'doc-1',
    name: 'Heart Care Clinic',
    status: 'ACTIVE',
    phone: '+92 21 1234567',
    state: 'Sindh',
    city: 'Karachi',
    town: 'Clifton',
    address: 'Plot 123, Block 5, Main Boulevard',
    services: ['ONLINE', 'ONSITE'],
    fee: 2500,
    hours: 'Mon-Fri: 9AM-5PM'
  },
  {
    id: 'fac-2',
    doctorId: 'doc-1',
    name: 'City Medical Center',
    status: 'ACTIVE',
    phone: '+92 21 7654321',
    state: 'Sindh',
    city: 'Karachi',
    town: 'Defence',
    address: 'Street 12, Phase 6',
    services: ['ONSITE'],
    fee: 3000,
    hours: 'Mon-Sat: 10AM-6PM'
  }
];

// Mock appointments
export const mockAppointments: Appointment[] = [
  {
    id: 'apt-1',
    doctorId: 'doc-1',
    doctorName: 'Dr. Sarah Mitchell',
    patientId: 'pat-2',
    patientName: 'Ali Hassan',
    facilityId: 'fac-1',
    facilityName: 'Heart Care Clinic',
    type: 'ONLINE',
    status: 'CONFIRMED',
    date: '2025-12-10',
    time: '10:00 AM',
    fee: 2500,
    zoomStartUrl: 'https://zoom.us/start/mock123',
    zoomJoinUrl: 'https://zoom.us/join/mock123'
  },
  {
    id: 'apt-2',
    doctorId: 'doc-1',
    doctorName: 'Dr. Sarah Mitchell',
    patientId: 'pat-3',
    patientName: 'Sana Ahmed',
    facilityId: 'fac-2',
    facilityName: 'City Medical Center',
    type: 'ONSITE',
    status: 'COMPLETED',
    date: '2025-12-05',
    time: '2:00 PM',
    fee: 3000,
    hasReview: false
  },
  {
    id: 'apt-3',
    doctorId: 'doc-1',
    doctorName: 'Dr. Sarah Mitchell',
    patientId: 'pat-4',
    patientName: 'Omar Farooq',
    facilityId: 'fac-1',
    facilityName: 'Heart Care Clinic',
    type: 'ONLINE',
    status: 'IN_PROGRESS',
    date: '2025-12-08',
    time: '11:30 AM',
    fee: 2500,
    zoomStartUrl: 'https://zoom.us/start/mock456',
    zoomJoinUrl: 'https://zoom.us/join/mock456'
  }
];

// Mock prescriptions
export const mockPrescriptions: Prescription[] = [
  {
    id: 'prx-1',
    appointmentId: 'apt-2',
    doctorName: 'Dr. Sarah Mitchell',
    facilityName: 'City Medical Center',
    patientName: 'Sana Ahmed',
    date: '2025-12-05',
    diagnosis: 'Hypertension',
    medications: [
      {
        name: 'Lisinopril',
        dosage: '10mg',
        frequency: 'Once daily',
        duration: '30 days'
      },
      {
        name: 'Aspirin',
        dosage: '81mg',
        frequency: 'Once daily',
        duration: '30 days'
      }
    ],
    notes: 'Monitor blood pressure regularly. Follow up in 2 weeks.',
    warnings: ['Take medication with food', 'Avoid grapefruit juice']
  }
];

// Mock tasks
export const mockTasks: Task[] = [
  {
    id: 'task-1',
    doctorId: 'doc-1',
    title: 'Review lab results for Ali Hassan',
    description: 'Check ECG and blood work results',
    dueDate: '2025-12-09',
    status: 'TODO',
    priority: 'HIGH',
    relatedAppointmentId: 'apt-1',
    relatedPatientName: 'Ali Hassan'
  },
  {
    id: 'task-2',
    doctorId: 'doc-1',
    title: 'Update medical records',
    description: 'Complete documentation for recent consultations',
    dueDate: '2025-12-11',
    status: 'IN_PROGRESS',
    priority: 'MEDIUM'
  }
];

// Mock medical history
export const mockMedicalHistory: MedicalHistory[] = [
  {
    id: 'mh-1',
    patientId: 'pat-1',
    appointmentId: 'apt-2',
    date: '2025-12-05',
    doctorName: 'Dr. Sarah Mitchell',
    facilityName: 'City Medical Center',
    type: 'ONSITE',
    diagnosis: 'Hypertension',
    notes: 'Patient presented with elevated blood pressure. Prescribed medication and advised lifestyle changes.',
    prescriptionId: 'prx-1'
  }
];

// Location data for cascading selects
export const locationData = {
  states: [
    { value: 'Punjab', label: 'Punjab' },
    { value: 'Sindh', label: 'Sindh' },
    { value: 'KPK', label: 'Khyber Pakhtunkhwa' },
    { value: 'Balochistan', label: 'Balochistan' }
  ],
  cities: {
    Punjab: [
      { value: 'Lahore', label: 'Lahore' },
      { value: 'Rawalpindi', label: 'Rawalpindi' },
      { value: 'Faisalabad', label: 'Faisalabad' }
    ],
    Sindh: [
      { value: 'Karachi', label: 'Karachi' },
      { value: 'Hyderabad', label: 'Hyderabad' }
    ],
    KPK: [
      { value: 'Peshawar', label: 'Peshawar' },
      { value: 'Abbottabad', label: 'Abbottabad' }
    ],
    Balochistan: [
      { value: 'Quetta', label: 'Quetta' },
      { value: 'Gwadar', label: 'Gwadar' }
    ]
  },
  towns: {
    Karachi: [
      { value: 'Clifton', label: 'Clifton' },
      { value: 'Defence', label: 'Defence' },
      { value: 'Gulshan', label: 'Gulshan' },
      { value: 'Nazimabad', label: 'Nazimabad' }
    ],
    Lahore: [
      { value: 'Gulberg', label: 'Gulberg' },
      { value: 'DHA', label: 'DHA' },
      { value: 'Johar Town', label: 'Johar Town' }
    ]
  }
};
