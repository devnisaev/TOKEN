/**
 * deployFlat.js — deploy PropertyToken for a single flat (invoked by ContractDeployer).
 *
 * Env vars (set by ContractDeployer):
 *   FLAT_ID, BUILDING_ID, TOKEN_NAME, TOKEN_SYMBOL, TOTAL_SUPPLY, PRICE_USD_CENTS,
 *   SPV_WALLET, COMPLIANCE_REGISTRY_ADDRESS
 */
const hre = require("hardhat");

async function main() {
  const flatId = process.env.FLAT_ID;
  const buildingId = process.env.BUILDING_ID;
  const tokenName = process.env.TOKEN_NAME || `Flat ${flatId} Token`;
  const tokenSymbol = process.env.TOKEN_SYMBOL || "FLT";
  const totalSupply = process.env.TOTAL_SUPPLY || "10000";
  const priceUsdCents = process.env.PRICE_USD_CENTS || "1000";
  const spvWallet = process.env.SPV_WALLET;
  const complianceRegistry = process.env.COMPLIANCE_REGISTRY_ADDRESS;

  if (!flatId || !buildingId || !spvWallet || !complianceRegistry) {
    throw new Error("FLAT_ID, BUILDING_ID, SPV_WALLET, COMPLIANCE_REGISTRY_ADDRESS are required");
  }

  const [operator] = await hre.ethers.getSigners();
  const PropertyToken = await hre.ethers.getContractFactory("PropertyToken");
  const token = await PropertyToken.deploy(
    tokenName,
    tokenSymbol,
    totalSupply,
    priceUsdCents,
    operator.address,
    complianceRegistry,
    flatId,
    buildingId,
    spvWallet
  );
  const deployTx = token.deploymentTransaction();
  await token.waitForDeployment();
  const receipt = deployTx ? await deployTx.wait() : null;
  const address = await token.getAddress();
  const network = await hre.ethers.provider.getNetwork();

  const enableTx = await token.enableTransfers();
  await enableTx.wait();

  console.log(JSON.stringify({
    contractAddress: address,
    txHash: deployTx ? deployTx.hash : null,
    blockNumber: receipt ? receipt.blockNumber : 0,
    operatorAddress: operator.address,
    network: hre.network.name,
    chainId: Number(network.chainId),
  }));
}

main().catch((error) => {
  console.error(JSON.stringify({ error: error.message }));
  process.exitCode = 1;
});
