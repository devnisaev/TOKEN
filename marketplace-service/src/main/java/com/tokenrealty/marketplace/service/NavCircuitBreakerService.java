package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.amm.AmmMath;
import com.tokenrealty.marketplace.entity.LiquidityPool;
import com.tokenrealty.marketplace.kafka.command.NavAttestedCommand;
import com.tokenrealty.marketplace.repository.LiquidityPoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NavCircuitBreakerService {

    private final LiquidityPoolRepository liquidityPoolRepository;

    @Transactional
    public void onNavAttested(NavAttestedCommand command) {
        List<LiquidityPool> pools = liquidityPoolRepository.findByFlatId(command.flatId());
        for (LiquidityPool pool : pools) {
            evaluatePool(pool, command.navPerTokenUsd(), command.attestedAt());
        }
    }

    @Transactional
    public void evaluatePool(LiquidityPool pool, BigDecimal navPerTokenUsd, java.time.Instant checkedAt) {
        pool.setLastNavPerTokenUsd(navPerTokenUsd);
        pool.setLastNavCheckedAt(checkedAt);
        if (pool.getTokenReserve() <= 0 || pool.getStatus() == LiquidityPool.PoolStatus.CLOSED) {
            liquidityPoolRepository.save(pool);
            return;
        }
        BigDecimal poolPrice = AmmMath.spotPrice(pool.getTokenReserve(), pool.getUsdcReserve());
        if (navPerTokenUsd == null || navPerTokenUsd.signum() <= 0 || poolPrice.signum() <= 0) {
            liquidityPoolRepository.save(pool);
            return;
        }
        BigDecimal deltaPct = poolPrice.subtract(navPerTokenUsd)
                .divide(navPerTokenUsd, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .abs();
        if (deltaPct.compareTo(pool.getNavBreakPct()) > 0
                && pool.getStatus() == LiquidityPool.PoolStatus.ACTIVE) {
            pool.setStatus(LiquidityPool.PoolStatus.PAUSED);
            log.warn(
                    "NAV circuit breaker paused pool {} — poolPrice={} nav={} deltaPct={}",
                    pool.getId(),
                    poolPrice,
                    navPerTokenUsd,
                    deltaPct);
        }
        liquidityPoolRepository.save(pool);
    }
}
