require("@nomicfoundation/hardhat-toolbox");
require("dotenv").config();

// ─── Environment variables ─────────────────────────────────────────────────
// Copy .env.example to .env and fill in your values
// NEVER commit .env to git

const OPERATOR_PRIVATE_KEY = process.env.OPERATOR_PRIVATE_KEY
    || "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"; // Hardhat account #0

const AMOY_RPC_URL = process.env.AMOY_RPC_URL
    || "https://rpc-amoy.polygon.technology";

const POLYGON_RPC_URL = process.env.POLYGON_RPC_URL
    || "https://polygon-rpc.com";

const POLYGONSCAN_API_KEY = process.env.POLYGONSCAN_API_KEY || "";

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
    solidity: {
        version: "0.8.20",
        settings: {
            optimizer: {
                enabled: true,
                runs: 200,
            },
            evmVersion: "paris",
        },
    },

    // ─── Contract sources location ──────────────────────────────────────────
    paths: {
        sources: "../contracts",   // .sol files live at project root /contracts/
        tests: "./test",
        cache: "./cache",
        artifacts: "./artifacts",
    },

    networks: {
        // ─── Local Hardhat node (default) ───────────────────────────────────
        hardhat: {
            chainId: 31337,
            mining: {
                auto: true,        // mine immediately on tx submission
                interval: 0,
            },
            accounts: {
                count: 20,
                accountsBalance: "10000000000000000000000", // 10,000 ETH each
            },
        },

        // ─── Local node started with `npm run node` ──────────────────────────
        localhost: {
            url: "http://127.0.0.1:8545",
            chainId: 31337,
        },

        // ─── Polygon Amoy testnet ────────────────────────────────────────────
        amoy: {
            url: AMOY_RPC_URL,
            chainId: 80002,
            accounts: [OPERATOR_PRIVATE_KEY],
            gasPrice: "auto",
            gas: "auto",
            timeout: 120000,
        },

        // ─── Polygon mainnet (production — use with extreme care) ────────────
        polygon: {
            url: POLYGON_RPC_URL,
            chainId: 137,
            accounts: [OPERATOR_PRIVATE_KEY],
            gasPrice: "auto",
        },
    },

    // ─── Contract verification on Polygonscan ──────────────────────────────
    etherscan: {
        apiKey: {
            polygonAmoy: POLYGONSCAN_API_KEY,
            polygon: POLYGONSCAN_API_KEY,
        },
        customChains: [
            {
                network: "polygonAmoy",
                chainId: 80002,
                urls: {
                    apiURL: "https://api-amoy.polygonscan.com/api",
                    browserURL: "https://amoy.polygonscan.com",
                },
            },
        ],
    },

    // ─── Gas reporter ──────────────────────────────────────────────────────
    gasReporter: {
        enabled: process.env.REPORT_GAS === "true",
        currency: "USD",
        coinmarketcap: process.env.COINMARKETCAP_API_KEY,
        token: "MATIC",
    },
}