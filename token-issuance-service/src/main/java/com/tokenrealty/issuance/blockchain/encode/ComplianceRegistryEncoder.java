package com.tokenrealty.issuance.blockchain.encode;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public final class ComplianceRegistryEncoder {

    private ComplianceRegistryEncoder() {
    }

    public static String encodeIsWhitelisted(String address) {
        return FunctionEncoder.encode(new Function(
                "isWhitelisted",
                List.of(AddressEncoder.toAddress(address)),
                List.of(new TypeReference<org.web3j.abi.datatypes.Bool>() {
                })));
    }

    public static String encodeAddToWhitelist(String address, String country, long expiresAt) {
        return FunctionEncoder.encode(new Function(
                "addToWhitelist",
                List.of(
                        AddressEncoder.toAddress(address),
                        new Utf8String(country != null ? country : ""),
                        new Uint256(BigInteger.valueOf(expiresAt))),
                Collections.emptyList()));
    }

    public static String encodeRemoveFromWhitelist(String address) {
        return FunctionEncoder.encode(new Function(
                "removeFromWhitelist",
                List.of(AddressEncoder.toAddress(address)),
                Collections.emptyList()));
    }
}
