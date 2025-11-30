import { LucideIcon } from "lucide-react";
import { useState } from "react";

interface SidebarItem {
  icon: LucideIcon;
  label: string;
  href: string;
  badge?: number;
}

interface SidebarProps {
  items: SidebarItem[];
  currentPath: string;
}

export function Sidebar({ items, currentPath }: SidebarProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);

  return (
    <>
      {/* Mobile overlay */}
      <div className="lg:hidden fixed inset-0 bg-slate-900/50 z-40 hidden" id="sidebar-overlay"></div>

      {/* Sidebar */}
      <aside
        className={`fixed lg:sticky top-16 left-0 h-[calc(100vh-4rem)] bg-white border-r border-slate-200 transition-all duration-300 z-40 ${
          isCollapsed ? "w-20" : "w-64"
        }`}
      >
        <div className="flex flex-col h-full">
          {/* Navigation */}
          <nav className="flex-1 p-4 space-y-2 overflow-y-auto">
            {items.map((item, index) => {
              const Icon = item.icon;
              const isActive = currentPath === item.href;

              return (
                <a
                  key={index}
                  href={item.href}
                  className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all ${
                    isActive
                      ? "bg-gradient-to-r from-teal-500 to-violet-600 text-white shadow-lg"
                      : "text-slate-700 hover:bg-slate-100"
                  }`}
                >
                  <Icon className="w-5 h-5 flex-shrink-0" />
                  {!isCollapsed && (
                    <>
                      <span className="flex-1 text-sm">{item.label}</span>
                      {item.badge && (
                        <span
                          className={`px-2 py-0.5 text-xs rounded-full ${
                            isActive
                              ? "bg-white/20 text-white"
                              : "bg-teal-100 text-teal-700"
                          }`}
                        >
                          {item.badge}
                        </span>
                      )}
                    </>
                  )}
                </a>
              );
            })}
          </nav>

          {/* Collapse toggle */}
          <div className="p-4 border-t border-slate-200">
            <button
              onClick={() => setIsCollapsed(!isCollapsed)}
              className="hidden lg:flex items-center justify-center w-full py-2 text-sm text-slate-600 hover:text-slate-900 transition"
            >
              <svg
                className={`w-5 h-5 transition-transform ${isCollapsed ? "rotate-180" : ""}`}
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M11 19l-7-7 7-7m8 14l-7-7 7-7"
                />
              </svg>
            </button>
          </div>
        </div>
      </aside>
    </>
  );
}
