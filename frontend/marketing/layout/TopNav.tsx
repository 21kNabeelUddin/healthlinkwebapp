import Link from "next/link";
import { Activity, Bell, ChevronDown, LogOut, Settings, User } from "lucide-react";
import { useState } from "react";

interface TopNavProps {
  userName: string;
  userRole: "Patient" | "Doctor" | "Admin";
  showPortalLinks?: boolean;
  onLogout?: () => void;
}

export function TopNav({ userName, userRole, showPortalLinks = true, onLogout }: TopNavProps) {
  const [showDropdown, setShowDropdown] = useState(false);

  const handleLogout = () => {
    setShowDropdown(false);
    if (onLogout) {
      onLogout();
    } else {
      window.location.href = "/";
    }
  };

  const initials = userName?.[0]?.toUpperCase() ?? "U";

  return (
    <nav className="sticky top-0 z-50 bg-white/80 backdrop-blur-lg border-b border-slate-200 shadow-sm">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            <div className="w-10 h-10 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center">
              <Activity className="w-6 h-6 text-white" />
            </div>
            <span className="text-xl text-slate-900 hidden sm:inline">HealthLink+</span>
          </Link>

          {/* Center Nav Links */}
          {showPortalLinks && (
            <div className="hidden lg:flex items-center gap-6">
              <Link href="/patient" className="text-slate-600 hover:text-slate-900 transition text-sm">
                Patient Portal
              </Link>
              <Link href="/doctor" className="text-slate-600 hover:text-slate-900 transition text-sm">
                Doctor Portal
              </Link>
              <Link href="/admin" className="text-slate-600 hover:text-slate-900 transition text-sm">
                Admin Portal
              </Link>
            </div>
          )}

          {/* Right side */}
          <div className="flex items-center gap-4">
            {/* Notifications */}
            <button className="relative p-2 text-slate-600 hover:text-slate-900 transition">
              <Bell className="w-5 h-5" />
              <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
            </button>

            {/* User Menu */}
            <div className="relative">
              <button
                onClick={() => setShowDropdown(!showDropdown)}
                className="flex items-center gap-3 p-2 rounded-lg hover:bg-slate-100 transition"
              >
                <div className="w-8 h-8 bg-gradient-to-br from-teal-500 to-violet-600 rounded-full flex items-center justify-center">
                  <span className="text-white text-sm">{initials}</span>
                </div>
                <div className="hidden sm:block text-left">
                  <p className="text-sm text-slate-900">{userName}</p>
                  <p className="text-xs text-slate-600">{userRole}</p>
                </div>
                <ChevronDown className="w-4 h-4 text-slate-600" />
              </button>

              {/* Dropdown */}
              {showDropdown && (
                <div className="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-xl border border-slate-200 py-2">
                  <Link
                    href="#profile"
                    className="flex items-center gap-2 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
                  >
                    <User className="w-4 h-4" />
                    Profile
                  </Link>
                  <Link
                    href="#settings"
                    className="flex items-center gap-2 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
                  >
                    <Settings className="w-4 h-4" />
                    Settings
                  </Link>
                  <hr className="my-2 border-slate-200" />
                  <button
                    onClick={handleLogout}
                    className="flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50 w-full text-left"
                  >
                    <LogOut className="w-4 h-4" />
                    Logout
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
}
