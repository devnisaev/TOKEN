// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

interface IComplianceRegistry {
    function isWhitelisted(address wallet) external view returns (bool);
}

/**
 * ERC-20 property token with compliance-gated transfers (local dev / testnet MVP).
 */
contract PropertyToken is ERC20, Ownable {
    IComplianceRegistry public immutable complianceRegistry;
    bytes32 public immutable flatId;
    bytes32 public immutable buildingId;
    uint256 public immutable tokenPriceUsd;
    bool public transfersEnabled;

    constructor(
        string memory name_,
        string memory symbol_,
        uint256 totalSupply_,
        uint256 tokenPriceUsd_,
        address operator_,
        address complianceRegistry_,
        string memory flatId_,
        string memory buildingId_,
        address spvWallet_
    ) ERC20(name_, symbol_) Ownable(operator_) {
        require(complianceRegistry_ != address(0), "Compliance registry required");
        require(spvWallet_ != address(0), "SPV wallet required");
        complianceRegistry = IComplianceRegistry(complianceRegistry_);
        flatId = keccak256(bytes(flatId_));
        buildingId = keccak256(bytes(buildingId_));
        tokenPriceUsd = tokenPriceUsd_;
        _mint(spvWallet_, totalSupply_);
    }

    function enableTransfers() external onlyOwner {
        transfersEnabled = true;
    }

    function suspendTransfers() external onlyOwner {
        transfersEnabled = false;
    }

    function _update(address from, address to, uint256 value) internal override {
        if (from != address(0) && to != address(0)) {
            require(transfersEnabled, "Transfers disabled");
            require(complianceRegistry.isWhitelisted(from), "Sender not whitelisted");
            require(complianceRegistry.isWhitelisted(to), "Recipient not whitelisted");
        }
        super._update(from, to, value);
    }
}
