'use client';

import Link from "next/link";
import { Button } from "./ui/button";
import { ImageWithFallback } from "./figma/ImageWithFallback";
import { 
  Shield, 
  Video, 
  Lock,
  Calendar,
  Users,
  ClipboardList,
  Building2,
  FileText,
  Activity
} from "lucide-react";

export function Hero() {
  return (
    <section className="relative overflow-hidden">
      {/* Navigation */}
      <nav className="container mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-10 h-10 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center">
              <Activity className="w-6 h-6 text-white" />
            </div>
            <span className="text-xl text-slate-900">HealthLink+</span>
          </div>
          
          <div className="hidden lg:flex items-center gap-8">
            <a href="#product" className="text-slate-600 hover:text-slate-900 transition">Product</a>
            <a href="#roles" className="text-slate-600 hover:text-slate-900 transition">Roles</a>
            <a href="#security" className="text-slate-600 hover:text-slate-900 transition">Security</a>
            <a href="#pricing" className="text-slate-600 hover:text-slate-900 transition">Pricing</a>
          </div>
          
          <div className="flex items-center gap-3">
            <Link href="/auth/login">
              <Button variant="ghost" className="hidden sm:inline-flex">
                Sign In
              </Button>
            </Link>
            <Link href="/auth/patient/signup">
              <Button className="bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700">
                Get Started
              </Button>
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero Content */}
      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-12 lg:py-20">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          {/* Left Content */}
          <div className="space-y-8">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white border border-teal-200 shadow-sm">
              <div className="w-2 h-2 bg-teal-500 rounded-full animate-pulse"></div>
              <span className="text-sm text-slate-700">Trusted by 10,000+ healthcare professionals</span>
            </div>

            <div className="space-y-6">
              <h1 className="text-4xl sm:text-5xl lg:text-6xl text-slate-900 leading-tight">
                One Platform For{" "}
                <span className="bg-gradient-to-r from-teal-500 to-violet-600 bg-clip-text text-transparent">
                  Patients, Doctors, and Admins
                </span>
              </h1>
              
              <p className="text-lg sm:text-xl text-slate-600 max-w-xl">
                Book appointments, manage clinics, track medical histories, and run admin dashboards from a single secure hub.
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-4">
              <Link href="/auth/login">
                <Button 
                  size="lg" 
                  className="bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700 shadow-lg shadow-teal-500/30 text-lg h-14"
                >
                  Explore HealthLink+
                </Button>
              </Link>
              <Link href="/patient">
                <Button 
                  size="lg" 
                  variant="outline" 
                  className="border-slate-300 hover:border-slate-400 text-lg h-14"
                >
                  See Live Demo
                </Button>
              </Link>
            </div>

            {/* Trust Indicators */}
            <div className="flex flex-wrap items-center gap-6 pt-4">
              <div className="flex items-center gap-2">
                <Shield className="w-5 h-5 text-teal-600" />
                <span className="text-sm text-slate-700">HIPAA-ready</span>
              </div>
              <div className="flex items-center gap-2">
                <Video className="w-5 h-5 text-teal-600" />
                <span className="text-sm text-slate-700">Secure Zoom integration</span>
              </div>
              <div className="flex items-center gap-2">
                <Lock className="w-5 h-5 text-teal-600" />
                <span className="text-sm text-slate-700">Real-time OTP verification</span>
              </div>
            </div>
          </div>

          {/* Right - Dashboard Mockup */}
          <div className="relative">
            <div className="absolute -inset-4 bg-gradient-to-r from-teal-500/20 to-violet-600/20 blur-3xl"></div>
            
            <div className="relative bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden">
              {/* Dashboard Header */}
              <div className="bg-gradient-to-r from-slate-900 to-slate-800 px-6 py-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 bg-gradient-to-br from-teal-500 to-violet-600 rounded-lg"></div>
                  <span className="text-white">HealthLink+ Dashboard</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                  <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                </div>
              </div>

              {/* Dashboard Content */}
              <div className="flex">
                {/* Sidebar */}
                <div className="w-20 bg-slate-50 border-r border-slate-200 py-6 flex flex-col items-center gap-6">
                  <div className="w-10 h-10 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center">
                    <Users className="w-5 h-5 text-white" />
                  </div>
                  <div className="w-10 h-10 bg-slate-200 rounded-xl flex items-center justify-center">
                    <Calendar className="w-5 h-5 text-slate-600" />
                  </div>
                  <div className="w-10 h-10 bg-slate-200 rounded-xl flex items-center justify-center">
                    <ClipboardList className="w-5 h-5 text-slate-600" />
                  </div>
                  <div className="w-10 h-10 bg-slate-200 rounded-xl flex items-center justify-center">
                    <Building2 className="w-5 h-5 text-slate-600" />
                  </div>
                  <div className="w-10 h-10 bg-slate-200 rounded-xl flex items-center justify-center">
                    <FileText className="w-5 h-5 text-slate-600" />
                  </div>
                </div>

                {/* Main Panel */}
                <div className="flex-1 p-6 space-y-4">
                  <div>
                    <h3 className="text-sm text-slate-900 mb-4">Upcoming Appointments</h3>
                    
                    {/* Appointment Card 1 */}
                    <div className="bg-gradient-to-br from-teal-50 to-violet-50 rounded-xl p-4 mb-3 border border-teal-200">
                      <div className="flex items-start justify-between mb-3">
                        <div>
                          <p className="text-sm text-slate-900">Dr. Sarah Johnson</p>
                          <p className="text-xs text-slate-600">Cardiology Consultation</p>
                        </div>
                        <span className="text-xs px-2 py-1 bg-green-100 text-green-700 rounded">Online</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-slate-600">Today, 2:00 PM</span>
                        <Button size="sm" className="h-7 text-xs bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700">
                          <Video className="w-3 h-3 mr-1" />
                          Join Zoom
                        </Button>
                      </div>
                    </div>

                    {/* Appointment Card 2 */}
                    <div className="bg-white rounded-xl p-4 border border-slate-200">
                      <div className="flex items-start justify-between mb-3">
                        <div>
                          <p className="text-sm text-slate-900">Dr. Michael Chen</p>
                          <p className="text-xs text-slate-600">General Checkup</p>
                        </div>
                        <span className="text-xs px-2 py-1 bg-blue-100 text-blue-700 rounded">On-site</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-slate-600">Tomorrow, 10:00 AM</span>
                        <span className="text-xs text-slate-500">Clinic A</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}