import React, { useState } from 'react';
import { 
  Calendar, 
  Building2, 
  FileText, 
  CheckSquare, 
  User, 
  LogOut,
  Menu,
  X,
  Activity,
  Clock
} from 'lucide-react';

interface NavItem {
  label: string;
  icon: React.ReactNode;
  path: string;
}

interface DashboardLayoutProps {
  children: React.ReactNode;
  role: 'doctor' | 'patient';
  currentPage: string;
  onNavigate: (page: string) => void;
  userName: string;
  userAvatar?: string;
}

export function DashboardLayout({ 
  children, 
  role, 
  currentPage, 
  onNavigate, 
  userName,
  userAvatar 
}: DashboardLayoutProps) {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const doctorNav: NavItem[] = [
    { label: 'Appointments', icon: <Calendar className="w-5 h-5" />, path: 'appointments' },
    { label: 'Clinics', icon: <Building2 className="w-5 h-5" />, path: 'clinics' },
    { label: 'Tasks', icon: <CheckSquare className="w-5 h-5" />, path: 'tasks' },
    { label: 'Profile', icon: <User className="w-5 h-5" />, path: 'profile' }
  ];

  const patientNav: NavItem[] = [
    { label: 'Appointments', icon: <Calendar className="w-5 h-5" />, path: 'appointments' },
    { label: 'Prescriptions', icon: <FileText className="w-5 h-5" />, path: 'prescriptions' },
    { label: 'Medical History', icon: <Activity className="w-5 h-5" />, path: 'medical-history' },
    { label: 'Profile', icon: <User className="w-5 h-5" />, path: 'profile' }
  ];

  const navItems = role === 'doctor' ? doctorNav : patientNav;

  return (
    <div className="min-h-screen bg-neutral-50">
      {/* Top Bar */}
      <div className="sticky top-0 z-40 bg-white border-b border-neutral-200">
        <div className="flex items-center justify-between px-4 lg:px-6 h-16">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="lg:hidden p-2 text-neutral-600 hover:bg-neutral-100 rounded-lg"
            >
              {isMobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 bg-gradient-to-br from-primary-600 to-secondary-600 rounded-lg flex items-center justify-center">
                <Activity className="w-5 h-5 text-white" />
              </div>
              <h1 className="text-neutral-900">HealthLink+</h1>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="hidden md:flex items-center gap-3 px-3 py-2 bg-neutral-50 rounded-lg">
              {userAvatar ? (
                <img src={userAvatar} alt={userName} className="w-8 h-8 rounded-full" />
              ) : (
                <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center">
                  <User className="w-4 h-4 text-primary-700" />
                </div>
              )}
              <div className="text-sm">
                <p className="text-neutral-900">{userName}</p>
                <p className="text-neutral-500 capitalize">{role}</p>
              </div>
            </div>
            <button
              onClick={() => onNavigate('logout')}
              className="p-2 text-neutral-600 hover:bg-neutral-100 rounded-lg transition-colors"
              title="Logout"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu */}
      {isMobileMenuOpen && (
        <div className="lg:hidden fixed inset-0 z-30 bg-black/30 backdrop-blur-sm" onClick={() => setIsMobileMenuOpen(false)}>
          <div className="bg-white w-64 h-full shadow-xl" onClick={e => e.stopPropagation()}>
            <nav className="p-4 space-y-1">
              {navItems.map((item) => (
                <button
                  key={item.path}
                  onClick={() => {
                    onNavigate(item.path);
                    setIsMobileMenuOpen(false);
                  }}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                    currentPage === item.path
                      ? 'bg-primary-50 text-primary-700'
                      : 'text-neutral-700 hover:bg-neutral-100'
                  }`}
                >
                  {item.icon}
                  <span>{item.label}</span>
                </button>
              ))}
            </nav>
          </div>
        </div>
      )}

      <div className="flex">
        {/* Desktop Sidebar */}
        <div className="hidden lg:block w-64 bg-white border-r border-neutral-200 min-h-[calc(100vh-4rem)] sticky top-16">
          <nav className="p-4 space-y-1">
            {navItems.map((item) => (
              <button
                key={item.path}
                onClick={() => onNavigate(item.path)}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                  currentPage === item.path
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-neutral-700 hover:bg-neutral-100'
                }`}
              >
                {item.icon}
                <span>{item.label}</span>
              </button>
            ))}
          </nav>
        </div>

        {/* Main Content */}
        <div className="flex-1 overflow-auto">
          <div className="max-w-7xl mx-auto p-4 lg:p-6">
            {children}
          </div>
        </div>
      </div>
    </div>
  );
}
