import { useAccount, useConnect, useDisconnect } from 'wagmi';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/lib/auth';
import { api } from '@/lib/api';
import { useState } from 'react';

export function ConnectWalletButton() {
  const { user, refreshProfile } = useAuth();
  const { address, isConnected } = useAccount();
  const { connect, connectors, isPending } = useConnect();
  const { disconnect } = useDisconnect();
  const [syncing, setSyncing] = useState(false);

  const short = address ? `${address.slice(0, 6)}…${address.slice(-4)}` : '';

  async function syncWallet() {
    if (!user || !address) return;
    setSyncing(true);
    try {
      await api.updateProfileWallet(address);
      await api.linkWallet(user.id, address).catch(() => undefined);
      await refreshProfile();
    } finally {
      setSyncing(false);
    }
  }

  if (isConnected && address) {
    return (
      <div className="flex flex-wrap items-center gap-2">
        <span className="rounded-md bg-secondary px-2 py-1 font-mono text-xs">{short}</span>
        {user?.walletAddress?.toLowerCase() !== address.toLowerCase() && (
          <Button size="sm" variant="secondary" onClick={syncWallet} disabled={syncing}>
            {syncing ? 'Syncing…' : 'Link to account'}
          </Button>
        )}
        <Button size="sm" variant="outline" onClick={() => disconnect()}>
          Disconnect
        </Button>
      </div>
    );
  }

  async function handleConnect() {
    if (user) {
      await api.createConnectSession(user.id).catch(() => undefined);
    }
    connect({ connector: connectors[0] });
  }

  return (
    <Button
      size="sm"
      variant="outline"
      disabled={isPending || connectors.length === 0}
      onClick={() => void handleConnect()}
    >
      {isPending ? 'Connecting…' : 'Connect MetaMask'}
    </Button>
  );
}
