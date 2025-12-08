import React, { useEffect } from 'react';
import { X } from 'lucide-react';
import { Button } from './Button';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl';
}

export function Modal({ isOpen, onClose, title, children, footer, size = 'md' }: ModalProps) {
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const sizeStyles = {
    sm: 'max-w-md',
    md: 'max-w-2xl',
    lg: 'max-w-4xl',
    xl: 'max-w-6xl'
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-full items-center justify-center p-4">
        {/* Backdrop */}
        <div 
          className="fixed inset-0 bg-black/30 backdrop-blur-sm transition-opacity"
          onClick={onClose}
        />
        
        {/* Modal */}
        <div className={`relative bg-white rounded-xl shadow-2xl w-full ${sizeStyles[size]} max-h-[90vh] flex flex-col`}>
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-200">
            <h3 className="text-neutral-900">{title}</h3>
            <button
              onClick={onClose}
              className="text-neutral-400 hover:text-neutral-600 transition-colors p-1 rounded-lg hover:bg-neutral-100"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
          
          {/* Content */}
          <div className="px-6 py-4 overflow-y-auto flex-1">
            {children}
          </div>
          
          {/* Footer */}
          {footer && (
            <div className="px-6 py-4 border-t border-neutral-200 bg-neutral-50 rounded-b-xl flex items-center justify-end gap-3">
              {footer}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
