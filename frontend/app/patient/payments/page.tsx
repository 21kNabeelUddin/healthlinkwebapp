'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { paymentsApi } from '@/lib/api';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { format } from 'date-fns';
import { CreditCard, CheckCircle2, XCircle, Clock, Upload } from 'lucide-react';

interface Payment {
  id: string;
  appointmentId: string;
  amount: number;
  currency: string;
  paymentMethod: string;
  status: string;
  receiptImageUrl?: string;
  createdAt: string;
  transactionId?: string;
}

export default function PaymentsPage() {
  const { user } = useAuth();
  const [payments, setPayments] = useState<Payment[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (user?.id) {
      loadPayments();
    }
  }, [user?.id]);

  const loadPayments = async () => {
    setIsLoading(true);
    try {
      const data = await paymentsApi.list(user?.id?.toString());
      // Ensure we always have an array, even if API returns unexpected format
      setPayments(Array.isArray(data) ? data : []);
    } catch (error: any) {
      toast.error('Failed to load payments');
      console.error('Payments load error:', error);
      // Prevent follow-up issues by resetting to empty
      setPayments([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUploadReceipt = async (paymentId: string, file: File) => {
    try {
      // In a real app, you'd upload the file first, then pass the URL
      // For now, we'll use a placeholder
      const receiptUrl = URL.createObjectURL(file);
      await paymentsApi.uploadReceipt(paymentId, receiptUrl);
      toast.success('Receipt uploaded successfully');
      loadPayments();
    } catch (error: any) {
      toast.error('Failed to upload receipt');
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'COMPLETED':
      case 'CAPTURED':
        return <CheckCircle2 className="w-5 h-5 text-green-600" />;
      case 'FAILED':
      case 'REJECTED':
        return <XCircle className="w-5 h-5 text-red-600" />;
      default:
        return <Clock className="w-5 h-5 text-yellow-600" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
      case 'CAPTURED':
        return 'bg-green-100 text-green-800';
      case 'FAILED':
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      case 'PENDING':
      case 'AUTHORIZED':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout requiredUserType="PATIENT">
        <div className="text-center py-8">Loading payments...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout requiredUserType="PATIENT">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">Payment History</h1>
            <p className="text-slate-600 mt-1">View and manage your payment transactions</p>
          </div>
        </div>

        {payments.length === 0 ? (
          <Card className="p-8 text-center">
            <CreditCard className="w-16 h-16 mx-auto text-slate-400 mb-4" />
            <h3 className="text-lg font-semibold text-slate-700 mb-2">No payments yet</h3>
            <p className="text-slate-500">Your payment history will appear here</p>
          </Card>
        ) : (
          <div className="grid gap-6">
            {payments.map((payment) => (
              <Card key={payment.id} className="p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-start gap-4">
                    {getStatusIcon(payment.status)}
                    <div>
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="font-semibold text-slate-900">
                          {payment.currency} {payment.amount.toFixed(2)}
                        </h3>
                        <span className={`text-xs px-2 py-1 rounded ${getStatusColor(payment.status)}`}>
                          {payment.status}
                        </span>
                      </div>
                      <div className="text-sm text-slate-600 space-y-1">
                        <p>Method: {payment.paymentMethod}</p>
                        <p>Date: {format(new Date(payment.createdAt), 'MMM dd, yyyy HH:mm')}</p>
                        {payment.transactionId && (
                          <p className="text-xs">Transaction ID: {payment.transactionId}</p>
                        )}
                      </div>
                    </div>
                  </div>
                </div>

                {payment.status === 'PENDING' && payment.paymentMethod === 'MOBILE_PAYMENT' && (
                  <div className="mt-4 pt-4 border-t border-slate-200">
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      Upload Payment Receipt
                    </label>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) {
                          handleUploadReceipt(payment.id, file);
                        }
                      }}
                      className="block w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-teal-50 file:text-teal-700 hover:file:bg-teal-100"
                    />
                  </div>
                )}

                {payment.receiptImageUrl && (
                  <div className="mt-4">
                    <a
                      href={payment.receiptImageUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-sm text-teal-600 hover:underline flex items-center gap-2"
                    >
                      <Upload className="w-4 h-4" />
                      View Receipt
                    </a>
                  </div>
                )}
              </Card>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}

