/**
 * Generates Web3j Java wrappers from Hardhat compile artifacts.
 *
 * Prerequisites:
 *   npm run compile
 *   web3j CLI on PATH (https://docs.web3j.io/latest/getting_started/installing_web3j/)
 *
 * Usage:
 *   npm run generate-wrappers
 */

const { execSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const CONTRACTS = [
    { artifact: "ComplianceRegistry.sol/ComplianceRegistry.json", className: "ComplianceRegistry" },
    { artifact: "PropertyToken.sol/PropertyToken.json", className: "PropertyToken" },
    { artifact: "MockUSDC.sol/MockUSDC.json", className: "MockUSDC" },
];

const ARTIFACTS_DIR = path.join(__dirname, "../artifacts/contracts");
const OUTPUT_DIR = path.join(__dirname, "../../src/main/java");
const JAVA_PACKAGE = "com.tokenrealty.issuance.blockchain.generated";

function run() {
    if (!commandExists("web3j")) {
        console.error("web3j CLI not found on PATH.");
        console.error("Install: https://docs.web3j.io/latest/getting_started/installing_web3j/");
        console.error("Then re-run: npm run generate-wrappers");
        process.exit(1);
    }

    for (const contract of CONTRACTS) {
        const abiPath = path.join(ARTIFACTS_DIR, contract.artifact);
        if (!fs.existsSync(abiPath)) {
            console.error(`Missing artifact: ${abiPath}`);
            console.error("Run: npm run compile");
            process.exit(1);
        }
    }

    fs.mkdirSync(OUTPUT_DIR, { recursive: true });

    for (const contract of CONTRACTS) {
        const abiPath = path.join(ARTIFACTS_DIR, contract.artifact);
        console.log(`Generating ${contract.className} → ${JAVA_PACKAGE}`);
        execSync(
            [
                "web3j", "generate", "solidity",
                "-a", abiPath,
                "-o", OUTPUT_DIR,
                "-p", JAVA_PACKAGE,
            ].join(" "),
            { stdio: "inherit" }
        );
    }

    console.log("\nDone. Wrappers written under:");
    console.log(`  ${OUTPUT_DIR}/${JAVA_PACKAGE.replace(/\./g, "/")}/`);
    console.log("\nNext: inject wrappers in BlockchainConnector / OnChainWhitelistService (optional refactor).");
}

function commandExists(name) {
    try {
        execSync(`command -v ${name}`, { stdio: "ignore" });
        return true;
    } catch {
        return false;
    }
}

run();
