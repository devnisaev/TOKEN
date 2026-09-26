const { expect } = require("chai");
const { ethers } = require("hardhat");
const { time } = require("@nomicfoundation/hardhat-network-helpers");
const { deployComplianceRegistry } = require("./fixtures");

describe("ComplianceRegistry", function () {
    let operator;
    let investor;
    let registry;

    beforeEach(async function () {
        [operator, investor] = await ethers.getSigners();
        registry = await deployComplianceRegistry(operator);
    });

    it("starts with wallet not whitelisted", async function () {
        expect(await registry.isWhitelisted(investor.address)).to.equal(false);
    });

    it("addToWhitelist allows only owner", async function () {
        await expect(
            registry.connect(investor).addToWhitelist(investor.address, "KG", 0)
        ).to.be.revertedWithCustomError(registry, "OwnableUnauthorizedAccount");

        await registry.connect(operator).addToWhitelist(investor.address, "KG", 0);
        expect(await registry.isWhitelisted(investor.address)).to.equal(true);
    });

    it("rejects zero address whitelist", async function () {
        await expect(
            registry.connect(operator).addToWhitelist(ethers.ZeroAddress, "KG", 0)
        ).to.be.revertedWith("Invalid wallet");
    });

    it("removeFromWhitelist clears entry", async function () {
        await registry.connect(operator).addToWhitelist(investor.address, "KG", 0);
        await registry.connect(operator).removeFromWhitelist(investor.address);
        expect(await registry.isWhitelisted(investor.address)).to.equal(false);
    });

    it("expires whitelist after expiresAt timestamp", async function () {
        const expiresAt = (await time.latest()) + 3600;
        await registry.connect(operator).addToWhitelist(investor.address, "KG", expiresAt);
        expect(await registry.isWhitelisted(investor.address)).to.equal(true);

        await time.increaseTo(expiresAt + 1);
        expect(await registry.isWhitelisted(investor.address)).to.equal(false);
    });

    it("expiresAt zero means no expiry", async function () {
        await registry.connect(operator).addToWhitelist(investor.address, "KG", 0);
        await time.increase(365 * 24 * 60 * 60);
        expect(await registry.isWhitelisted(investor.address)).to.equal(true);
    });
});
