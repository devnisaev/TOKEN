package com.tokenrealty.corporateactions.service;

import com.tokenrealty.corporateactions.dto.CorporateActionDtos.*;
import com.tokenrealty.corporateactions.entity.IndexConstituent;
import com.tokenrealty.corporateactions.entity.IndexDefinition;
import com.tokenrealty.corporateactions.repository.IndexConstituentRepository;
import com.tokenrealty.corporateactions.repository.IndexDefinitionRepository;
import com.tokenrealty.web.exception.ConflictException;
import com.tokenrealty.web.exception.ResourceNotFoundException;
import com.tokenrealty.web.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndexBasketService {

    private final IndexDefinitionRepository indexDefinitionRepository;
    private final IndexConstituentRepository indexConstituentRepository;

    public List<IndexDefinitionResponse> findAll() {
        return indexDefinitionRepository.findAll().stream().map(this::toResponse).toList();
    }

    public IndexDefinitionResponse findById(UUID id) {
        return toResponse(getIndex(id));
    }

    @Transactional
    public IndexDefinitionResponse create(CreateIndexDefinitionRequest request) {
        if (indexDefinitionRepository.findBySymbol(request.symbol()).isPresent()) {
            throw new ConflictException("Index symbol already exists: " + request.symbol());
        }
        validateWeights(request.constituents());
        IndexDefinition index = indexDefinitionRepository.save(IndexDefinition.builder()
                .name(request.name())
                .symbol(request.symbol())
                .description(request.description())
                .indexContractId(request.indexContractId())
                .status(IndexDefinition.IndexStatus.DRAFT)
                .build());
        saveConstituents(index.getId(), request.constituents());
        return toResponse(index);
    }

    @Transactional
    public IndexDefinitionResponse rebalance(UUID indexId, RebalanceIndexRequest request) {
        IndexDefinition index = getIndex(indexId);
        validateWeights(request.constituents());
        index.setStatus(IndexDefinition.IndexStatus.REBALANCING);
        indexDefinitionRepository.save(index);
        indexConstituentRepository.deleteByIndexId(indexId);
        saveConstituents(indexId, request.constituents());
        index.setStatus(IndexDefinition.IndexStatus.ACTIVE);
        return toResponse(indexDefinitionRepository.save(index));
    }

    @Transactional
    public IndexDefinitionResponse activate(UUID indexId) {
        IndexDefinition index = getIndex(indexId);
        if (indexConstituentRepository.findByIndexIdOrderByWeightBpsDesc(indexId).isEmpty()) {
            raiseValidation("Index must have constituents before activation");
        }
        index.setStatus(IndexDefinition.IndexStatus.ACTIVE);
        return toResponse(indexDefinitionRepository.save(index));
    }

    private void saveConstituents(UUID indexId, List<IndexConstituentRequest> constituents) {
        for (IndexConstituentRequest c : constituents) {
            indexConstituentRepository.save(IndexConstituent.builder()
                    .indexId(indexId)
                    .contractId(c.contractId())
                    .weightBps(c.weightBps())
                    .build());
        }
    }

    private static void validateWeights(List<IndexConstituentRequest> constituents) {
        if (constituents == null || constituents.isEmpty()) {
            raiseValidation("Index must have at least one constituent");
        }
        int total = constituents.stream().mapToInt(IndexConstituentRequest::weightBps).sum();
        if (total != 10_000) {
            raiseValidation("Constituent weights must sum to 10000 bps (100%)");
        }
    }

    private IndexDefinition getIndex(UUID id) {
        return indexDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IndexDefinition", id));
    }

    private IndexDefinitionResponse toResponse(IndexDefinition index) {
        List<IndexConstituentResponse> constituents = indexConstituentRepository
                .findByIndexIdOrderByWeightBpsDesc(index.getId()).stream()
                .map(c -> new IndexConstituentResponse(c.getContractId(), c.getWeightBps()))
                .toList();
        return IndexDefinitionResponse.builder()
                .id(index.getId())
                .name(index.getName())
                .symbol(index.getSymbol())
                .description(index.getDescription())
                .indexContractId(index.getIndexContractId())
                .status(index.getStatus())
                .constituents(constituents)
                .createdAt(index.getCreatedAt())
                .build();
    }

    private static void raiseValidation(String message) {
        throw new ValidationException(message);
    }
}
