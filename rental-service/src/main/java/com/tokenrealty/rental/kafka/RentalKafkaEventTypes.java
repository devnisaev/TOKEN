package com.tokenrealty.rental.kafka;

public final class RentalKafkaEventTypes {

    public static final String RENT_DUE = "tokenrealty.rental.rent.due.v1";
    public static final String LEASE_EXPIRED = "tokenrealty.rental.lease.expired.v1";

    private RentalKafkaEventTypes() {
    }
}
