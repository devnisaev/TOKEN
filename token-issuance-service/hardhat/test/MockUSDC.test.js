const { expect } = require("chai");
const { ethers } = require("hardhat");
const { deployMockUsdc } = require("./fixtures");

describe("MockUSDC", function () {
    let operator;
    let recipient;
    let usdc;

    beforeEach(async function () {
        [operator, recipient] = await ethers.getSigners();
        usdc = await deployMockUsdc(operator);
    });

    it("uses 6 decimals", async function () {
        expect(await usdc.decimals()).to.equal(6);
    });

    it("mints only by owner", async function () {
        const amount = 1_000_000n;
        await usdc.connect(operator).mint(recipient.address, amount);
        expect(await usdc.balanceOf(recipient.address)).to.equal(amount);

        await expect(
            usdc.connect(recipient).mint(recipient.address, 1n)
        ).to.be.revertedWithCustomError(usdc, "OwnableUnauthorizedAccount");
    });

    it("supports standard ERC-20 transfer", async function () {
        const amount = 500_000n;
        await usdc.connect(operator).mint(operator.address, amount);
        await usdc.connect(operator).transfer(recipient.address, 100_000n);
        expect(await usdc.balanceOf(recipient.address)).to.equal(100_000n);
    });
});
