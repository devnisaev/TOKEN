// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

/**
 * Minimal PropertyToken for local Hardhat dev — replace with ERC-1400 + compliance hooks in production.
 */
contract PropertyToken is ERC20 {
    address public immutable spvWallet;

    constructor(
        string memory name_,
        string memory symbol_,
        uint256 totalSupply_,
        address spvWallet_
    ) ERC20(name_, symbol_) {
        require(spvWallet_ != address(0), "SPV wallet required");
        spvWallet = spvWallet_;
        _mint(spvWallet_, totalSupply_);
    }
}
