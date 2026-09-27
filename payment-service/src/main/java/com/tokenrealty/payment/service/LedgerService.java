package com.tokenrealty.payment.service;

import com.tokenrealty.payment.entity.*;
import com.tokenrealty.payment.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public void recordEscrowHold(UUID paymentId, BigDecimal amount, PaymentCurrency currency) {
        UUID transactionId = UUID.randomUUID();
        saveEntry(transactionId, paymentId, null, LedgerAccountCode.INVESTOR_AVAILABLE,
                LedgerEntry.EntryType.DEBIT, amount, currency);
        saveEntry(transactionId, paymentId, null, LedgerAccountCode.ESCROW,
                LedgerEntry.EntryType.CREDIT, amount, currency);
    }

    public void recordEscrowRelease(UUID paymentId, BigDecimal amount, PaymentCurrency currency) {
        UUID transactionId = UUID.randomUUID();
        saveEntry(transactionId, paymentId, null, LedgerAccountCode.ESCROW,
                LedgerEntry.EntryType.DEBIT, amount, currency);
        saveEntry(transactionId, paymentId, null, LedgerAccountCode.SPV_COLLECTION,
                LedgerEntry.EntryType.CREDIT, amount, currency);
    }

    public void recordBorrow(UUID loanId, BigDecimal amount, PaymentCurrency currency) {
        UUID transactionId = UUID.randomUUID();
        saveEntry(transactionId, null, null, LedgerAccountCode.LOAN_PRINCIPAL,
                LedgerEntry.EntryType.DEBIT, amount, currency, loanId);
        saveEntry(transactionId, null, null, LedgerAccountCode.INVESTOR_AVAILABLE,
                LedgerEntry.EntryType.CREDIT, amount, currency, loanId);
    }

    public void recordLiquidation(UUID loanId, BigDecimal seizedValueUsd, PaymentCurrency currency) {
        UUID transactionId = UUID.randomUUID();
        saveEntry(transactionId, null, null, LedgerAccountCode.COLLATERAL,
                LedgerEntry.EntryType.DEBIT, seizedValueUsd, currency, loanId);
        saveEntry(transactionId, null, null, LedgerAccountCode.LOAN_PRINCIPAL,
                LedgerEntry.EntryType.CREDIT, seizedValueUsd, currency, loanId);
    }

    public void recordRepay(UUID loanId, BigDecimal principal, BigDecimal interest, PaymentCurrency currency) {
        UUID transactionId = UUID.randomUUID();
        BigDecimal total = principal.add(interest);
        saveEntry(transactionId, null, null, LedgerAccountCode.INVESTOR_AVAILABLE,
                LedgerEntry.EntryType.DEBIT, total, currency, loanId);
        saveEntry(transactionId, null, null, LedgerAccountCode.LOAN_PRINCIPAL,
                LedgerEntry.EntryType.CREDIT, principal, currency, loanId);
        if (interest.signum() > 0) {
            saveEntry(transactionId, null, null, LedgerAccountCode.LOAN_INTEREST,
                    LedgerEntry.EntryType.CREDIT, interest, currency, loanId);
        }
    }

    private void saveEntry(
            UUID transactionId,
            UUID paymentId,
            UUID payoutId,
            LedgerAccountCode accountCode,
            LedgerEntry.EntryType entryType,
            BigDecimal amount,
            PaymentCurrency currency
    ) {
        saveEntry(transactionId, paymentId, payoutId, accountCode, entryType, amount, currency, null);
    }

    private void saveEntry(
            UUID transactionId,
            UUID paymentId,
            UUID payoutId,
            LedgerAccountCode accountCode,
            LedgerEntry.EntryType entryType,
            BigDecimal amount,
            PaymentCurrency currency,
            UUID referenceId
    ) {
        LedgerEntry entry = LedgerEntry.builder()
                .transactionId(transactionId)
                .paymentId(paymentId)
                .payoutId(payoutId)
                .accountCode(accountCode)
                .entryType(entryType)
                .amount(amount)
                .currency(currency)
                .referenceId(referenceId)
                .build();
        ledgerEntryRepository.save(entry);
    }
}
