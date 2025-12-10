'use client';

import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/marketing/ui/dialog';
import { Checkbox } from '@/marketing/ui/checkbox';
import { Button } from '@/marketing/ui/button';
import { Lock } from 'lucide-react';

interface ConsentDialogProps {
  open: boolean;
  onClose: () => void;
  onConsent: (consent: { prescriptions: boolean; history: boolean }) => void;
}

export function ConsentDialog({ open, onClose, onConsent }: ConsentDialogProps) {
  const [prescriptionsConsent, setPrescriptionsConsent] = useState(false);
  const [historyConsent, setHistoryConsent] = useState(false);
  const [understood, setUnderstood] = useState(false);

  const handleAllow = () => {
    onConsent({
      prescriptions: prescriptionsConsent,
      history: historyConsent,
    });
    // Reset state
    setPrescriptionsConsent(false);
    setHistoryConsent(false);
    setUnderstood(false);
    onClose();
  };

  const handleSkip = () => {
    onConsent({
      prescriptions: false,
      history: false,
    });
    // Reset state
    setPrescriptionsConsent(false);
    setHistoryConsent(false);
    setUnderstood(false);
    onClose();
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2 mb-2">
            <Lock className="w-5 h-5 text-teal-600" />
            <DialogTitle>Privacy & Data Access</DialogTitle>
          </div>
          <DialogDescription>
            To provide personalized assistance, I can access your medical information. This is completely optional.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-3">
            <div className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-slate-50">
              <Checkbox
                id="prescriptions"
                checked={prescriptionsConsent}
                onCheckedChange={(checked) => setPrescriptionsConsent(checked === true)}
                className="mt-0.5"
              />
              <div className="flex-1">
                <label
                  htmlFor="prescriptions"
                  className="text-sm font-medium text-slate-900 cursor-pointer"
                >
                  Prescriptions
                </label>
                <p className="text-xs text-slate-600 mt-1">
                  Current medications, dosages, and frequency
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-slate-50">
              <Checkbox
                id="history"
                checked={historyConsent}
                onCheckedChange={(checked) => setHistoryConsent(checked === true)}
                className="mt-0.5"
              />
              <div className="flex-1">
                <label
                  htmlFor="history"
                  className="text-sm font-medium text-slate-900 cursor-pointer"
                >
                  Medical History
                </label>
                <p className="text-xs text-slate-600 mt-1">
                  Past conditions, diagnoses, and procedures
                </p>
              </div>
            </div>
          </div>

          <div className="flex items-start gap-2 p-3 rounded-lg border border-amber-200 bg-amber-50">
            <Checkbox
              id="understood"
              checked={understood}
              onCheckedChange={(checked) => setUnderstood(checked === true)}
              className="mt-0.5"
            />
            <label
              htmlFor="understood"
              className="text-xs text-slate-700 cursor-pointer"
            >
              I understand this is optional and I can change these settings anytime
            </label>
          </div>
        </div>

        <DialogFooter className="flex-col sm:flex-row gap-2">
          <Button
            variant="outline"
            onClick={handleSkip}
            className="w-full sm:w-auto"
          >
            Skip for Now
          </Button>
          <Button
            onClick={handleAllow}
            disabled={!understood || (!prescriptionsConsent && !historyConsent)}
            className="w-full sm:w-auto bg-gradient-to-br from-teal-500 to-violet-600 hover:from-teal-600 hover:to-violet-700"
          >
            Allow Access
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

