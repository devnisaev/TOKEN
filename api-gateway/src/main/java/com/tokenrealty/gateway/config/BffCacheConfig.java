package com.tokenrealty.gateway.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class BffCacheConfig {

    public static final String BFF_LISTING_CACHE = "bff-listings";
    public static final String BFF_FLAT_CACHE = "bff-flats";
}
