package com.tokenrealty.web.rest;

public final class DownstreamServices {

    public record ServiceSpec(String label) {
    }

    public static final ServiceSpec PROPERTY_REGISTRY = new ServiceSpec("Property Registry");
    public static final ServiceSpec TOKEN_ISSUANCE = new ServiceSpec("Token Issuance service");
    public static final ServiceSpec MARKETPLACE = new ServiceSpec("Marketplace service");
    public static final ServiceSpec PAYMENT = new ServiceSpec("Payment service");
    public static final ServiceSpec COMPLIANCE = new ServiceSpec("Compliance service");
    public static final ServiceSpec DOCUMENT = new ServiceSpec("Document service");
    public static final ServiceSpec WALLET = new ServiceSpec("Wallet service");
    public static final ServiceSpec AUTH = new ServiceSpec("Auth service");
    public static final ServiceSpec RENTAL = new ServiceSpec("Rental service");
    public static final ServiceSpec BLOCKCHAIN_INDEXER = new ServiceSpec("Blockchain Indexer service");

    private DownstreamServices() {
    }
}
