import React, { useState } from 'react';
import { DashboardLayout } from './components/shared/DashboardLayout';
import { ToastContainer } from './components/ui/Toast';

// Doctor Components
import { DoctorAppointments } from './components/doctor/DoctorAppointments';
import { DoctorClinics } from './components/doctor/DoctorClinics';
import { DoctorClinicForm } from './components/doctor/DoctorClinicForm';
import { DoctorTasks } from './components/doctor/DoctorTasks';
import { DoctorProfile } from './components/doctor/DoctorProfile';

// Patient Components
import { PatientAppointments } from './components/patient/PatientAppointments';
import { PatientPrescriptions } from './components/patient/PatientPrescriptions';
import { PatientMedicalHistory } from './components/patient/PatientMedicalHistory';
import { PatientProfile } from './components/patient/PatientProfile';

import { mockDoctor, mockPatient, type Facility } from './utils/mockData';
import './styles/globals.css';

type UserRole = 'doctor' | 'patient';
type ToastType = 'success' | 'error' | 'info' | 'warning';

interface Toast {
  id: string;
  type: ToastType;
  message: string;
}

export default function App() {
  // Toggle between doctor and patient portals (in production, this would be based on auth)
  const [userRole, setUserRole] = useState<UserRole>('doctor');
  const [currentPage, setCurrentPage] = useState('appointments');
  const [toasts, setToasts] = useState<Toast[]>([]);
  
  // Clinic form state
  const [showClinicForm, setShowClinicForm] = useState(false);
  const [editingFacility, setEditingFacility] = useState<Facility | undefined>();

  const addToast = (type: ToastType, message: string) => {
    const id = Date.now().toString();
    setToasts(prev => [...prev, { id, type, message }]);
  };

  const removeToast = (id: string) => {
    setToasts(prev => prev.filter(toast => toast.id !== id));
  };

  const handleNavigate = (page: string) => {
    if (page === 'logout') {
      // Toggle between doctor and patient for demo purposes
      setUserRole(prev => prev === 'doctor' ? 'patient' : 'doctor');
      setCurrentPage('appointments');
      addToast('info', `Switched to ${userRole === 'doctor' ? 'patient' : 'doctor'} portal`);
    } else {
      setCurrentPage(page);
      setShowClinicForm(false);
      setEditingFacility(undefined);
    }
  };

  const handleCreateClinic = () => {
    setEditingFacility(undefined);
    setShowClinicForm(true);
  };

  const handleEditClinic = (facility: Facility) => {
    setEditingFacility(facility);
    setShowClinicForm(true);
  };

  const handleClinicFormBack = () => {
    setShowClinicForm(false);
    setEditingFacility(undefined);
    setCurrentPage('clinics');
  };

  const handleClinicFormSuccess = () => {
    setShowClinicForm(false);
    setEditingFacility(undefined);
    setCurrentPage('clinics');
  };

  const currentUser = userRole === 'doctor' ? mockDoctor : mockPatient;

  const renderDoctorPage = () => {
    if (currentPage === 'clinics' && showClinicForm) {
      return (
        <DoctorClinicForm
          facility={editingFacility}
          onBack={handleClinicFormBack}
          onSuccess={handleClinicFormSuccess}
          onToast={addToast}
        />
      );
    }

    switch (currentPage) {
      case 'appointments':
        return <DoctorAppointments />;
      case 'clinics':
        return (
          <DoctorClinics
            onCreateClinic={handleCreateClinic}
            onEditClinic={handleEditClinic}
          />
        );
      case 'tasks':
        return <DoctorTasks />;
      case 'profile':
        return <DoctorProfile onToast={addToast} />;
      default:
        return <DoctorAppointments />;
    }
  };

  const renderPatientPage = () => {
    switch (currentPage) {
      case 'appointments':
        return <PatientAppointments />;
      case 'prescriptions':
        return <PatientPrescriptions />;
      case 'medical-history':
        return <PatientMedicalHistory />;
      case 'profile':
        return <PatientProfile onToast={addToast} />;
      default:
        return <PatientAppointments />;
    }
  };

  return (
    <>
      <DashboardLayout
        role={userRole}
        currentPage={currentPage}
        onNavigate={handleNavigate}
        userName={currentUser.name}
        userAvatar={currentUser.avatar}
      >
        {userRole === 'doctor' ? renderDoctorPage() : renderPatientPage()}
      </DashboardLayout>

      <ToastContainer toasts={toasts} removeToast={removeToast} />

      {/* Demo toggle button */}
      <button
        onClick={() => {
          setUserRole(prev => prev === 'doctor' ? 'patient' : 'doctor');
          setCurrentPage('appointments');
          addToast('info', `Switched to ${userRole === 'doctor' ? 'patient' : 'doctor'} portal`);
        }}
        className="fixed bottom-6 right-6 px-4 py-2 bg-gradient-to-br from-primary-600 to-secondary-600 text-white rounded-lg shadow-lg hover:shadow-xl transition-shadow z-40"
      >
        Switch to {userRole === 'doctor' ? 'Patient' : 'Doctor'} Portal
      </button>
    </>
  );
}
