/**
 * deployFlat.js — deploy PropertyToken for a single flat (invoked by ContractDeployer).
 *
 * Env vars (set by ContractDeployer):
 *   FLAT_ID, BUILDING_ID, TOKEN_NAME, TOKEN_SYMBOL, TOTAL_SUPPLY, SPV_WALLET
 */
const hre = require("hardhat");

async function main() {
  const flatId = process.env.FLAT_ID;
  const tokenName = process.env.TOKEN_NAME || `Flat ${flatId} Token`;
  const tokenSymbol = process.env.TOKEN_SYMBOL || "FLT";
  const totalSupply = process.env.TOTAL_SUPPLY || "10000";
  const spvWallet = process.env.SPV_WALLET;

  if (!flatId || !spvWallet) {
    throw new Error("FLAT_ID and SPV_WALLET are required");
  }

  const PropertyToken = await hre.ethers.getContractFactory("PropertyToken");
  const token = await PropertyToken.deploy(tokenName, tokenSymbol, totalSupply, spvWallet);
  await token.waitForDeployment();
  const address = await token.getAddress();

  console.log(JSON.stringify({
    flatId,
    contractAddress: address,
    network: hre.network.name,
    chainId: (await hre.ethers.provider.getNetwork()).chainId.toString(),
  }));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
