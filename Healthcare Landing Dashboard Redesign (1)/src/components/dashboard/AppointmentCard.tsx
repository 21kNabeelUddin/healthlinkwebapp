import { Calendar, Clock, Video, MapPin, User } from "lucide-react";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";

interface AppointmentCardProps {
  doctorName?: string;
  patientName?: string;
  specialty?: string;
  date: string;
  time: string;
  type: "online" | "onsite";
  status?: "pending" | "confirmed" | "completed" | "cancelled";
  clinicName?: string;
  showActions?: boolean;
  onJoinZoom?: () => void;
  onConfirm?: () => void;
  onReject?: () => void;
}

export function AppointmentCard({
  doctorName,
  patientName,
  specialty,
  date,
  time,
  type,
  status = "confirmed",
  clinicName,
  showActions = false,
  onJoinZoom,
  onConfirm,
  onReject
}: AppointmentCardProps) {
  const typeConfig = {
    online: { icon: Video, color: "green", label: "Online" },
    onsite: { icon: MapPin, color: "blue", label: "On-site" }
  };

  const statusConfig = {
    pending: { color: "yellow", label: "Pending" },
    confirmed: { color: "green", label: "Confirmed" },
    completed: { color: "slate", label: "Completed" },
    cancelled: { color: "red", label: "Cancelled" }
  };

  const TypeIcon = typeConfig[type].icon;

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-4 hover:shadow-md transition-shadow">
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        {/* Left side - Info */}
        <div className="flex items-start gap-3 flex-1">
          <div className="w-12 h-12 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center flex-shrink-0">
            <User className="w-6 h-6 text-white" />
          </div>
          
          <div className="flex-1 min-w-0">
            <div className="flex items-start gap-2 mb-1">
              <p className="text-slate-900">{doctorName || patientName}</p>
              {status && (
                <Badge variant={statusConfig[status].color === "green" ? "default" : "secondary"} className="text-xs">
                  {statusConfig[status].label}
                </Badge>
              )}
            </div>
            {specialty && <p className="text-sm text-slate-600 mb-2">{specialty}</p>}
            
            <div className="flex flex-wrap items-center gap-3 text-xs text-slate-600">
              <div className="flex items-center gap-1">
                <Calendar className="w-3 h-3" />
                <span>{date}</span>
              </div>
              <div className="flex items-center gap-1">
                <Clock className="w-3 h-3" />
                <span>{time}</span>
              </div>
              <div className="flex items-center gap-1">
                <TypeIcon className="w-3 h-3" />
                <span>{typeConfig[type].label}</span>
              </div>
              {clinicName && type === "onsite" && (
                <span className="text-slate-500">• {clinicName}</span>
              )}
            </div>
          </div>
        </div>

        {/* Right side - Actions */}
        <div className="flex items-center gap-2">
          {showActions && status === "pending" && (
            <>
              <Button 
                size="sm" 
                variant="outline" 
                onClick={onReject}
                className="text-red-600 border-red-200 hover:bg-red-50"
              >
                Reject
              </Button>
              <Button 
                size="sm" 
                onClick={onConfirm}
                className="bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700"
              >
                Confirm
              </Button>
            </>
          )}
          
          {type === "online" && status === "confirmed" && (
            <Button 
              size="sm" 
              onClick={onJoinZoom}
              className="bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700"
            >
              <Video className="w-4 h-4 mr-1" />
              Join Zoom
            </Button>
          )}
          
          {!showActions && status === "confirmed" && type === "onsite" && (
            <Button size="sm" variant="outline">
              View Details
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
