package com.tokenrealty.issuance.blockchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenrealty.issuance.config.BlockchainProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ContractDeployer — deploys PropertyToken contracts by invoking
 * the Hardhat deployFlat.js script as a subprocess.
 *
 * This approach keeps the Java code clean — no need to manually
 * encode contract bytecode + constructor args in Java.
 * The script outputs a single JSON line that we parse.
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

    /**
     * Deploys a PropertyToken contract for a flat.
     * Calls hardhat/scripts/deployFlat.js as a subprocess.
     *
     * @param flatId       UUID of the flat in Property Registry
     * @param buildingId   UUID of the building
     * @param tokenName    e.g. "Bishkek City Plaza — Flat 101"
     * @param tokenSymbol  e.g. "BKCP-101"
     * @param totalSupply  e.g. 1000
     * @param tokenPriceUsd e.g. 45.00
     */
    public DeploymentResult deployPropertyToken(
            UUID flatId,
            UUID buildingId,
            String tokenName,
            String tokenSymbol,
            Long totalSupply,
            BigDecimal tokenPriceUsd
    ) throws Exception {

        // Convert price to cents (integer) for Solidity
        long priceInCents = tokenPriceUsd.multiply(BigDecimal.valueOf(100)).longValue();

        String network = resolveHardhatNetwork();
        String hardhatDir = resolveHardhatDir();

        List<String> command = List.of(
                "npx", "hardhat", "run", "scripts/deployFlat.js",
                "--network", network,
                "--flat-id", flatId.toString(),
                "--building-id", buildingId.toString(),
                "--name", tokenName,
                "--symbol", tokenSymbol,
                "--supply", String.valueOf(totalSupply),
                "--price-usd", String.valueOf(priceInCents)
        );

        log.info("Deploying PropertyToken: flat={} network={}", flatId, network);
        log.debug("Command: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new java.io.File(hardhatDir));
        pb.environment().put("COMPLIANCE_REGISTRY_ADDRESS", props.getComplianceRegistryAddress());
        pb.environment().put("OPERATOR_PRIVATE_KEY", props.getOperatorPrivateKey());
        pb.redirectErrorStream(false);

        Process process = pb.start();

        // Read stdout (JSON result line)
        StringBuilder stdout = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("{")) stdout.append(line);
                else log.debug("[hardhat] {}", line);
            }
        }

        // Read stderr (logs)
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
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
        // Resolve relative to working directory
        // In dev: project root/hardhat
        // Override with HARDHAT_DIR env var if needed
        String envDir = System.getenv("HARDHAT_DIR");
        if (envDir != null && !envDir.isBlank()) return envDir;
        return "hardhat";
    }
}