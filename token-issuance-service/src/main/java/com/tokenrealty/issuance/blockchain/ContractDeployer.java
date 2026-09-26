package com.tokenrealty.issuance.blockchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.issuance.config.BlockchainProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Deploys PropertyToken contracts by invoking hardhat/scripts/deployFlat.js.
 */
@Component
@Slf4j
public class ContractDeployer {

    private final BlockchainProperties props;
    private final ObjectMapper objectMapper;

    public ContractDeployer(BlockchainProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public record DeploymentResult(
            String contractAddress,
            String txHash,
            Long blockNumber,
            String operatorAddress,
            String network,
            Long chainId
    ) {}

    public DeploymentResult deployPropertyToken(
            UUID flatId,
            UUID buildingId,
            String tokenName,
            String tokenSymbol,
            Long totalSupply,
            BigDecimal tokenPriceUsd,
            String spvWalletAddress
    ) throws Exception {

        long priceInCents = tokenPriceUsd.multiply(BigDecimal.valueOf(100)).longValue();
        String network = resolveHardhatNetwork();
        String hardhatDir = resolveHardhatDir();

        ProcessBuilder pb = new ProcessBuilder(
                "npx", "hardhat", "run", "scripts/deployFlat.js", "--network", network);
        pb.directory(new java.io.File(hardhatDir));
        pb.environment().put("FLAT_ID", flatId.toString());
        pb.environment().put("BUILDING_ID", buildingId.toString());
        pb.environment().put("TOKEN_NAME", tokenName);
        pb.environment().put("TOKEN_SYMBOL", tokenSymbol);
        pb.environment().put("TOTAL_SUPPLY", String.valueOf(totalSupply));
        pb.environment().put("PRICE_USD_CENTS", String.valueOf(priceInCents));
        pb.environment().put("SPV_WALLET", spvWalletAddress);
        pb.environment().put("COMPLIANCE_REGISTRY_ADDRESS", props.getComplianceRegistryAddress());
        pb.environment().put("OPERATOR_PRIVATE_KEY", props.getOperatorPrivateKey());
        pb.redirectErrorStream(false);

        log.info("Deploying PropertyToken: flat={} network={}", flatId, network);

        Process process = pb.start();

        StringBuilder stdout = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("{")) {
                    stdout.append(line);
                } else {
                    log.debug("[hardhat] {}", line);
                }
            }
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[hardhat stderr] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Hardhat deployment failed with exit code " + exitCode);
        }

        String json = stdout.toString().trim();
        if (json.isEmpty()) {
            throw new RuntimeException("Hardhat script produced no output");
        }

        log.info("Deployment output: {}", json);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = objectMapper.readValue(json, Map.class);

        if (result.containsKey("error")) {
            throw new RuntimeException("Contract deployment error: " + result.get("error"));
        }

        return new DeploymentResult(
                (String) result.get("contractAddress"),
                (String) result.get("txHash"),
                ((Number) result.get("blockNumber")).longValue(),
                (String) result.get("operatorAddress"),
                (String) result.get("network"),
                ((Number) result.get("chainId")).longValue()
        );
    }

    private String resolveHardhatNetwork() {
        return switch (props.getNetwork()) {
            case "hardhat", "local" -> "localhost";
            case "polygon-amoy" -> "amoy";
            case "polygon" -> "polygon";
            default -> "localhost";
        };
    }

    private String resolveHardhatDir() {
        String envDir = System.getenv("HARDHAT_DIR");
        if (envDir != null && !envDir.isBlank()) {
            return envDir;
        }
        return "hardhat";
    }
}
