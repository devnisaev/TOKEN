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

    private void saveEntry(
            UUID transactionId,
            UUID paymentId,
            UUID payoutId,
            LedgerAccountCode accountCode,
            LedgerEntry.EntryType entryType,
            BigDecimal amount,
            PaymentCurrency currency
    ) {
        LedgerEntry entry = LedgerEntry.builder()
                .transactionId(transactionId)
                .paymentId(paymentId)
                .payoutId(payoutId)
                .accountCode(accountCode)
                .entryType(entryType)
                .amount(amount)
                .currency(currency)
                .build();
        ledgerEntryRepository.save(entry);
    }
}
