import { cn } from '../lib/utils';

const STATUS_VARIANTS: Record<string, string> = {
  SETTLED: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
  APPROVED: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
  TOKENIZED: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
  CONFIRMED: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
  MATCHED: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
  PAID: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
  PENDING: 'bg-amber-100 text-amber-900 dark:bg-amber-900/30 dark:text-amber-200',
  CANCELLED: 'bg-destructive/10 text-destructive',
  REJECTED: 'bg-destructive/10 text-destructive',
  FAILED: 'bg-destructive/10 text-destructive',
  REVOKED: 'bg-destructive/10 text-destructive',
};

export interface StatusBadgeProps {
  status: string;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const normalized = status.toUpperCase();
  return (
    <span
      className={cn(
        'inline-flex rounded-full px-2 py-0.5 text-xs font-medium',
        STATUS_VARIANTS[normalized] ?? 'bg-secondary text-secondary-foreground',
        className,
      )}
    >
      {status}
    </span>
  );
}
