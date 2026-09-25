/**
 * deploy.js — Deploys ComplianceRegistry and a sample PropertyToken.
 *
 * Usage:
 *   npm run deploy:local   → deploys to localhost:8545 (Hardhat node)
 *   npm run deploy:amoy    → deploys to Polygon Amoy testnet
 *
 * After deployment, copy the contract addresses to:
 *   - hardhat/.env  (COMPLIANCE_REGISTRY_ADDRESS etc.)
 *   - Spring Boot application.yml (blockchain.compliance-registry-address)
 */

const { ethers, network } = require("hardhat");

async function main() {
    console.log(`\n🚀 Deploying to network: ${network.name} (chainId: ${network.config.chainId})\n`);

    const [operator] = await ethers.getSigners();
    console.log(`Operator wallet: ${operator.address}`);

    const balance = await ethers.provider.getBalance(operator.address);
    console.log(`Operator balance: ${ethers.formatEther(balance)} MATIC\n`);

    // ─── 1. Deploy ComplianceRegistry ──────────────────────────────────────
    console.log("📋 Deploying ComplianceRegistry...");
    const ComplianceRegistry = await ethers.getContractFactory("ComplianceRegistry");
    const complianceRegistry = await ComplianceRegistry.deploy(operator.address);
    await complianceRegistry.waitForDeployment();
    const complianceAddress = await complianceRegistry.getAddress();
    console.log(`✅ ComplianceRegistry deployed at: ${complianceAddress}`);

    // ─── 2. Deploy a sample PropertyToken (Flat 101 demo) ──────────────────
    console.log("\n🏠 Deploying PropertyToken (demo — Flat 101)...");
    const PropertyToken = await ethers.getContractFactory("PropertyToken");

    const tokenArgs = {
        name: "Bishkek City Plaza — Flat 101",
        symbol: "BKCP-101",
        totalSupply: 1000n,               // 1000 tokens
        tokenPriceUsd: 4500n,            // $45.00 per token (in cents)
        operator: operator.address,
        complianceRegistry: complianceAddress,
        flatId: "2dc02ceb-2c64-4716-bbf4-90cf6276e4e1",
        buildingId: "34c3690a-729d-464b-90a9-c77caee031b5",
    };

    const propertyToken = await PropertyToken.deploy(
        tokenArgs.name,
        tokenArgs.symbol,
        tokenArgs.totalSupply,
        tokenArgs.tokenPriceUsd,
        tokenArgs.operator,
        tokenArgs.complianceRegistry,
        tokenArgs.flatId,
        tokenArgs.buildingId
    );
    await propertyToken.waitForDeployment();
    const tokenAddress = await propertyToken.getAddress();
    console.log(`✅ PropertyToken deployed at: ${tokenAddress}`);

    // ─── 3. Whitelist operator in ComplianceRegistry ───────────────────────
    console.log("\n🔐 Whitelisting operator in ComplianceRegistry...");
    const oneYearFromNow = Math.floor(Date.now() / 1000) + 365 * 24 * 60 * 60;
    const whitelistTx = await complianceRegistry.addToWhitelist(
        operator.address,
        "KG",
        oneYearFromNow
    );
    await whitelistTx.wait();
    console.log(`✅ Operator ${operator.address} whitelisted`);

    // ─── 4. Verify deployment ───────────────────────────────────────────────
    const operatorBalance = await propertyToken.balanceOf(operator.address);
    const totalSupply = await propertyToken.totalSupply();
    console.log(`\n📊 PropertyToken verification:`);
    console.log(`   Name:          ${await propertyToken.name()}`);
    console.log(`   Symbol:        ${await propertyToken.symbol()}`);
    console.log(`   Total supply:  ${totalSupply}`);
    console.log(`   Operator bal:  ${operatorBalance}`);
    console.log(`   Flat ID:       ${await propertyToken.flatId()}`);

    // ─── 5. Print deployment summary ───────────────────────────────────────
    console.log("\n" + "─".repeat(60));
    console.log("📝 DEPLOYMENT SUMMARY — add these to your config files:");
    console.log("─".repeat(60));
    console.log(`Network:                  ${network.name}`);
    console.log(`Chain ID:                 ${network.config.chainId}`);
    console.log(`ComplianceRegistry:       ${complianceAddress}`);
    console.log(`PropertyToken (Flat 101): ${tokenAddress}`);
    console.log(`Operator wallet:          ${operator.address}`);
    console.log("─".repeat(60));
    console.log("\n📋 Copy to application.yml:");
    console.log(`blockchain.compliance-registry-address: ${complianceAddress}`);
    console.log("\n📋 Copy to .env:");
    console.log(`COMPLIANCE_REGISTRY_ADDRESS=${complianceAddress}`);

    if (network.name !== "localhost" && network.name !== "hardhat") {
        console.log("\n🔍 Verify on Polygonscan (wait ~30s for indexing):");
        console.log(`npx hardhat verify --network ${network.name} ${complianceAddress} "${operator.address}"`);
        console.log(`npx hardhat verify --network ${network.name} ${tokenAddress} \\`);
        console.log(`  "${tokenArgs.name}" "${tokenArgs.symbol}" ${tokenArgs.totalSupply} ${tokenArgs.tokenPriceUsd} \\`);
        console.log(`  "${tokenArgs.operator}" "${complianceAddress}" "${tokenArgs.flatId}" "${tokenArgs.buildingId}"`);
    }

    return { complianceAddress, tokenAddress };
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error("❌ Deployment failed:", error);
        process.exit(1);
    });