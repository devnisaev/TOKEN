/**
 * Mint MockUSDC to dev wallets on local Hardhat node.
 *
 * Usage:
 *   USDC_CONTRACT_ADDRESS=0x... RECIPIENTS=0xabc,0xdef AMOUNT=100000000000 npm run mint:usdc
 *
 * AMOUNT is in USDC base units (6 decimals). Default 100,000 USDC per recipient.
 */
const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
    const usdcAddress = process.env.USDC_CONTRACT_ADDRESS;
    if (!usdcAddress) {
        throw new Error("USDC_CONTRACT_ADDRESS is required");
    }

    const deploymentFile = path.join(__dirname, "../deployments/localhost.json");
    let recipients = process.env.RECIPIENTS;
    if (!recipients && fs.existsSync(deploymentFile)) {
        const deployment = JSON.parse(fs.readFileSync(deploymentFile, "utf8"));
        recipients = deployment.devWallets.join(",");
    }
    if (!recipients) {
        throw new Error("RECIPIENTS or deployments/localhost.json devWallets required");
    }

    const amount = BigInt(process.env.AMOUNT || "100000000000"); // 100k USDC
    const [operator] = await hre.ethers.getSigners();
    const MockUSDC = await hre.ethers.getContractFactory("MockUSDC");
    const usdc = MockUSDC.attach(usdcAddress);

    for (const wallet of recipients.split(",").map((w) => w.trim()).filter(Boolean)) {
        const tx = await usdc.mint(wallet, amount);
        await tx.wait();
        console.log(`Minted ${amount} to ${wallet}`);
    }
}

main().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});
