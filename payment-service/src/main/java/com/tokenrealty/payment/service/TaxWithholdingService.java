package com.tokenrealty.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TaxWithholdingService {

    private final BigDecimal withholdingRate;

    public TaxWithholdingService(
            @Value("${tokenrealty.payment.tax.withholding-rate:0}") BigDecimal withholdingRate) {
        this.withholdingRate = withholdingRate == null ? BigDecimal.ZERO : withholdingRate;
    }

    public BigDecimal calculateWithholding(BigDecimal grossAmount) {
        if (withholdingRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return grossAmount.multiply(withholdingRate).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isEnabled() {
        return withholdingRate.compareTo(BigDecimal.ZERO) > 0;
    }
}
