export { cn } from '@tokenrealty/shared-ui';

export function formatUsd(value: number | string | undefined): string {
  const n = typeof value === 'string' ? parseFloat(value) : value ?? 0;
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
  }).format(n);
}
