const { expect } = require("chai");
const { ethers } = require("hardhat");
const { deployComplianceRegistry, deployPropertyToken, whitelist } = require("./fixtures");

describe("PropertyToken", function () {
    let operator;
    let spv;
    let buyer;
    let outsider;
    let registry;
    let token;

    beforeEach(async function () {
        [operator, spv, buyer, outsider] = await ethers.getSigners();
        registry = await deployComplianceRegistry(operator);
        ({ token } = await deployPropertyToken(operator, registry, spv, { totalSupply: 1000n }));
    });

    it("mints total supply to SPV wallet", async function () {
        expect(await token.balanceOf(spv.address)).to.equal(1000n);
        expect(await token.totalSupply()).to.equal(1000n);
    });

    it("blocks transfers until enabled", async function () {
        await whitelist(registry, operator, spv);
        await whitelist(registry, operator, buyer);

        await expect(
            token.connect(operator).operatorTransfer(spv.address, buyer.address, 10n)
        ).to.be.revertedWith("Transfers disabled");
    });

    it("operatorTransfer moves tokens between whitelisted wallets", async function () {
        await whitelist(registry, operator, spv);
        await whitelist(registry, operator, buyer);
        await token.connect(operator).enableTransfers();

        await token.connect(operator).operatorTransfer(spv.address, buyer.address, 250n);

        expect(await token.balanceOf(spv.address)).to.equal(750n);
        expect(await token.balanceOf(buyer.address)).to.equal(250n);
    });

    it("rejects operatorTransfer to non-whitelisted recipient", async function () {
        await whitelist(registry, operator, spv);
        await token.connect(operator).enableTransfers();

        await expect(
            token.connect(operator).operatorTransfer(spv.address, outsider.address, 1n)
        ).to.be.revertedWith("Recipient not whitelisted");
    });

    it("rejects operatorTransfer from non-whitelisted sender", async function () {
        await whitelist(registry, operator, buyer);
        await token.connect(operator).enableTransfers();

        await expect(
            token.connect(operator).operatorTransfer(spv.address, buyer.address, 1n)
        ).to.be.revertedWith("Sender not whitelisted");
    });

    it("suspendTransfers blocks further operatorTransfer", async function () {
        await whitelist(registry, operator, spv);
        await whitelist(registry, operator, buyer);
        await token.connect(operator).enableTransfers();
        await token.connect(operator).suspendTransfers();

        await expect(
            token.connect(operator).operatorTransfer(spv.address, buyer.address, 1n)
        ).to.be.revertedWith("Transfers disabled");
    });

    it("only owner may call operatorTransfer", async function () {
        await whitelist(registry, operator, spv);
        await whitelist(registry, operator, buyer);
        await token.connect(operator).enableTransfers();

        await expect(
            token.connect(buyer).operatorTransfer(spv.address, buyer.address, 1n)
        ).to.be.revertedWithCustomError(token, "OwnableUnauthorizedAccount");
    });

    it("stores immutable flat and building ids", async function () {
        const flatId = ethers.keccak256(ethers.toUtf8Bytes("flat-uuid-1"));
        const buildingId = ethers.keccak256(ethers.toUtf8Bytes("building-uuid-1"));
        expect(await token.flatId()).to.equal(flatId);
        expect(await token.buildingId()).to.equal(buildingId);
    });
});
