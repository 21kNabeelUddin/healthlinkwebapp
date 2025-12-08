import React, { useState } from 'react';
import { Search } from 'lucide-react';

interface Option {
  value: string;
  label: string;
}

interface CascadingSelectProps {
  label: string;
  options: Option[];
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  error?: string;
}

export function CascadingSelect({ 
  label, 
  options, 
  value, 
  onChange, 
  placeholder = 'Select...',
  disabled = false,
  error 
}: CascadingSelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  
  const filteredOptions = options.filter(option =>
    option.label.toLowerCase().includes(search.toLowerCase())
  );
  
  const selectedOption = options.find(opt => opt.value === value);
  
  return (
    <div className="w-full">
      <label className="block text-neutral-700 mb-2">
        {label}
      </label>
      <div className="relative">
        <button
          type="button"
          onClick={() => !disabled && setIsOpen(!isOpen)}
          disabled={disabled}
          className={`w-full px-4 py-2.5 bg-white border rounded-lg transition-all duration-200 text-left
            ${error ? 'border-danger-500' : 'border-neutral-300'}
            ${disabled ? 'bg-neutral-100 cursor-not-allowed' : 'hover:border-primary-600'}
            ${isOpen ? 'border-primary-600 ring-2 ring-primary-100' : ''}`}
        >
          {selectedOption ? selectedOption.label : <span className="text-neutral-400">{placeholder}</span>}
        </button>
        
        {isOpen && !disabled && (
          <>
            <div className="fixed inset-0 z-10" onClick={() => setIsOpen(false)} />
            <div className="absolute z-20 w-full mt-2 bg-white border border-neutral-200 rounded-lg shadow-lg max-h-64 overflow-hidden">
              <div className="p-2 border-b border-neutral-200">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-neutral-400" />
                  <input
                    type="text"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="Search..."
                    className="w-full pl-9 pr-3 py-2 border border-neutral-300 rounded-lg focus:border-primary-600 focus:ring-2 focus:ring-primary-100 outline-none"
                  />
                </div>
              </div>
              <div className="overflow-y-auto max-h-48">
                {filteredOptions.length > 0 ? (
                  filteredOptions.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => {
                        onChange(option.value);
                        setIsOpen(false);
                        setSearch('');
                      }}
                      className={`w-full px-4 py-2.5 text-left hover:bg-primary-50 transition-colors
                        ${option.value === value ? 'bg-primary-50 text-primary-700' : 'text-neutral-700'}`}
                    >
                      {option.label}
                    </button>
                  ))
                ) : (
                  <div className="px-4 py-6 text-center text-neutral-500">
                    No results found
                  </div>
                )}
              </div>
            </div>
          </>
        )}
      </div>
      {error && (
        <p className="mt-1.5 text-sm text-danger-600">{error}</p>
      )}
    </div>
  );
}
