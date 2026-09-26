package com.tokenrealty.web.exception;

/**
 * Raised when a trade or investment is blocked by KYC / compliance policy.
 */
public class ComplianceBlockedException extends RuntimeException {

    public ComplianceBlockedException(String message) {
        super(message);
    }
}
