'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { analyticsApi } from '@/lib/api';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import { TrendingUp, DollarSign, Star, Users, Calendar } from 'lucide-react';

interface DoctorAnalytics {
  totalRevenue: number;
  averageRating: number;
  totalReviews: number;
  totalAppointments: number;
  completedAppointments: number;
  cancelledAppointments: number;
  revenueByPeriod?: Array<{
    period: string;
    revenue: number;
  }>;
}

export default function DoctorAnalyticsPage() {
  const { user } = useAuth();
  const [analytics, setAnalytics] = useState<DoctorAnalytics | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (user?.id) {
      loadAnalytics();
    }
  }, [user?.id]);

  const loadAnalytics = async () => {
    setIsLoading(true);
    try {
      const data = await analyticsApi.getDoctorAnalytics(user?.id?.toString());
      setAnalytics(data);
    } catch (error: any) {
      toast.error('Failed to load analytics');
      console.error('Analytics load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="DOCTOR">
        <div className="text-center py-8">Loading analytics...</div>
      </DashboardLayout>
    );
  }

  if (!analytics) {
    return (
      <DashboardLayout requiredUserType="DOCTOR">
        <div className="text-center py-8">
          <p className="text-slate-500">No analytics data available</p>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Analytics & Insights</h1>
          <p className="text-slate-600 mt-1">Track your performance and revenue</p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 bg-gradient-to-br from-green-500 to-emerald-500 rounded-xl flex items-center justify-center">
                <DollarSign className="w-6 h-6 text-white" />
              </div>
            </div>
            <div>
              <p className="text-3xl font-bold text-slate-900 mb-1">
                PKR {analytics.totalRevenue.toFixed(2)}
              </p>
              <p className="text-sm text-slate-600">Total Revenue</p>
            </div>
          </Card>

          <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 bg-gradient-to-br from-yellow-500 to-amber-500 rounded-xl flex items-center justify-center">
                <Star className="w-6 h-6 text-white" />
              </div>
            </div>
            <div>
              <p className="text-3xl font-bold text-slate-900 mb-1">
                {analytics.averageRating.toFixed(1)}
              </p>
              <p className="text-sm text-slate-600">
                Average Rating ({analytics.totalReviews} reviews)
              </p>
            </div>
          </Card>

          <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-xl flex items-center justify-center">
                <Calendar className="w-6 h-6 text-white" />
              </div>
            </div>
            <div>
              <p className="text-3xl font-bold text-slate-900 mb-1">
                {analytics.totalAppointments}
              </p>
              <p className="text-sm text-slate-600">Total Appointments</p>
            </div>
          </Card>

          <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 bg-gradient-to-br from-violet-500 to-purple-500 rounded-xl flex items-center justify-center">
                <Users className="w-6 h-6 text-white" />
              </div>
            </div>
            <div>
              <p className="text-3xl font-bold text-slate-900 mb-1">
                {analytics.completedAppointments}
              </p>
              <p className="text-sm text-slate-600">Completed</p>
            </div>
          </Card>
        </div>

        <div className="grid lg:grid-cols-2 gap-6">
          <Card className="p-6">
            <h3 className="text-lg font-semibold text-slate-900 mb-4 flex items-center gap-2">
              <TrendingUp className="w-5 h-5" />
              Performance Overview
            </h3>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-slate-600">Completion Rate</span>
                <span className="font-semibold text-slate-900">
                  {analytics.totalAppointments > 0
                    ? ((analytics.completedAppointments / analytics.totalAppointments) * 100).toFixed(1)
                    : 0}
                  %
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-600">Cancellation Rate</span>
                <span className="font-semibold text-slate-900">
                  {analytics.totalAppointments > 0
                    ? ((analytics.cancelledAppointments / analytics.totalAppointments) * 100).toFixed(1)
                    : 0}
                  %
                </span>
              </div>
            </div>
          </Card>

          {analytics.revenueByPeriod && analytics.revenueByPeriod.length > 0 && (
            <Card className="p-6">
              <h3 className="text-lg font-semibold text-slate-900 mb-4">Revenue Trends</h3>
              <div className="space-y-3">
                {analytics.revenueByPeriod.map((period, idx) => (
                  <div key={idx} className="flex items-center justify-between">
                    <span className="text-slate-600">{period.period}</span>
                    <span className="font-semibold text-slate-900">
                      PKR {period.revenue.toFixed(2)}
                    </span>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}

