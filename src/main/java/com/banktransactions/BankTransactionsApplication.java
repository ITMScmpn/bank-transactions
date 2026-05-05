package com.banktransactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BankTransactionsApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankTransactionsApplication.class, args);
    }
}

