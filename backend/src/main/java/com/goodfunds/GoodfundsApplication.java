package com.goodfunds;

import com.goodfunds.config.InvoiceUploadProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(InvoiceUploadProperties.class)
public class GoodfundsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoodfundsApplication.class, args);
    }
}
