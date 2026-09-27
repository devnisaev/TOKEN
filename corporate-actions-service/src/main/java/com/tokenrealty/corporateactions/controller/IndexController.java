package com.tokenrealty.corporateactions.controller;

import com.tokenrealty.corporateactions.dto.CorporateActionDtos.IndexDefinitionResponse;
import com.tokenrealty.corporateactions.dto.CorporateActionDtos.*;
import com.tokenrealty.corporateactions.entity.IndexDividendAccrual;
import com.tokenrealty.corporateactions.repository.IndexDividendAccrualRepository;
import com.tokenrealty.corporateactions.service.IndexBasketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/corporate-actions/indices")
@RequiredArgsConstructor
public class IndexController {

    private final IndexBasketService indexBasketService;
    private final IndexDividendAccrualRepository indexDividendAccrualRepository;

    @GetMapping
    public List<IndexDefinitionResponse> list() {
        return indexBasketService.findAll();
    }

    @GetMapping("/{id}")
    public IndexDefinitionResponse getById(@PathVariable UUID id) {
        return indexBasketService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public IndexDefinitionResponse create(@Valid @RequestBody CreateIndexDefinitionRequest request) {
        return indexBasketService.create(request);
    }

    @PostMapping("/{id}/rebalance")
    @PreAuthorize("hasRole('ADMIN')")
    public IndexDefinitionResponse rebalance(
            @PathVariable UUID id,
            @Valid @RequestBody RebalanceIndexRequest request) {
        return indexBasketService.rebalance(id, request);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public IndexDefinitionResponse activate(@PathVariable UUID id) {
        return indexBasketService.activate(id);
    }

    @GetMapping("/{id}/dividends")
    public List<IndexDividendAccrualResponse> listDividendAccruals(@PathVariable UUID id) {
        indexBasketService.findById(id);
        return indexDividendAccrualRepository.findByIndexIdOrderByCreatedAtDesc(id).stream()
                .map(this::toAccrualResponse)
                .toList();
    }

    private IndexDividendAccrualResponse toAccrualResponse(IndexDividendAccrual accrual) {
        return new IndexDividendAccrualResponse(
                accrual.getId(),
                accrual.getIndexId(),
                accrual.getConstituentContractId(),
                accrual.getSourcePayoutId(),
                accrual.getConstituentPayoutUsd(),
                accrual.getIndexShareUsd(),
                accrual.getWeightBps(),
                accrual.getCreatedAt());
    }
}
