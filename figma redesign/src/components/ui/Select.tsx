import React from 'react';

interface Option {
  value: string;
  label: string;
}

interface SelectProps extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'onChange'> {
  label?: string;
  error?: string;
  hint?: string;
  options: Option[];
  onChange?: (value: string) => void;
}

export function Select({ label, error, hint, options, onChange, className = '', ...props }: SelectProps) {
  return (
    <div className="w-full">
      {label && (
        <label className="block text-neutral-700 mb-2">
          {label}
        </label>
      )}
      <select
        className={`w-full px-4 py-2.5 bg-white border rounded-lg transition-all duration-200 appearance-none
          ${error ? 'border-danger-500 focus:border-danger-600 focus:ring-2 focus:ring-danger-100' : 'border-neutral-300 focus:border-primary-600 focus:ring-2 focus:ring-primary-100'}
          disabled:bg-neutral-100 disabled:cursor-not-allowed outline-none ${className}`}
        onChange={(e) => onChange?.(e.target.value)}
        {...props}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {hint && !error && (
        <p className="mt-1.5 text-sm text-neutral-500">{hint}</p>
      )}
      {error && (
        <p className="mt-1.5 text-sm text-danger-600">{error}</p>
      )}
    </div>
  );
}
