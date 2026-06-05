package com.utility.billing;

import com.utility.billing.common.security.JwtProperties;
import com.utility.billing.customer.config.FileProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, FileProperties.class})
public class UbsApplication {

	public static void main(String[] args) {
		SpringApplication.run(UbsApplication.class, args);
	}
}
