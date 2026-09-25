// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * Minimal compliance whitelist for local dev.
 */
contract ComplianceRegistry {
    mapping(address => bool) private whitelisted;

    function addToWhitelist(address wallet) external {
        whitelisted[wallet] = true;
    }

    function removeFromWhitelist(address wallet) external {
        whitelisted[wallet] = false;
    }

    function isWhitelisted(address wallet) external view returns (bool) {
        return whitelisted[wallet];
    }
}
