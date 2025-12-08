import React from 'react';

type BadgeVariant = 'default' | 'success' | 'warning' | 'danger' | 'info' | 'pending' | 'confirmed' | 'in-progress' | 'completed' | 'cancelled' | 'no-show';

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  className?: string;
}

export function Badge({ variant = 'default', children, className = '' }: BadgeProps) {
  const variantStyles = {
    default: 'bg-neutral-100 text-neutral-700',
    success: 'bg-success-50 text-success-700',
    warning: 'bg-warning-50 text-warning-700',
    danger: 'bg-danger-50 text-danger-700',
    info: 'bg-secondary-50 text-secondary-700',
    pending: 'bg-warning-50 text-warning-700',
    confirmed: 'bg-primary-50 text-primary-700',
    'in-progress': 'bg-secondary-50 text-secondary-700',
    completed: 'bg-success-50 text-success-700',
    cancelled: 'bg-neutral-100 text-neutral-600',
    'no-show': 'bg-danger-50 text-danger-700'
  };
  
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-sm ${variantStyles[variant]} ${className}`}>
      {children}
    </span>
  );
}
