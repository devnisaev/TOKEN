import { http, createConfig } from 'wagmi';
import { hardhat, polygonAmoy } from 'wagmi/chains';
import { injected, walletConnect } from 'wagmi/connectors';

const walletConnectProjectId = import.meta.env.VITE_WALLETCONNECT_PROJECT_ID?.trim();

export const walletConnectEnabled = Boolean(walletConnectProjectId);

// WalletConnect v2: POST /v1/wallets/connect-session returns { sessionTopic, uri }
// for handshake prep; walletConnect connector activates when VITE_WALLETCONNECT_PROJECT_ID is set.
export const wagmiConfig = createConfig({
  chains: [hardhat, polygonAmoy],
  connectors: [
    injected(),
    ...(walletConnectProjectId
      ? [walletConnect({ projectId: walletConnectProjectId, showQrModal: true })]
      : []),
  ],
  transports: {
    [hardhat.id]: http('http://127.0.0.1:8545'),
    [polygonAmoy.id]: http(),
  },
});
