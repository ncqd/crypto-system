package com.project.crypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CryptoSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoSystemApplication.class, args);
	}

}
