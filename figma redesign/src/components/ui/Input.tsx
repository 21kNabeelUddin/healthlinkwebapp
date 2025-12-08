import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export function Input({ label, error, hint, className = '', ...props }: InputProps) {
  return (
    <div className="w-full">
      {label && (
        <label className="block text-neutral-700 mb-2">
          {label}
        </label>
      )}
      <input
        className={`w-full px-4 py-2.5 bg-white border rounded-lg transition-all duration-200 
          ${error ? 'border-danger-500 focus:border-danger-600 focus:ring-2 focus:ring-danger-100' : 'border-neutral-300 focus:border-primary-600 focus:ring-2 focus:ring-primary-100'}
          disabled:bg-neutral-100 disabled:cursor-not-allowed outline-none ${className}`}
        {...props}
      />
      {hint && !error && (
        <p className="mt-1.5 text-sm text-neutral-500">{hint}</p>
      )}
      {error && (
        <p className="mt-1.5 text-sm text-danger-600">{error}</p>
      )}
    </div>
  );
}
