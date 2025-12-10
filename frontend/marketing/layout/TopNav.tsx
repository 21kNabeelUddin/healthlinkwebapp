import Link from "next/link";
import { Activity, Bell, ChevronDown, LogOut, Settings, User, Search, X, Command } from "lucide-react";
import { useState, useEffect, useRef } from "react";

interface TopNavProps {
  userName: string;
  userRole: "Patient" | "Doctor" | "Admin";
  showPortalLinks?: boolean;
  onLogout?: () => void;
  onGlobalSearch?: (query: string) => void;
  notifications?: Array<{ id: string; title: string; message: string; time: string; read: boolean }>;
  onNotificationClick?: (id: string) => void;
}

export function TopNav({ 
  userName, 
  userRole, 
  showPortalLinks = true, 
  onLogout,
  onGlobalSearch,
  notifications = [],
  onNotificationClick
}: TopNavProps) {
  const [showDropdown, setShowDropdown] = useState(false);
  const [showSearch, setShowSearch] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const searchRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setShowSearch(true);
        setTimeout(() => searchRef.current?.focus(), 0);
      }
      if (e.key === 'Escape') {
        setShowSearch(false);
        setShowNotifications(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const unreadCount = notifications.filter(n => !n.read).length;

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

          {/* Center: Global Search (for admin) */}
          {userRole === "Admin" && (
            <div className="hidden md:flex flex-1 max-w-md mx-8">
              {showSearch ? (
                <div className="relative w-full">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-slate-400" />
                  <input
                    ref={searchRef}
                    type="text"
                    value={searchQuery}
                    onChange={(e) => {
                      setSearchQuery(e.target.value);
                      onGlobalSearch?.(e.target.value);
                    }}
                    placeholder="Search users, appointments, settings... (Press ESC to close)"
                    className="w-full pl-10 pr-10 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                  />
                  <button
                    onClick={() => {
                      setShowSearch(false);
                      setSearchQuery("");
                    }}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2"
                  >
                    <X className="w-4 h-4 text-slate-400" />
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setShowSearch(true)}
                  className="flex items-center gap-2 px-4 py-2 w-full text-left text-slate-600 bg-slate-100 rounded-lg hover:bg-slate-200 transition"
                >
                  <Search className="w-4 h-4" />
                  <span className="text-sm">Search...</span>
                  <kbd className="ml-auto hidden lg:inline-flex items-center gap-1 px-2 py-1 text-xs font-semibold text-slate-500 bg-white border border-slate-300 rounded">
                    <Command className="w-3 h-3" />K
                  </kbd>
                </button>
              )}
            </div>
          )}

          {/* Right side */}
          <div className="flex items-center gap-4">
            {/* Notifications */}
            <div className="relative">
              <button 
                onClick={() => setShowNotifications(!showNotifications)}
                className="relative p-2 text-slate-600 hover:text-slate-900 transition"
              >
                <Bell className="w-5 h-5" />
                {unreadCount > 0 && (
                  <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
                )}
              </button>
              
              {showNotifications && (
                <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-xl border border-slate-200 py-2 max-h-96 overflow-y-auto z-50">
                  <div className="px-4 py-2 border-b border-slate-200">
                    <h3 className="font-semibold text-slate-900">Notifications</h3>
                  </div>
                  {notifications.length === 0 ? (
                    <div className="px-4 py-8 text-center text-slate-500 text-sm">
                      No notifications
                    </div>
                  ) : (
                    <div className="py-2">
                      {notifications.slice(0, 5).map((notif) => (
                        <button
                          key={notif.id}
                          onClick={() => {
                            onNotificationClick?.(notif.id);
                            setShowNotifications(false);
                          }}
                          className={`w-full px-4 py-3 text-left hover:bg-slate-50 transition ${
                            !notif.read ? 'bg-blue-50' : ''
                          }`}
                        >
                          <p className="text-sm font-medium text-slate-900">{notif.title}</p>
                          <p className="text-xs text-slate-600 mt-1">{notif.message}</p>
                          <p className="text-xs text-slate-400 mt-1">{notif.time}</p>
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>

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
                <div className="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-xl border border-slate-200 py-2 z-50">
                  <div className="px-4 py-2 border-b border-slate-200">
                    <p className="text-sm font-medium text-slate-900">{userName}</p>
                    <p className="text-xs text-slate-600">{userRole}</p>
                  </div>
                  <Link
                    href="#profile"
                    onClick={() => setShowDropdown(false)}
                    className="flex items-center gap-2 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
                  >
                    <User className="w-4 h-4" />
                    Profile
                  </Link>
                  <Link
                    href={userRole === "Admin" ? "/admin/settings" : "#settings"}
                    onClick={() => setShowDropdown(false)}
                    className="flex items-center gap-2 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
                  >
                    <Settings className="w-4 h-4" />
                    Settings
                  </Link>
                  {userRole === "Admin" && (
                    <>
                      <hr className="my-2 border-slate-200" />
                      <Link
                        href="/admin/dashboard"
                        onClick={() => setShowDropdown(false)}
                        className="flex items-center gap-2 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
                      >
                        <Activity className="w-4 h-4" />
                        Dashboard
                      </Link>
                    </>
                  )}
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
