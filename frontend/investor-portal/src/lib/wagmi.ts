import { http, createConfig } from 'wagmi';
import { hardhat, polygonAmoy } from 'wagmi/chains';
import { injected } from 'wagmi/connectors';

// WalletConnect v2: POST /v1/wallets/connect-session returns { sessionTopic, uri }
// for handshake prep; add walletConnect({ projectId }) connector when enabling WC.
export const wagmiConfig = createConfig({
  chains: [hardhat, polygonAmoy],
  connectors: [injected()],
  transports: {
    [hardhat.id]: http('http://127.0.0.1:8545'),
    [polygonAmoy.id]: http(),
  },
});
