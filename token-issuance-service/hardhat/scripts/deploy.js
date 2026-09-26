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
const fs = require("fs");
const path = require("path");

/** Hardhat default accounts #0–#2 (public dev keys). Use #0 as operator/SPV in local demos. */
const DEV_WALLETS = [
    "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
    "0x70997970C51812dc3A010C724d1AfE6Fc599aa84",
    "0x3C44CdDdB6a8fa426Eb90476d3413321120A0A01",
];

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

    // ─── 2. Deploy MockUSDC for Payment Service dev ─────────────────────────
    console.log("💵 Deploying MockUSDC...");
    const MockUSDC = await ethers.getContractFactory("MockUSDC");
    const mockUsdc = await MockUSDC.deploy(operator.address);
    await mockUsdc.waitForDeployment();
    const usdcAddress = await mockUsdc.getAddress();
    console.log(`✅ MockUSDC deployed at: ${usdcAddress}`);

    // ─── 3. Deploy a sample PropertyToken (Flat 101 demo) ──────────────────
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
        tokenArgs.buildingId,
        operator.address
    );
    await propertyToken.waitForDeployment();
    const tokenAddress = await propertyToken.getAddress();
    console.log(`✅ PropertyToken deployed at: ${tokenAddress}`);

    // ─── 4. Whitelist dev wallets in ComplianceRegistry ────────────────────
    console.log("\n🔐 Whitelisting dev wallets in ComplianceRegistry...");
    const oneYearFromNow = Math.floor(Date.now() / 1000) + 365 * 24 * 60 * 60;
    for (const wallet of DEV_WALLETS) {
        const whitelistTx = await complianceRegistry.addToWhitelist(wallet, "KG", oneYearFromNow);
        await whitelistTx.wait();
        console.log(`✅ Whitelisted ${wallet}`);
    }

    // ─── 4b. Mint MockUSDC to operator (dividend payout funding) ───────────
    const mintAmount = 1_000_000n * 10n ** 6n; // 1M USDC
    const mintTx = await mockUsdc.mint(operator.address, mintAmount);
    await mintTx.wait();
    console.log(`✅ Minted ${mintAmount} MockUSDC to operator ${operator.address}`);

    // ─── 4c. Enable transfers on demo token ────────────────────────────────
    const enableTx = await propertyToken.enableTransfers();
    await enableTx.wait();
    console.log("✅ Transfers enabled on demo PropertyToken");

    // ─── 4d. Optional DividendDistributor (DEPLOY_DIVIDEND_DISTRIBUTOR=true) ─
    let dividendDistributorAddress = null;
    if (process.env.DEPLOY_DIVIDEND_DISTRIBUTOR === "true") {
        console.log("\n💰 Deploying DividendDistributor...");
        const DividendDistributor = await ethers.getContractFactory("DividendDistributor");
        const dividendDistributor = await DividendDistributor.deploy(
            operator.address,
            usdcAddress,
            tokenAddress
        );
        await dividendDistributor.waitForDeployment();
        dividendDistributorAddress = await dividendDistributor.getAddress();
        console.log(`✅ DividendDistributor deployed at: ${dividendDistributorAddress}`);
    }

    // ─── 5. Verify deployment ───────────────────────────────────────────────
    const operatorBalance = await propertyToken.balanceOf(operator.address);
    const spvBalance = await propertyToken.balanceOf(operator.address);
    const totalSupply = await propertyToken.totalSupply();
    console.log(`\n📊 PropertyToken verification:`);
    console.log(`   Name:          ${await propertyToken.name()}`);
    console.log(`   Symbol:        ${await propertyToken.symbol()}`);
    console.log(`   Total supply:  ${totalSupply}`);
    console.log(`   SPV bal:       ${spvBalance}`);
    console.log(`   Flat ID:       ${await propertyToken.flatId()}`);

    // ─── 6. Print deployment summary ───────────────────────────────────────
    console.log("\n" + "─".repeat(60));
    console.log("📝 DEPLOYMENT SUMMARY — add these to your config files:");
    console.log("─".repeat(60));
    console.log(`Network:                  ${network.name}`);
    console.log(`Chain ID:                 ${network.config.chainId}`);
    console.log(`ComplianceRegistry:       ${complianceAddress}`);
    console.log(`MockUSDC:                 ${usdcAddress}`);
    console.log(`PropertyToken (Flat 101): ${tokenAddress}`);
    if (dividendDistributorAddress) {
        console.log(`DividendDistributor:        ${dividendDistributorAddress}`);
    }
    console.log(`Operator wallet:          ${operator.address}`);
    console.log("─".repeat(60));
    console.log("\n📋 Copy to application.yml:");
    console.log(`blockchain.compliance-registry-address: ${complianceAddress}`);
    console.log("\n📋 Copy to payment-service application.yml / .env:");
    console.log(`USDC_CONTRACT_ADDRESS=${usdcAddress}`);
    console.log(`PAYMENT_BLOCKCHAIN_ENABLED=true`);
    console.log("\n📋 Copy to .env:");
    console.log(`COMPLIANCE_REGISTRY_ADDRESS=${complianceAddress}`);
    console.log(`USDC_CONTRACT_ADDRESS=${usdcAddress}`);

    const deployment = {
        network: network.name,
        chainId: Number(network.config.chainId),
        operatorAddress: operator.address,
        complianceRegistryAddress: complianceAddress,
        usdcContractAddress: usdcAddress,
        demoPropertyTokenAddress: tokenAddress,
        dividendDistributorAddress,
        devWallets: DEV_WALLETS,
        deployedAt: new Date().toISOString(),
    };

    const deploymentsDir = path.join(__dirname, "../deployments");
    fs.mkdirSync(deploymentsDir, { recursive: true });
    const deploymentPath = path.join(deploymentsDir, "localhost.json");
    fs.writeFileSync(deploymentPath, JSON.stringify(deployment, null, 2));
    console.log(`\n💾 Wrote ${deploymentPath}`);

    if (network.name !== "localhost" && network.name !== "hardhat") {
        console.log("\n🔍 Verify on Polygonscan (wait ~30s for indexing):");
        console.log(`npx hardhat verify --network ${network.name} ${complianceAddress} "${operator.address}"`);
        console.log(`npx hardhat verify --network ${network.name} ${tokenAddress} \\`);
        console.log(`  "${tokenArgs.name}" "${tokenArgs.symbol}" ${tokenArgs.totalSupply} ${tokenArgs.tokenPriceUsd} \\`);
        console.log(`  "${tokenArgs.operator}" "${complianceAddress}" "${tokenArgs.flatId}" "${tokenArgs.buildingId}"`);
    }

    return deployment;
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error("❌ Deployment failed:", error);
        process.exit(1);
    });