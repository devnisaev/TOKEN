const { ethers } = require("hardhat");

async function deployComplianceRegistry(operator) {
    const Factory = await ethers.getContractFactory("ComplianceRegistry");
    const registry = await Factory.deploy(operator.address);
    await registry.waitForDeployment();
    return registry;
}

async function deployMockUsdc(operator) {
    const Factory = await ethers.getContractFactory("MockUSDC");
    const usdc = await Factory.deploy(operator.address);
    await usdc.waitForDeployment();
    return usdc;
}

async function deployPropertyToken(operator, complianceRegistry, spvWallet, overrides = {}) {
    const Factory = await ethers.getContractFactory("PropertyToken");
    const complianceAddress = await complianceRegistry.getAddress();
    const args = {
        name: overrides.name ?? "Test Flat Token",
        symbol: overrides.symbol ?? "TFT",
        totalSupply: overrides.totalSupply ?? 1000n,
        tokenPriceUsd: overrides.tokenPriceUsd ?? 100n,
        operator: operator.address,
        complianceRegistry: complianceAddress,
        flatId: overrides.flatId ?? "flat-uuid-1",
        buildingId: overrides.buildingId ?? "building-uuid-1",
        spvWallet: spvWallet.address,
    };
    const token = await Factory.deploy(
        args.name,
        args.symbol,
        args.totalSupply,
        args.tokenPriceUsd,
        args.operator,
        args.complianceRegistry,
        args.flatId,
        args.buildingId,
        args.spvWallet
    );
    await token.waitForDeployment();
    return { token, args };
}

async function whitelist(registry, operator, wallet, country = "KG", expiresAt = 0) {
    const tx = await registry.connect(operator).addToWhitelist(wallet.address, country, expiresAt);
    await tx.wait();
}

async function deployDividendDistributor(operator, usdc, propertyToken) {
    const Factory = await ethers.getContractFactory("DividendDistributor");
    const distributor = await Factory.deploy(
        operator.address,
        await usdc.getAddress(),
        await propertyToken.getAddress()
    );
    await distributor.waitForDeployment();
    return distributor;
}

module.exports = {
    deployComplianceRegistry,
    deployMockUsdc,
    deployPropertyToken,
    deployDividendDistributor,
    whitelist,
};
