package com.tokenrealty.corporateactions.service;

import com.tokenrealty.corporateactions.client.TokenIssuanceClient;
import com.tokenrealty.corporateactions.dto.CorporateActionDtos.CorporateActionView;
import com.tokenrealty.corporateactions.entity.CorporateAction;
import com.tokenrealty.corporateactions.entity.CorporateActionStatus;
import com.tokenrealty.corporateactions.entity.CorporateActionType;
import com.tokenrealty.corporateactions.kafka.command.DividendDistributedCommand;
import com.tokenrealty.corporateactions.kafka.command.RentCollectedCommand;
import com.tokenrealty.corporateactions.kafka.events.DividendDistributionRequestedEvent;
import com.tokenrealty.corporateactions.kafka.port.DividendDistributionRequestedPublisher;
import com.tokenrealty.corporateactions.repository.CorporateActionRepository;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CorporateActionsService {

    private final CorporateActionRepository repository;
    private final TokenIssuanceClient tokenIssuanceClient;
    private final DividendDistributionRequestedPublisher dividendDistributionRequestedPublisher;

    @Transactional
    public void onRentCollected(RentCollectedCommand command) {
        UUID contractId = resolveContractId(command.flatId(), command.contractId());
        CorporateAction action = repository.save(CorporateAction.builder()
                .type(CorporateActionType.DIVIDEND)
                .flatId(command.flatId())
                .contractId(contractId)
                .period(command.period())
                .grossAmountUsd(command.grossAmountUsd())
                .status(CorporateActionStatus.REQUESTED)
                .sourceEventId(command.eventId())
                .build());

        dividendDistributionRequestedPublisher.publish(new DividendDistributionRequestedEvent(
                action.getId(),
                action.getFlatId(),
                action.getContractId(),
                action.getPeriod(),
                action.getGrossAmountUsd()));
    }

    @Transactional
    public void onDividendDistributed(DividendDistributedCommand command) {
        repository.findFirstByFlatIdAndPeriodAndTypeAndStatus(
                        command.flatId(),
                        command.period(),
                        CorporateActionType.DIVIDEND,
                        CorporateActionStatus.REQUESTED)
                .ifPresent(action -> {
                    if (action.getContractId() == null && command.contractId() != null) {
                        action.setContractId(command.contractId());
                    }
                    action.setStatus(CorporateActionStatus.COMPLETED);
                    repository.save(action);
                });
    }

    @Transactional(readOnly = true)
    public Page<CorporateActionView> listDividends(Pageable pageable) {
        return repository.findByTypeOrderByCreatedAtDesc(CorporateActionType.DIVIDEND, pageable)
                .map(CorporateActionView::from);
    }

    @Transactional(readOnly = true)
    public CorporateActionView getById(UUID actionId) {
        return repository.findById(actionId)
                .map(CorporateActionView::from)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate action not found: " + actionId));
    }

    private UUID resolveContractId(UUID flatId, UUID contractIdFromEvent) {
        if (contractIdFromEvent != null) {
            return contractIdFromEvent;
        }
        return tokenIssuanceClient.findContractIdByFlatId(flatId).orElse(null);
    }
}
