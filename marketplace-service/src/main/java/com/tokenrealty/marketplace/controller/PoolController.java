package com.tokenrealty.marketplace.controller;

import com.tokenrealty.marketplace.dto.MarketplaceDtos.*;
import com.tokenrealty.marketplace.service.LiquidityPoolService;
import com.tokenrealty.marketplace.service.PoolSwapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/pools")
@RequiredArgsConstructor
@Tag(name = "Liquidity Pools", description = "AMM pools for TIER_1 property tokens (Phase 14)")
public class PoolController {

    private final LiquidityPoolService liquidityPoolService;
    private final PoolSwapService poolSwapService;

    @GetMapping
    @Operation(summary = "List all liquidity pools")
    public List<LiquidityPoolResponse> list() {
        return liquidityPoolService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pool by id")
    public LiquidityPoolResponse getById(@PathVariable UUID id) {
        return liquidityPoolService.findById(id);
    }

    @GetMapping("/by-contract/{contractId}")
    @Operation(summary = "Get pool by token contract id")
    public LiquidityPoolResponse getByContract(@PathVariable UUID contractId) {
        return liquidityPoolService.findByContractId(contractId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a liquidity pool for a TIER_1 contract")
    public LiquidityPoolResponse create(@Valid @RequestBody CreateLiquidityPoolRequest request) {
        return liquidityPoolService.createPool(request);
    }

    @PostMapping("/{id}/seed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROPERTY_MANAGER')")
    @Operation(summary = "Seed initial pool liquidity")
    public LiquidityPoolResponse seed(
            @PathVariable UUID id,
            @Valid @RequestBody SeedLiquidityPoolRequest request) {
        return liquidityPoolService.seedPool(id, request);
    }

    @PostMapping("/{id}/liquidity")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Add liquidity to a pool")
    public LpPositionResponse addLiquidity(
            @PathVariable UUID id,
            @Valid @RequestBody AddLiquidityRequest request) {
        return liquidityPoolService.addLiquidity(id, request);
    }

    @PostMapping("/{id}/liquidity/withdraw")
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Withdraw LP shares from a pool")
    public LpPositionResponse withdrawLiquidity(
            @PathVariable UUID id,
            @Valid @RequestBody WithdrawLiquidityRequest request) {
        return liquidityPoolService.withdrawLiquidity(id, request);
    }

    @PostMapping("/{id}/quote")
    @Operation(summary = "Quote a pool swap without executing")
    public SwapQuoteResponse quote(
            @PathVariable UUID id,
            @Valid @RequestBody SwapQuoteRequest request) {
        return poolSwapService.quote(id, request);
    }

    @PostMapping("/{id}/swaps")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTOR') or hasRole('ADMIN')")
    @Operation(summary = "Execute a pool swap")
    public PoolSwapResponse swap(
            @PathVariable UUID id,
            @Valid @RequestBody ExecuteSwapRequest request) {
        return poolSwapService.executeSwap(id, request);
    }

    @GetMapping("/{id}/swaps")
    @Operation(summary = "List recent swaps for a pool")
    public Page<PoolSwapResponse> listSwaps(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return poolSwapService.listSwaps(id, pageable);
    }
}
