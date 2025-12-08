import React, { useEffect } from 'react';
import { CheckCircle, AlertCircle, X, Info } from 'lucide-react';

type ToastType = 'success' | 'error' | 'info' | 'warning';

interface ToastProps {
  type: ToastType;
  message: string;
  onClose: () => void;
  duration?: number;
}

export function Toast({ type, message, onClose, duration = 5000 }: ToastProps) {
  useEffect(() => {
    if (duration > 0) {
      const timer = setTimeout(onClose, duration);
      return () => clearTimeout(timer);
    }
  }, [duration, onClose]);

  const icons = {
    success: <CheckCircle className="w-5 h-5 text-success-600" />,
    error: <AlertCircle className="w-5 h-5 text-danger-600" />,
    warning: <AlertCircle className="w-5 h-5 text-warning-600" />,
    info: <Info className="w-5 h-5 text-secondary-600" />
  };

  const styles = {
    success: 'bg-success-50 border-success-200',
    error: 'bg-danger-50 border-danger-200',
    warning: 'bg-warning-50 border-warning-200',
    info: 'bg-secondary-50 border-secondary-200'
  };

  return (
    <div className={`flex items-start gap-3 px-4 py-3 rounded-lg border ${styles[type]} shadow-lg max-w-md`}>
      {icons[type]}
      <p className="flex-1 text-neutral-800">{message}</p>
      <button onClick={onClose} className="text-neutral-400 hover:text-neutral-600 transition-colors">
        <X className="w-4 h-4" />
      </button>
    </div>
  );
}

interface ToastContainerProps {
  toasts: Array<{ id: string; type: ToastType; message: string }>;
  removeToast: (id: string) => void;
}

export function ToastContainer({ toasts, removeToast }: ToastContainerProps) {
  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => (
        <Toast
          key={toast.id}
          type={toast.type}
          message={toast.message}
          onClose={() => removeToast(toast.id)}
        />
      ))}
    </div>
  );
}
