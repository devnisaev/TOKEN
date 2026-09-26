package com.tokenrealty.reporting.service;

import com.tokenrealty.reporting.dto.ReportingDtos.DividendSummaryItem;
import com.tokenrealty.reporting.dto.ReportingDtos.DividendsResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.OccupancyResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.RegulatoryExportItem;
import com.tokenrealty.reporting.dto.ReportingDtos.RegulatoryExportResponse;
import com.tokenrealty.reporting.dto.ReportingDtos.TradingSummaryResponse;
import com.tokenrealty.reporting.entity.DividendRecord;
import com.tokenrealty.reporting.entity.OrderMatchedRecord;
import com.tokenrealty.reporting.entity.RentCollectedRecord;
import com.tokenrealty.reporting.entity.TradeSettledRecord;
import com.tokenrealty.reporting.repository.DividendRecordRepository;
import com.tokenrealty.reporting.repository.FlatTokenizedRecordRepository;
import com.tokenrealty.reporting.repository.OrderMatchedRecordRepository;
import com.tokenrealty.reporting.repository.RentCollectedRecordRepository;
import com.tokenrealty.reporting.repository.TradeSettledRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
