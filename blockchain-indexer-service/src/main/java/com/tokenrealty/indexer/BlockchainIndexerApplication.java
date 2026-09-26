package com.tokenrealty.indexer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BlockchainIndexerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlockchainIndexerApplication.class, args);
    }
}
