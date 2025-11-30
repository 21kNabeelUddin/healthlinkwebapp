import { LucideIcon, TrendingUp, TrendingDown } from "lucide-react";

interface StatsCardProps {
  icon: LucideIcon;
  label: string;
  value: string | number;
  trend?: {
    value: number;
    isPositive: boolean;
  };
  gradient: string;
}

export function StatsCard({ icon: Icon, label, value, trend, gradient }: StatsCardProps) {
  return (
    <div className="bg-white rounded-xl shadow-lg border border-slate-200 p-6 hover:shadow-xl transition-shadow">
      <div className="flex items-start justify-between mb-4">
        <div className={`w-12 h-12 bg-gradient-to-br ${gradient} rounded-xl flex items-center justify-center shadow-lg`}>
          <Icon className="w-6 h-6 text-white" />
        </div>
        {trend && (
          <div className={`flex items-center gap-1 text-sm ${trend.isPositive ? "text-green-600" : "text-red-600"}`}>
            {trend.isPositive ? <TrendingUp className="w-4 h-4" /> : <TrendingDown className="w-4 h-4" />}
            <span>{Math.abs(trend.value)}%</span>
          </div>
        )}
      </div>
      <div>
        <p className="text-3xl text-slate-900 mb-1">{value}</p>
        <p className="text-sm text-slate-600">{label}</p>
      </div>
    </div>
  );
}
