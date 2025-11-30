'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { paymentsApi } from '@/lib/api';
import { toast } from 'react-hot-toast';
import DashboardLayout from '@/components/layout/DashboardLayout';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { format } from 'date-fns';
import { CreditCard, CheckCircle2, XCircle, Clock, Eye, Check, X } from 'lucide-react';

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
  payerId: string;
  payeeId: string;
}

export default function DoctorPaymentsPage() {
  const { user } = useAuth();
  const [payments, setPayments] = useState<Payment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filter, setFilter] = useState<string>('');

  useEffect(() => {
    if (user?.id) {
      loadPayments();
    }
  }, [user?.id, filter]);

  const loadPayments = async () => {
    setIsLoading(true);
    try {
      const data = await paymentsApi.list(undefined, true); // isDoctorView = true
      const filtered = filter ? data.filter((p: Payment) => p.status === filter) : data;
      setPayments(filtered || []);
    } catch (error: any) {
      toast.error('Failed to load payments');
      console.error('Payments load error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleVerify = async (paymentId: string, approve: boolean) => {
    try {
      if (approve) {
        await paymentsApi.verify({
          paymentId,
          status: 'CAPTURED',
          verificationNotes: 'Verified by doctor',
        });
        toast.success('Payment verified successfully');
      } else {
        await paymentsApi.verify({
          paymentId,
          status: 'REJECTED',
          verificationNotes: 'Rejected by doctor',
        });
        toast.success('Payment rejected');
      }
      loadPayments();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to verify payment');
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
      <DashboardLayout requiredUserType="DOCTOR">
        <div className="text-center py-8">Loading payments...</div>
      </DashboardLayout>
    );
  }

  const pendingPayments = payments.filter((p) => p.status === 'PENDING' || p.status === 'AUTHORIZED');

  return (
    <DashboardLayout requiredUserType="DOCTOR">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">Payment Verification</h1>
            <p className="text-slate-600 mt-1">Verify and manage patient payments</p>
          </div>
        </div>

        {pendingPayments.length > 0 && (
          <Card className="p-6 bg-yellow-50 border-yellow-200">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-semibold text-slate-900 mb-1">
                  {pendingPayments.length} Payment{pendingPayments.length > 1 ? 's' : ''} Pending Verification
                </h3>
                <p className="text-sm text-slate-600">Review and verify payment receipts</p>
              </div>
            </div>
          </Card>
        )}

        <div className="flex gap-2 mb-4">
          <Button
            variant={filter === '' ? 'primary' : 'outline'}
            onClick={() => setFilter('')}
            size="sm"
          >
            All
          </Button>
          <Button
            variant={filter === 'PENDING' ? 'primary' : 'outline'}
            onClick={() => setFilter('PENDING')}
            size="sm"
          >
            Pending
          </Button>
          <Button
            variant={filter === 'CAPTURED' ? 'primary' : 'outline'}
            onClick={() => setFilter('CAPTURED')}
            size="sm"
          >
            Verified
          </Button>
          <Button
            variant={filter === 'REJECTED' ? 'primary' : 'outline'}
            onClick={() => setFilter('REJECTED')}
            size="sm"
          >
            Rejected
          </Button>
        </div>

        {payments.length === 0 ? (
          <Card className="p-8 text-center">
            <CreditCard className="w-16 h-16 mx-auto text-slate-400 mb-4" />
            <h3 className="text-lg font-semibold text-slate-700 mb-2">No payments found</h3>
            <p className="text-slate-500">Payment history will appear here</p>
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

                {payment.receiptImageUrl && (
                  <div className="mb-4">
                    <a
                      href={payment.receiptImageUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-sm text-teal-600 hover:underline flex items-center gap-2"
                    >
                      <Eye className="w-4 h-4" />
                      View Receipt
                    </a>
                  </div>
                )}

                {(payment.status === 'PENDING' || payment.status === 'AUTHORIZED') && (
                  <div className="flex gap-2 pt-4 border-t border-slate-200">
                    <Button
                      variant="outline"
                      onClick={() => handleVerify(payment.id, false)}
                      className="text-red-600 border-red-200 hover:bg-red-50"
                    >
                      <X className="w-4 h-4 mr-2" />
                      Reject
                    </Button>
                    <Button
                      onClick={() => handleVerify(payment.id, true)}
                      className="bg-gradient-to-r from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700"
                    >
                      <Check className="w-4 h-4 mr-2" />
                      Verify Payment
                    </Button>
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

