import { cn } from '../lib/utils';

const STEPS = ['MATCHED', 'PAID', 'SETTLED'] as const;

export interface OrderTimelineTrade {
  status?: string;
  paymentId?: string;
  transferId?: string;
}

export interface OrderTimelineProps {
  orderStatus: string;
  trade?: OrderTimelineTrade | null;
  polling?: boolean;
}

function stepIndex(orderStatus: string, trade: OrderTimelineTrade | null | undefined): number {
  const tradeStatus = trade?.status;
  if (orderStatus === 'SETTLED' || tradeStatus === 'SETTLED') return 3;
  if (orderStatus === 'PAID' || tradeStatus === 'PAID') return 2;
  if (orderStatus === 'MATCHED' || tradeStatus === 'PENDING') return 1;
  if (orderStatus === 'CANCELLED' || tradeStatus === 'FAILED') return -1;
  return 0;
}

export function OrderStatusTimeline({ orderStatus, trade, polling }: OrderTimelineProps) {
  const current = stepIndex(orderStatus, trade);
  const failed = current === -1;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-medium">Settlement progress</h3>
        {polling && (
          <span className="flex items-center gap-1 text-xs text-muted-foreground">
            <span className="inline-block h-3 w-3 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            Live
          </span>
        )}
      </div>
      <ol className="space-y-3">
        {STEPS.map((step, i) => {
          const done = !failed && current > i;
          const active = !failed && current === i + 1;
          return (
            <li key={step} className="flex items-center gap-3">
              <span
                className={cn(
                  'flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-2 text-[10px] font-bold',
                  done && 'border-primary bg-primary text-primary-foreground',
                  active && 'border-primary text-primary',
                  !done && !active && 'border-muted-foreground/40 text-transparent',
                )}
              >
                {done ? '✓' : ''}
              </span>
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
              <dd className="max-w-[200px] truncate font-mono">{trade.paymentId}</dd>
            </div>
          )}
          {trade.transferId && (
            <div className="flex justify-between">
              <dt>Transfer</dt>
              <dd className="max-w-[200px] truncate font-mono">{trade.transferId}</dd>
            </div>
          )}
        </dl>
      )}
    </div>
  );
}
