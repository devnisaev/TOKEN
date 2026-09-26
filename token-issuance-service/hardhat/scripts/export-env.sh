#!/usr/bin/env bash
# Source contract addresses from deployments/localhost.json into the shell.
# Usage: eval "$(./scripts/export-env.sh)"

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENT_FILE="${SCRIPT_DIR}/../deployments/localhost.json"

if [[ ! -f "$DEPLOYMENT_FILE" ]]; then
  echo "echo 'Run npm run deploy:local first (Hardhat node on :8545)' >&2" >&2
  exit 1
fi

node -e "
const d = require('${DEPLOYMENT_FILE}');
console.log('export COMPLIANCE_REGISTRY_ADDRESS=' + d.complianceRegistryAddress);
console.log('export USDC_CONTRACT_ADDRESS=' + d.usdcContractAddress);
console.log('export OPERATOR_PRIVATE_KEY=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80');
console.log('export BLOCKCHAIN_RPC_URL=http://localhost:8545');
console.log('export BLOCKCHAIN_CHAIN_ID=31337');
console.log('export PAYMENT_BLOCKCHAIN_ENABLED=true');
console.log('export KAFKA_ENABLED=true');
"
