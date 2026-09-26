package com.tokenrealty.corporateactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CorporateActionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorporateActionsApplication.class, args);
    }
}
