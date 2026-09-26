package com.tokenrealty.issuance.blockchain.encode;

import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.nio.charset.StandardCharsets;

final class AddressEncoder {

    private AddressEncoder() {
    }

    static Address toAddress(String address) {
        return new Address(normalizeAddress(address));
    }

    static String normalizeAddress(String address) {
        String raw = address.startsWith("0x") ? address.substring(2) : address;
        if (raw.length() == 40 && raw.matches("[0-9a-fA-F]+")) {
            return "0x" + raw.toLowerCase();
        }
        byte[] hash = Hash.sha3(raw.getBytes(StandardCharsets.UTF_8));
        byte[] addr = new byte[20];
        System.arraycopy(hash, 12, addr, 0, 20);
        return Numeric.toHexString(addr);
    }
}
