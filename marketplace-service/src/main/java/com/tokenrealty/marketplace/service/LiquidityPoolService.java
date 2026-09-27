package com.tokenrealty.marketplace.service;

import com.tokenrealty.marketplace.amm.AmmMath;
import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.entity.LiquidityPool;
import com.tokenrealty.marketplace.entity.LpPosition;
import com.tokenrealty.marketplace.exchange.ExchangeLiquidityRules;
import com.tokenrealty.marketplace.repository.LiquidityPoolRepository;
import com.tokenrealty.marketplace.repository.LpPositionRepository;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiquidityPoolService {

    private final LiquidityPoolRepository liquidityPoolRepository;
    private final LpPositionRepository lpPositionRepository;
    private final Clock clock;

    public List<LiquidityPoolResponse> findAll() {
        return liquidityPoolRepository.findAll().stream().map(this::toResponse).toList();
    }

    public LiquidityPoolResponse findByContractId(UUID contractId) {
        return toResponse(getByContractId(contractId));
    }

    public LiquidityPoolResponse findById(UUID id) {
        return toResponse(getById(id));
    }

    @Transactional
    public LiquidityPoolResponse createPool(CreateLiquidityPoolRequest request) {
        if (liquidityPoolRepository.findByContractId(request.contractId()).isPresent()) {
            throw new ConflictException("Pool already exists for contract " + request.contractId());
        }
        String tier = request.liquidityTier() != null ? request.liquidityTier() : "TIER_1";
        ExchangeLiquidityRules.assertClobAllowed(tier);
        if (!"TIER_1".equals(tier)) {
            throw new ValidationException("AMM pools are only supported for TIER_1 assets");
        }
        LiquidityPool pool = liquidityPoolRepository.save(LiquidityPool.builder()
                .contractId(request.contractId())
                .flatId(request.flatId())
                .buildingId(request.buildingId())
                .feeBps(request.feeBps() != null ? request.feeBps() : 30)
                .navBreakPct(request.navBreakPct() != null ? request.navBreakPct() : new BigDecimal("10"))
                .liquidityTier(tier)
                .lpLockDays(request.lpLockDays() != null ? request.lpLockDays() : 0)
                .build());
        return toResponse(pool);
    }

    @Transactional
    public LiquidityPoolResponse seedPool(UUID poolId, SeedLiquidityPoolRequest request) {
        LiquidityPool pool = getById(poolId);
        assertActiveOrEmpty(pool);
        if (pool.getTokenReserve() > 0 || pool.getUsdcReserve().signum() > 0) {
            throw new ConflictException("Pool already seeded — use add liquidity");
        }
        pool.setTokenReserve(request.tokenAmount());
        pool.setUsdcReserve(request.usdcAmount());
        pool.setTotalLpShares(AmmMath.mintInitialShares(request.tokenAmount(), request.usdcAmount()));
        liquidityPoolRepository.save(pool);

        lpPositionRepository.save(LpPosition.builder()
                .poolId(pool.getId())
                .investorId(request.investorId())
                .lpShares(pool.getTotalLpShares())
                .depositedAt(clock.instant())
                .build());
        return toResponse(pool);
    }

    @Transactional
    public LpPositionResponse addLiquidity(UUID poolId, AddLiquidityRequest request) {
        LiquidityPool pool = getById(poolId);
        assertSwappable(pool);
        BigDecimal shares = AmmMath.mintProportionalShares(
                request.tokenAmount(),
                request.usdcAmount(),
                pool.getTokenReserve(),
                pool.getUsdcReserve(),
                pool.getTotalLpShares());
        if (shares.signum() <= 0) {
            raiseValidation("Deposit too small for proportional LP shares");
        }
        pool.setTokenReserve(pool.getTokenReserve() + request.tokenAmount());
        pool.setUsdcReserve(pool.getUsdcReserve().add(request.usdcAmount()));
        pool.setTotalLpShares(pool.getTotalLpShares().add(shares));
        liquidityPoolRepository.save(pool);

        LpPosition position = lpPositionRepository.findByPoolIdAndInvestorId(poolId, request.investorId())
                .orElseGet(() -> LpPosition.builder()
                        .poolId(poolId)
                        .investorId(request.investorId())
                        .lpShares(BigDecimal.ZERO)
                        .depositedAt(clock.instant())
                        .build());
        position.setLpShares(position.getLpShares().add(shares));
        if (position.getDepositedAt() == null) {
            position.setDepositedAt(clock.instant());
        }
        return toLpResponse(lpPositionRepository.save(position), pool);
    }

    @Transactional
    public LpPositionResponse withdrawLiquidity(UUID poolId, WithdrawLiquidityRequest request) {
        LiquidityPool pool = getById(poolId);
        LpPosition position = lpPositionRepository.findByPoolIdAndInvestorId(poolId, request.investorId())
                .orElseThrow(() -> new ResourceNotFoundException("LpPosition", request.investorId()));
        if (position.getLpShares().compareTo(request.lpShares()) < 0) {
            raiseValidation("Insufficient LP shares");
        }
        if (pool.getLpLockDays() > 0) {
            java.time.Instant unlockAt = position.getDepositedAt().plusSeconds(pool.getLpLockDays() * 86400L);
            if (clock.instant().isBefore(unlockAt)) {
                raiseValidation("LP lock period not elapsed");
            }
        }
        BigDecimal shareRatio = request.lpShares()
                .divide(pool.getTotalLpShares(), 12, java.math.RoundingMode.HALF_UP);
        long tokensOut = shareRatio.multiply(BigDecimal.valueOf(pool.getTokenReserve()))
                .setScale(0, java.math.RoundingMode.DOWN).longValue();
        BigDecimal usdcOut = pool.getUsdcReserve().multiply(shareRatio)
                .setScale(2, java.math.RoundingMode.DOWN);

        pool.setTokenReserve(pool.getTokenReserve() - tokensOut);
        pool.setUsdcReserve(pool.getUsdcReserve().subtract(usdcOut));
        pool.setTotalLpShares(pool.getTotalLpShares().subtract(request.lpShares()));
        liquidityPoolRepository.save(pool);

        position.setLpShares(position.getLpShares().subtract(request.lpShares()));
        return toLpResponse(lpPositionRepository.save(position), pool);
    }

    LiquidityPool getById(UUID id) {
        return liquidityPoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LiquidityPool", id));
    }

    LiquidityPool getByContractId(UUID contractId) {
        return liquidityPoolRepository.findByContractId(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("LiquidityPool for contract", contractId));
    }

    private static void assertSwappable(LiquidityPool pool) {
        if (pool.getStatus() != LiquidityPool.PoolStatus.ACTIVE) {
            raiseValidation("Pool is not active for swaps or liquidity changes");
        }
        if (pool.getTokenReserve() <= 0 || pool.getUsdcReserve().signum() <= 0) {
            raiseValidation("Pool has no liquidity");
        }
    }

    private static void assertActiveOrEmpty(LiquidityPool pool) {
        if (pool.getStatus() == LiquidityPool.PoolStatus.CLOSED) {
            raiseValidation("Pool is closed");
        }
    }

    private LiquidityPoolResponse toResponse(LiquidityPool pool) {
        return LiquidityPoolResponse.builder()
                .id(pool.getId())
                .contractId(pool.getContractId())
                .flatId(pool.getFlatId())
                .buildingId(pool.getBuildingId())
                .tokenReserve(pool.getTokenReserve())
                .usdcReserve(pool.getUsdcReserve())
                .totalLpShares(pool.getTotalLpShares())
                .spotPriceUsd(AmmMath.spotPrice(pool.getTokenReserve(), pool.getUsdcReserve()))
                .feeBps(pool.getFeeBps())
                .navBreakPct(pool.getNavBreakPct())
                .liquidityTier(pool.getLiquidityTier())
                .status(pool.getStatus())
                .lastNavPerTokenUsd(pool.getLastNavPerTokenUsd())
                .lastNavCheckedAt(pool.getLastNavCheckedAt())
                .lpLockDays(pool.getLpLockDays())
                .createdAt(pool.getCreatedAt())
                .build();
    }

    private LpPositionResponse toLpResponse(LpPosition position, LiquidityPool pool) {
        return new LpPositionResponse(
                position.getId(),
                position.getPoolId(),
                position.getInvestorId(),
                position.getLpShares(),
                pool.getTokenReserve(),
                pool.getUsdcReserve(),
                pool.getTotalLpShares(),
                position.getDepositedAt());
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
