export interface SettlementAuditOrder {
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SettlementAuditTrade {
  id?: string;
  status?: string;
  paymentId?: string;
  transferId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SettlementAuditTrailProps {
  order: SettlementAuditOrder;
  trade?: SettlementAuditTrade | null;
}

interface AuditEntry {
  label: string;
  detail?: string;
  at?: string;
  state: 'done' | 'pending' | 'failed';
}

function formatTime(iso?: string): string | undefined {
  if (!iso) return undefined;
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

function buildEntries(order: SettlementAuditOrder, trade?: SettlementAuditTrade | null): AuditEntry[] {
  const entries: AuditEntry[] = [
    {
      label: 'Order placed',
      at: order.createdAt,
      detail: `Status: ${order.status}`,
      state: 'done',
    },
  ];

  if (order.status === 'CANCELLED') {
    entries.push({
      label: 'Order cancelled',
      at: order.updatedAt,
      state: 'failed',
    });
    return entries;
  }

  if (['MATCHED', 'PAID', 'SETTLED'].includes(order.status)) {
    entries.push({
      label: 'Order matched',
      at: order.updatedAt,
      detail: 'Escrow initiated',
      state: 'done',
    });
  } else {
    entries.push({ label: 'Awaiting match', state: 'pending' });
    return entries;
  }

  if (!trade) {
    entries.push({ label: 'Trade record', state: 'pending' });
    return entries;
  }

  entries.push({
    label: 'Trade opened',
    at: trade.createdAt,
    detail: trade.id ? `Trade ${trade.id.slice(0, 8)}…` : undefined,
    state: 'done',
  });

  if (trade.status === 'FAILED') {
    entries.push({ label: 'Trade failed', at: trade.updatedAt, state: 'failed' });
    return entries;
  }

  if (trade.paymentId) {
    entries.push({
      label: 'Payment linked',
      detail: trade.paymentId,
      state: 'done',
    });
  } else {
    entries.push({ label: 'Payment pending', state: 'pending' });
  }

  if (trade.transferId) {
    entries.push({
      label: 'Transfer linked',
      detail: trade.transferId,
      state: 'done',
    });
  } else if (trade.paymentId) {
    entries.push({ label: 'Transfer pending', state: 'pending' });
  }

  if (order.status === 'SETTLED' || trade.status === 'SETTLED') {
    entries.push({
      label: 'Trade settled',
      at: trade.updatedAt ?? order.updatedAt,
      state: 'done',
    });
  }

  return entries;
}

export function SettlementAuditTrail({ order, trade }: SettlementAuditTrailProps) {
  const entries = buildEntries(order, trade);

  return (
    <div className="space-y-3">
      <h3 className="font-medium text-sm">Settlement audit</h3>
      <ol className="space-y-2 border-l border-muted pl-4">
        {entries.map((entry) => (
          <li key={entry.label} className="relative text-sm">
            <span
              className={`absolute -left-[1.3rem] top-1.5 h-2 w-2 rounded-full ${
                entry.state === 'done'
                  ? 'bg-primary'
                  : entry.state === 'failed'
                    ? 'bg-destructive'
                    : 'bg-muted-foreground/40'
              }`}
            />
            <p className="font-medium">{entry.label}</p>
            {entry.detail && (
              <p className="font-mono text-xs text-muted-foreground break-all">{entry.detail}</p>
            )}
            {entry.at && (
              <p className="text-xs text-muted-foreground">{formatTime(entry.at)}</p>
            )}
          </li>
        ))}
      </ol>
    </div>
  );
}
