// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC20/utils/SafeERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

/**
 * MVP dividend distributor — owner deposits USDC; token holders claim pro-rata.
 */
contract DividendDistributor is Ownable, ReentrancyGuard {
    using SafeERC20 for IERC20;

    IERC20 public immutable usdc;
    IERC20 public immutable propertyToken;

    uint256 public totalDeposited;
    uint256 public supplySnapshot;
    mapping(address => uint256) public claimed;

    event Deposited(address indexed owner, uint256 amount, uint256 supplySnapshot);
    event Claimed(address indexed holder, uint256 amount);

    constructor(address operator, address usdcToken, address token) Ownable(operator) {
        require(usdcToken != address(0) && token != address(0), "Zero address");
        usdc = IERC20(usdcToken);
        propertyToken = IERC20(token);
    }

    function deposit(uint256 amount) external onlyOwner {
        require(amount > 0, "Zero amount");
        uint256 supply = propertyToken.totalSupply();
        require(supply > 0, "No token supply");

        usdc.safeTransferFrom(msg.sender, address(this), amount);
        totalDeposited += amount;
        supplySnapshot = supply;

        emit Deposited(msg.sender, amount, supply);
    }

    function claimable(address holder) public view returns (uint256) {
        if (supplySnapshot == 0 || totalDeposited == 0) {
            return 0;
        }
        uint256 balance = propertyToken.balanceOf(holder);
        if (balance == 0) {
            return 0;
        }
        uint256 owed = (totalDeposited * balance) / supplySnapshot;
        if (owed <= claimed[holder]) {
            return 0;
        }
        return owed - claimed[holder];
    }

    function claim() external nonReentrant {
        uint256 amount = claimable(msg.sender);
        require(amount > 0, "Nothing to claim");
        claimed[msg.sender] += amount;
        usdc.safeTransfer(msg.sender, amount);
        emit Claimed(msg.sender, amount);
    }
}
