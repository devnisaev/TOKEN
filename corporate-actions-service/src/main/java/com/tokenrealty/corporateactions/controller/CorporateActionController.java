package com.tokenrealty.corporateactions.controller;

import com.tokenrealty.corporateactions.dto.CorporateActionDtos.CorporateActionView;
import com.tokenrealty.corporateactions.service.CorporateActionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/corporate-actions")
@RequiredArgsConstructor
public class CorporateActionController {

    private final CorporateActionsService corporateActionsService;

    @GetMapping("/dividends")
    public Page<CorporateActionView> listDividends(@PageableDefault(size = 20) Pageable pageable) {
        return corporateActionsService.listDividends(pageable);
    }

    @GetMapping("/{actionId}")
    public CorporateActionView getById(@PathVariable UUID actionId) {
        return corporateActionsService.getById(actionId);
    }
}
