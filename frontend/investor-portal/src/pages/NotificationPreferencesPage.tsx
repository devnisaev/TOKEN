import { FormEvent, useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import type { NotificationPreferences, UpdateNotificationPreferencesRequest } from '@/types/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';

const defaultPreferences: UpdateNotificationPreferencesRequest = {
  emailEnabled: true,
  tradeAlerts: true,
  dividendAlerts: true,
  rentReminders: true,
};

function PreferenceToggle({
  id,
  label,
  description,
  checked,
  onChange,
}: {
  id: string;
  label: string;
  description: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <div className="flex items-start justify-between gap-4 rounded-lg border p-4">
      <div>
        <Label htmlFor={id} className="text-base font-medium">
          {label}
        </Label>
        <p className="mt-1 text-sm text-muted-foreground">{description}</p>
      </div>
      <input
        id={id}
        type="checkbox"
        className="mt-1 h-4 w-4 accent-primary"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
      />
    </div>
  );
}

export function NotificationPreferencesPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<UpdateNotificationPreferencesRequest>(defaultPreferences);
  const [savedNotice, setSavedNotice] = useState(false);

  const preferencesQuery = useQuery({
    queryKey: ['notification-preferences', user?.id],
    queryFn: () => api.getNotificationPreferences(user!.id),
    enabled: !!user?.id,
  });

  useEffect(() => {
    if (preferencesQuery.data) {
      setForm(toForm(preferencesQuery.data));
    }
  }, [preferencesQuery.data]);

  const saveMutation = useMutation({
    mutationFn: () => api.updateNotificationPreferences(user!.id, form),
    onSuccess: () => {
      setSavedNotice(true);
      queryClient.invalidateQueries({ queryKey: ['notification-preferences', user?.id] });
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    setSavedNotice(false);
    saveMutation.mutate();
  }

  function setField<K extends keyof UpdateNotificationPreferencesRequest>(
    key: K,
    value: UpdateNotificationPreferencesRequest[K],
  ) {
    setForm((prev) => ({ ...prev, [key]: value }));
    setSavedNotice(false);
  }

  if (!user) return null;

  if (preferencesQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading preferences…
      </div>
    );
  }

  if (preferencesQuery.error) {
    return (
      <p className="text-destructive">
        {preferencesQuery.error instanceof Error
          ? preferencesQuery.error.message
          : 'Failed to load notification preferences'}
      </p>
    );
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Notification preferences</h1>
        <p className="text-muted-foreground">Choose which email alerts you receive from TokenRealty</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Email alerts</CardTitle>
          <CardDescription>Changes apply to {user.email}</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            <PreferenceToggle
              id="emailEnabled"
              label="Email notifications"
              description="Master switch for all email alerts"
              checked={form.emailEnabled}
              onChange={(checked) => setField('emailEnabled', checked)}
            />
            <PreferenceToggle
              id="tradeAlerts"
              label="Trade alerts"
              description="Order matched, settled, or failed"
              checked={form.tradeAlerts}
              onChange={(checked) => setField('tradeAlerts', checked)}
            />
            <PreferenceToggle
              id="dividendAlerts"
              label="Dividend alerts"
              description="Rental income distributions to your wallet"
              checked={form.dividendAlerts}
              onChange={(checked) => setField('dividendAlerts', checked)}
            />
            <PreferenceToggle
              id="rentReminders"
              label="Rent reminders"
              description="Monthly rent due notices (tenant accounts only)"
              checked={form.rentReminders}
              onChange={(checked) => setField('rentReminders', checked)}
            />

            {savedNotice && (
              <p className="flex items-center gap-2 rounded-md bg-green-50 p-3 text-sm text-green-800">
                <CheckCircle2 className="h-4 w-4" />
                Preferences saved.
              </p>
            )}

            {saveMutation.error && (
              <p className="text-sm text-destructive">
                {saveMutation.error instanceof Error
                  ? saveMutation.error.message
                  : 'Failed to save preferences'}
              </p>
            )}

            <Button type="submit" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Saving…' : 'Save preferences'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

function toForm(preferences: NotificationPreferences): UpdateNotificationPreferencesRequest {
  return {
    emailEnabled: preferences.emailEnabled,
    tradeAlerts: preferences.tradeAlerts,
    dividendAlerts: preferences.dividendAlerts,
    rentReminders: preferences.rentReminders,
  };
}
