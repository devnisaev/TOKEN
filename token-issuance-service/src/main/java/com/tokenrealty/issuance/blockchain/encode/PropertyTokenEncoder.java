package com.tokenrealty.issuance.blockchain.encode;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public final class PropertyTokenEncoder {

    private PropertyTokenEncoder() {
    }

    public static String encodeEnableTransfers() {
        return FunctionEncoder.encode(new Function(
                "enableTransfers",
                Collections.emptyList(),
                Collections.emptyList()));
    }

    public static String encodeSuspendTransfers() {
        return FunctionEncoder.encode(new Function(
                "suspendTransfers",
                Collections.emptyList(),
                Collections.emptyList()));
    }

    public static String encodeOperatorTransfer(String fromAddress, String toAddress, long amount) {
        return FunctionEncoder.encode(new Function(
                "operatorTransfer",
                List.of(
                        AddressEncoder.toAddress(fromAddress),
                        AddressEncoder.toAddress(toAddress),
                        new Uint256(BigInteger.valueOf(amount))),
                Collections.emptyList()));
    }

    public static String encodeBalanceOf(String address) {
        return FunctionEncoder.encode(new Function(
                "balanceOf",
                List.of(AddressEncoder.toAddress(address)),
                List.of(new TypeReference<Uint256>() {
                })));
    }

    public static String encodeTransfersEnabled() {
        return FunctionEncoder.encode(new Function(
                "transfersEnabled",
                Collections.emptyList(),
                List.of(new TypeReference<org.web3j.abi.datatypes.Bool>() {
                })));
    }
}
