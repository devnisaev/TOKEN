package com.tokenrealty.wallet.service;

import com.tokenrealty.security.TokenPrincipal;
import com.tokenrealty.security.UserRole;
import com.tokenrealty.wallet.client.IssuanceClient;
import com.tokenrealty.wallet.client.PaymentClient;
import com.tokenrealty.wallet.dto.WalletDtos.*;
import com.tokenrealty.wallet.entity.InvestorWallet;
import com.tokenrealty.wallet.entity.InvestorWallet.WalletType;
import com.tokenrealty.wallet.crypto.KmsWalletEncryptionService;
import com.tokenrealty.wallet.repository.InvestorWalletRepository;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final InvestorWalletRepository repository;
    private final KmsWalletEncryptionService encryptionService;
    private final PaymentClient paymentClient;
    private final IssuanceClient issuanceClient;
    private final WalletAccessGuard accessGuard;

    public List<WalletResponse> listWallets(UUID investorId) {
        accessGuard.checkInvestorAccess(investorId);
        return repository.findByInvestorIdOrderByPrimaryDescCreatedAtAsc(investorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WalletResponse createCustodialWallet(CreateCustodialWalletRequest request) {
        UUID investorId = resolveInvestorId(request.investorId());
        accessGuard.checkInvestorAccess(investorId);
        if (repository.existsByInvestorIdAndWalletType(investorId, WalletType.CUSTODIAL)) {
            throw new ConflictException("Investor already has a custodial wallet");
        }

        ECKeyPair keyPair = createKeyPair();
        String address = "0x" + Keys.getAddress(keyPair);
        String privateKeyHex = keyPair.getPrivateKey().toString(16);
        boolean primary = repository.findByInvestorIdAndPrimaryTrue(investorId).isEmpty();

        InvestorWallet wallet = repository.save(InvestorWallet.builder()
                .investorId(investorId)
                .walletAddress(address.toLowerCase())
                .walletType(WalletType.CUSTODIAL)
                .encryptedPrivateKey(encryptionService.encrypt(privateKeyHex))
                .label(request.label() != null ? request.label() : "Custodial wallet")
                .primary(primary)
                .build());
        log.info("Created custodial wallet for investor {}", investorId);
        return toResponse(wallet);
    }

    public ConnectSessionResponse createConnectSession(ConnectSessionRequest request) {
        UUID investorId = resolveInvestorId(request.investorId());
        accessGuard.checkInvestorAccess(investorId);
        String sessionTopic = "wc:" + UUID.randomUUID();
        String uri = sessionTopic + "@2?relay-protocol=irn&symKey=stub";
        log.info("Created WalletConnect v2 session stub for investor {}", investorId);
        return new ConnectSessionResponse(sessionTopic, uri);
    }

    @Transactional
    public WalletResponse linkWallet(LinkWalletRequest request) {
        accessGuard.checkInvestorAccess(request.investorId());
        String normalized = request.walletAddress().toLowerCase();
        if (repository.existsByWalletAddressIgnoreCase(normalized)) {
            throw new ConflictException("Wallet address already registered");
        }
        if (request.primary()) {
            clearPrimaryFlag(request.investorId());
        }
        InvestorWallet wallet = repository.save(InvestorWallet.builder()
                .investorId(request.investorId())
                .walletAddress(normalized)
                .walletType(WalletType.LINKED)
                .label(request.label() != null ? request.label() : "Linked wallet")
                .primary(request.primary())
                .build());
        log.info("Linked external wallet for investor {}", request.investorId());
        return toResponse(wallet);
    }

    public AggregateBalanceResponse getAggregateBalance(UUID investorId) {
        accessGuard.checkInvestorAccess(investorId);
        var paymentBalance = paymentClient.getWalletBalance(investorId);
        var holdings = issuanceClient.getHoldings(investorId).stream()
                .map(h -> new TokenHoldingView(
                        h.contractId(), h.tokenSymbol(), h.walletAddress(), h.balance()))
                .toList();
        String primary = repository.findByInvestorIdAndPrimaryTrue(investorId)
                .map(InvestorWallet::getWalletAddress)
                .orElse(holdings.isEmpty() ? null : holdings.getFirst().walletAddress());

        return new AggregateBalanceResponse(
                investorId,
                primary,
                List.of(new FiatBalanceView(
                        paymentBalance.currency(),
                        paymentBalance.availableBalance(),
                        paymentBalance.heldBalance())),
                holdings);
    }

    @Transactional(readOnly = true)
    public InvestorWallet getCustodialWallet(UUID investorId) {
        accessGuard.checkInvestorAccess(investorId);
        return repository.findByInvestorIdAndWalletType(investorId, WalletType.CUSTODIAL)
                .orElseThrow(() -> new ResourceNotFoundException("Custodial wallet", investorId));
    }

    private UUID resolveInvestorId(UUID requested) {
        if (requested != null) {
            return requested;
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TokenPrincipal principal
                && principal.role() != UserRole.ADMIN && principal.role() != UserRole.SERVICE) {
            return principal.userId();
        }
        throw new ConflictException("investorId is required for admin/service requests");
    }

    private void clearPrimaryFlag(UUID investorId) {
        repository.findByInvestorIdOrderByPrimaryDescCreatedAtAsc(investorId).stream()
                .filter(InvestorWallet::isPrimary)
                .forEach(w -> {
                    w.setPrimary(false);
                    repository.save(w);
                });
    }

    private static ECKeyPair createKeyPair() {
        try {
            return Keys.createEcKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate wallet keypair", ex);
        }
    }

    private WalletResponse toResponse(InvestorWallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getInvestorId(),
                wallet.getWalletAddress(),
                wallet.getWalletType(),
                wallet.getLabel(),
                wallet.isPrimary(),
                wallet.getCreatedAt());
    }
}
