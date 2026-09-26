package com.tokenrealty.indexer.blockchain;

import org.web3j.crypto.Hash;

public final class EventTopics {

    public static final String TRANSFER = Hash.sha3String("Transfer(address,address,uint256)");
    public static final String WHITELIST_ADDED = Hash.sha3String("WhitelistAdded(address,string,uint256)");
    public static final String WHITELIST_REMOVED = Hash.sha3String("WhitelistRemoved(address)");
    public static final String DIVIDEND_DEPOSITED = Hash.sha3String("Deposited(address,uint256,uint256)");
    public static final String DIVIDEND_CLAIMED = Hash.sha3String("Claimed(address,uint256)");

    private EventTopics() {
    }
}
