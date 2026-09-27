package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.DividendSummaryItem;
import com.tokenrealty.reporting.dto.ReportingDtos.DividendsResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.OccupancyResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.RegulatoryExportItem;
import com.tokenrealty.reporting.dto.ReportingDtos.RegulatoryExportResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.SurveillanceAlertItem;
import com.tokenrealty.reporting.dto.ReportingDtos.TaxSummaryItem;
import com.tokenrealty.reporting.dto.ReportingDtos.TradingSummaryResponse;
import com.tokenrealty.reporting.entity.SurveillanceAlertRecord;
import com.tokenrealty.reporting.entity.TaxSummaryRecord;
import com.tokenrealty.reporting.entity.DividendRecord;
import com.tokenrealty.reporting.entity.OrderMatchedRecord;
import com.tokenrealty.reporting.entity.RentCollectedRecord;
import com.tokenrealty.reporting.entity.TradeSettledRecord;
import com.tokenrealty.reporting.repository.DividendRecordRepository;
import com.tokenrealty.reporting.repository.FlatTokenizedRecordRepository;
import com.tokenrealty.reporting.repository.OrderMatchedRecordRepository;
import com.tokenrealty.reporting.repository.RentCollectedRecordRepository;
import com.tokenrealty.reporting.repository.SurveillanceAlertRecordRepository;
import com.tokenrealty.reporting.repository.TaxSummaryRecordRepository;
import com.tokenrealty.reporting.repository.TradeSettledRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportingQueryService {

    private final TradeSettledRecordRepository tradeSettledRecordRepository;
    private final OrderMatchedRecordRepository orderMatchedRecordRepository;
    private final DividendRecordRepository dividendRecordRepository;
    private final RentCollectedRecordRepository rentCollectedRecordRepository;
    private final FlatTokenizedRecordRepository flatTokenizedRecordRepository;
    private final TaxSummaryRecordRepository taxSummaryRecordRepository;
    private final SurveillanceAlertRecordRepository surveillanceAlertRecordRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public TradingSummaryResponse tradingSummary() {
        Instant now = clock.instant();
        return new TradingSummaryResponse(
                tradeSettledRecordRepository.count(),
                orderMatchedRecordRepository.count(),
                orderMatchedRecordRepository.sumTotalMatchedVolumeUsd(),
                now
        );
    }

    @Transactional(readOnly = true)
    public OccupancyResponse occupancy() {
        long tokenized = flatTokenizedRecordRepository.count();
        long occupied = rentCollectedRecordRepository.countDistinctOccupiedFlats();
        BigDecimal rate = tokenized == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(occupied)
                        .divide(BigDecimal.valueOf(tokenized), 4, RoundingMode.HALF_UP);
        return new OccupancyResponse(tokenized, occupied, rate, clock.instant());
    }

    @Transactional(readOnly = true)
    public DividendsResponse dividends() {
        List<DividendRecord> records = dividendRecordRepository.findAllByOrderByDistributedAtDesc();
        BigDecimal total = records.stream()
                .map(DividendRecord::getTotalAmountUsd)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<DividendSummaryItem> items = records.stream()
                .map(record -> new DividendSummaryItem(
                        record.getId(),
                        record.getFlatId(),
                        record.getContractId(),
                        record.getPeriod(),
                        record.getTotalAmountUsd(),
                        record.getHolderCount() == null ? 0 : record.getHolderCount(),
                        record.getDistributedAt()
                ))
                .toList();
        return new DividendsResponse(items, total, clock.instant());
    }

    @Transactional(readOnly = true)
    public RegulatoryExportResponse regulatoryExport(String format) {
        List<RegulatoryExportItem> items = new ArrayList<>();

        for (TradeSettledRecord record : tradeSettledRecordRepository.findAll()) {
            items.add(new RegulatoryExportItem(
                    "TRADE_SETTLED",
                    record.getTradeId(),
                    record.getSettledAt(),
                    "orderId=" + record.getOrderId() + ", paymentId=" + record.getPaymentId()
            ));
        }
        for (OrderMatchedRecord record : orderMatchedRecordRepository.findAll()) {
            items.add(new RegulatoryExportItem(
                    "ORDER_MATCHED",
                    record.getOrderId(),
                    record.getMatchedAt(),
                    "flatId=" + record.getFlatId() + ", volumeUsd=" + record.getTotalPriceUsd()
            ));
        }
        for (DividendRecord record : dividendRecordRepository.findAll()) {
            items.add(new RegulatoryExportItem(
                    "DIVIDEND_DISTRIBUTED",
                    record.getContractId(),
                    record.getDistributedAt(),
                    "flatId=" + record.getFlatId() + ", period=" + record.getPeriod()
                            + ", totalUsd=" + record.getTotalAmountUsd()
            ));
        }
        for (RentCollectedRecord record : rentCollectedRecordRepository.findAll()) {
            items.add(new RegulatoryExportItem(
                    "RENT_COLLECTED",
                    record.getLeaseId(),
                    record.getCollectedAt(),
                    "flatId=" + record.getFlatId() + ", period=" + record.getPeriod()
                            + ", amountUsd=" + record.getAmountUsd()
            ));
        }

        items.sort(Comparator.comparing(RegulatoryExportItem::occurredAt));
        return new RegulatoryExportResponse(format, items, clock.instant());
    }

    @Transactional(readOnly = true)
    public Page<TaxSummaryItem> taxSummaries(UUID recipientInvestorId, Pageable pageable) {
        Page<TaxSummaryRecord> page = recipientInvestorId == null
                ? taxSummaryRecordRepository.findAll(pageable)
                : taxSummaryRecordRepository.findByRecipientInvestorId(recipientInvestorId, pageable);
        return page.map(this::toTaxSummaryItem);
    }

    @Transactional(readOnly = true)
    public Page<SurveillanceAlertItem> surveillanceAlerts(Pageable pageable) {
        return surveillanceAlertRecordRepository.findAll(pageable).map(this::toSurveillanceAlertItem);
    }

    @Transactional(readOnly = true)
    public long countSurveillanceAlerts() {
        return surveillanceAlertRecordRepository.count();
    }

    private TaxSummaryItem toTaxSummaryItem(TaxSummaryRecord record) {
        return new TaxSummaryItem(
                record.getId(),
                record.getPayoutId(),
                record.getRecipientInvestorId(),
                record.getGrossAmountUsd(),
                record.getWithholdingAmountUsd(),
                record.getNetAmountUsd(),
                record.getCompletedAt());
    }

    private SurveillanceAlertItem toSurveillanceAlertItem(SurveillanceAlertRecord record) {
        return new SurveillanceAlertItem(
                record.getId(),
                record.getOrderId(),
                record.getBuyerId(),
                record.getSellerId(),
                record.getAlertType(),
                record.getDetectedAt());
    }
}
