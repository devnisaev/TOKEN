package com.tokenrealty.wallet.controller;

import com.tokenrealty.wallet.dto.WalletDtos.*;
import com.tokenrealty.wallet.service.CustodialSignService;
import com.tokenrealty.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Custodial and linked investor wallets")
public class WalletController {

    private final WalletService walletService;
    private final CustodialSignService custodialSignService;

    @GetMapping("/{investorId}")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN') or hasRole('SERVICE')")
    @Operation(summary = "List wallets for an investor")
    public List<WalletResponse> list(@PathVariable UUID investorId) {
        return walletService.listWallets(investorId);
    }

    @GetMapping("/{investorId}/balance")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN') or hasRole('SERVICE')")
    @Operation(summary = "Aggregate fiat + token holdings for an investor")
    public AggregateBalanceResponse balance(@PathVariable UUID investorId) {
        return walletService.getAggregateBalance(investorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Create a custodial wallet (encrypted key at rest)")
    public WalletResponse createCustodial(@Valid @RequestBody CreateCustodialWalletRequest request) {
        return walletService.createCustodialWallet(request);
    }

    @PostMapping("/link")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Link an external wallet (MetaMask / WalletConnect)")
    public WalletResponse link(@Valid @RequestBody LinkWalletRequest request) {
        return walletService.linkWallet(request);
    }

    @PostMapping("/connect-session")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Prepare WalletConnect v2 handshake (session topic + URI stub)")
    public ConnectSessionResponse connectSession(@Valid @RequestBody ConnectSessionRequest request) {
        return walletService.createConnectSession(request);
    }

    @PostMapping("/{investorId}/sign")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Sign a transaction with the investor custodial wallet")
    public SignTransactionResponse sign(
            @PathVariable UUID investorId,
            @Valid @RequestBody SignTransactionRequest request) {
        return custodialSignService.signTransaction(investorId, request);
    }
}
