// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * On-chain KYC whitelist — synced from compliance-service via Token Issuance listeners.
 */
contract ComplianceRegistry is Ownable {
    struct Entry {
        bool whitelisted;
        uint256 expiresAt;
    }

    mapping(address => Entry) private entries;

    event WhitelistAdded(address indexed wallet, string country, uint256 expiresAt);
    event WhitelistRemoved(address indexed wallet);

    constructor(address operator) Ownable(operator) {}

    function addToWhitelist(address wallet, string calldata country, uint256 expiresAt) external onlyOwner {
        require(wallet != address(0), "Invalid wallet");
        entries[wallet] = Entry({whitelisted: true, expiresAt: expiresAt});
        emit WhitelistAdded(wallet, country, expiresAt);
    }

    function removeFromWhitelist(address wallet) external onlyOwner {
        delete entries[wallet];
        emit WhitelistRemoved(wallet);
    }

    function isWhitelisted(address wallet) external view returns (bool) {
        Entry memory entry = entries[wallet];
        if (!entry.whitelisted) {
            return false;
        }
        if (entry.expiresAt != 0 && block.timestamp > entry.expiresAt) {
            return false;
        }
        return true;
    }
}
