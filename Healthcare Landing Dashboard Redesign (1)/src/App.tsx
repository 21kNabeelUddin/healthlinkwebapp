import { LandingPage } from "./pages/LandingPage";
import { PatientDashboard } from "./pages/PatientDashboard";
import { DoctorDashboard } from "./pages/DoctorDashboard";
import { AdminDashboard } from "./pages/AdminDashboard";

export default function App() {
  // Simple routing based on pathname
  const path = window.location.pathname;

  if (path === "/patient") {
    return <PatientDashboard />;
  }

  if (path === "/doctor") {
    return <DoctorDashboard />;
  }

  if (path === "/admin") {
    return <AdminDashboard />;
  }

  // Default to landing page
  return <LandingPage />;
}
