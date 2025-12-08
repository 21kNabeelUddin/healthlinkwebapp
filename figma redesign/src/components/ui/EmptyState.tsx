import React from 'react';

interface EmptyStateProps {
  icon: React.ReactNode;
  title: string;
  description: string;
  action?: React.ReactNode;
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4">
      <div className="w-16 h-16 rounded-full bg-neutral-100 flex items-center justify-center text-neutral-400 mb-4">
        {icon}
      </div>
      <h3 className="text-neutral-900 mb-2">{title}</h3>
      <p className="text-neutral-500 text-center max-w-md mb-6">{description}</p>
      {action}
    </div>
  );
}
