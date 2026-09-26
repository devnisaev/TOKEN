package com.tokenrealty.indexer.blockchain;

import org.web3j.crypto.Hash;

public final class EventTopics {

    public static final String TRANSFER = Hash.sha3String("Transfer(address,address,uint256)");
    public static final String WHITELIST_ADDED = Hash.sha3String("WhitelistAdded(address,string,uint256)");
    public static final String WHITELIST_REMOVED = Hash.sha3String("WhitelistRemoved(address)");

    private EventTopics() {
    }
}
