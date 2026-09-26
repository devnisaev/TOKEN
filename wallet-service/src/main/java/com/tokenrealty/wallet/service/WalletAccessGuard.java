package com.tokenrealty.wallet.service;

import com.tokenrealty.security.TokenPrincipal;
import com.tokenrealty.security.UserRole;
import com.tokenrealty.web.exception.ValidationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WalletAccessGuard {

    public void checkInvestorAccess(UUID investorId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof TokenPrincipal principal)) {
            throw new ValidationException("Authentication required");
        }
        if (principal.role() == UserRole.ADMIN || principal.role() == UserRole.SERVICE) {
            return;
        }
        if (!investorId.equals(principal.userId())) {
            throw new ValidationException("Access denied for investor " + investorId);
        }
    }
}
