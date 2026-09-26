package com.tokenrealty.issuance.blockchain.encode;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Collections;

public final class DividendDistributorEncoder {

    private DividendDistributorEncoder() {
    }

    public static String encodeDeposit(BigInteger amount) {
        Function function = new Function(
                "deposit",
                Collections.singletonList(new Uint256(amount)),
                Collections.emptyList());
        return FunctionEncoder.encode(function);
    }
}
