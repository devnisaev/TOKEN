import type { Order, Trade } from '@/types/api';
import { cn } from '@/lib/utils';
import { CheckCircle2, Circle, Loader2 } from 'lucide-react';

const STEPS = ['MATCHED', 'PAID', 'SETTLED'] as const;

function stepIndex(order: Order, trade: Trade | null): number {
  if (order.status === 'SETTLED' || trade?.status === 'SETTLED') return 3;
  if (order.status === 'PAID' || trade?.status === 'PAID') return 2;
  if (order.status === 'MATCHED' || trade?.status === 'PENDING') return 1;
  if (order.status === 'CANCELLED' || trade?.status === 'FAILED') return -1;
  return 0;
}

export function OrderStatusTimeline({
  order,
  trade,
  polling,
}: {
  order: Order;
  trade: Trade | null;
  polling?: boolean;
}) {
  const current = stepIndex(order, trade);
  const failed = current === -1;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-medium">Settlement progress</h3>
        {polling && (
          <span className="flex items-center gap-1 text-xs text-muted-foreground">
            <Loader2 className="h-3 w-3 animate-spin" /> Live
          </span>
        )}
      </div>
      <ol className="space-y-3">
        {STEPS.map((step, i) => {
          const done = !failed && current > i;
          const active = !failed && current === i + 1;
          return (
            <li key={step} className="flex items-center gap-3">
              {done ? (
                <CheckCircle2 className="h-5 w-5 text-primary" />
              ) : (
                <Circle
                  className={cn('h-5 w-5', active ? 'text-primary' : 'text-muted-foreground/40')}
                />
              )}
              <div>
                <p className={cn('text-sm font-medium', active && 'text-primary')}>
                  {step === 'MATCHED' && 'Order matched · escrow initiated'}
                  {step === 'PAID' && 'Payment confirmed'}
                  {step === 'SETTLED' && 'Tokens transferred · trade settled'}
                </p>
                {active && step === 'MATCHED' && (
                  <p className="text-xs text-muted-foreground">Waiting for USDC deposit confirmation</p>
                )}
                {active && step === 'PAID' && (
                  <p className="text-xs text-muted-foreground">On-chain token transfer in progress</p>
                )}
              </div>
            </li>
          );
        })}
      </ol>
      {failed && (
        <p className="text-sm text-destructive">Order or trade failed — contact support.</p>
      )}
      {trade && (
        <dl className="grid gap-1 rounded-md bg-muted/50 p-3 text-xs text-muted-foreground">
          <div className="flex justify-between">
            <dt>Trade status</dt>
            <dd className="font-mono">{trade.status}</dd>
          </div>
          {trade.paymentId && (
            <div className="flex justify-between">
              <dt>Payment</dt>
              <dd className="font-mono truncate max-w-[200px]">{trade.paymentId}</dd>
            </div>
          )}
          {trade.transferId && (
            <div className="flex justify-between">
              <dt>Transfer</dt>
              <dd className="font-mono truncate max-w-[200px]">{trade.transferId}</dd>
            </div>
          )}
        </dl>
      )}
    </div>
  );
}
