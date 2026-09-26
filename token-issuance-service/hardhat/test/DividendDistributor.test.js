const { expect } = require("chai");
const { ethers } = require("hardhat");
const {
    deployComplianceRegistry,
    deployMockUsdc,
    deployPropertyToken,
    whitelist,
} = require("./fixtures");

describe("DividendDistributor", function () {
    let operator;
    let holder;
    let outsider;
    let usdc;
    let token;
    let distributor;

    beforeEach(async function () {
        [operator, holder, outsider] = await ethers.getSigners();
        const registry = await deployComplianceRegistry(operator);
        usdc = await deployMockUsdc(operator);
        const deployed = await deployPropertyToken(operator, registry, operator, {
            totalSupply: 1000n,
        });
        token = deployed.token;

        await whitelist(registry, operator, operator);
        await whitelist(registry, operator, holder);
        await token.connect(operator).enableTransfers();
        await token.connect(operator).operatorTransfer(operator.address, holder.address, 400n);

        const Factory = await ethers.getContractFactory("DividendDistributor");
        distributor = await Factory.deploy(
            operator.address,
            await usdc.getAddress(),
            await token.getAddress()
        );
        await distributor.waitForDeployment();
    });

    it("allows owner to deposit USDC", async function () {
        const depositAmount = 1_000_000n;
        await usdc.connect(operator).mint(operator.address, depositAmount);
        await usdc.connect(operator).approve(await distributor.getAddress(), depositAmount);

        await expect(distributor.connect(operator).deposit(depositAmount))
            .to.emit(distributor, "Deposited")
            .withArgs(operator.address, depositAmount, 1000n);

        expect(await usdc.balanceOf(await distributor.getAddress())).to.equal(depositAmount);
    });

    it("pays holders pro-rata on claim", async function () {
        const depositAmount = 1_000_000n;
        await usdc.connect(operator).mint(operator.address, depositAmount);
        await usdc.connect(operator).approve(await distributor.getAddress(), depositAmount);
        await distributor.connect(operator).deposit(depositAmount);

        const expected = (depositAmount * 400n) / 1000n;
        expect(await distributor.claimable(holder.address)).to.equal(expected);

        await expect(distributor.connect(holder).claim())
            .to.emit(distributor, "Claimed")
            .withArgs(holder.address, expected);

        expect(await usdc.balanceOf(holder.address)).to.equal(expected);
        expect(await distributor.claimable(holder.address)).to.equal(0n);
    });

    it("rejects claim when holder has no tokens", async function () {
        const depositAmount = 500_000n;
        await usdc.connect(operator).mint(operator.address, depositAmount);
        await usdc.connect(operator).approve(await distributor.getAddress(), depositAmount);
        await distributor.connect(operator).deposit(depositAmount);

        await expect(distributor.connect(outsider).claim()).to.be.revertedWith("Nothing to claim");
    });
});
