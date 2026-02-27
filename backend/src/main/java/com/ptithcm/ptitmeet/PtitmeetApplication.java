package com.ptithcm.ptitmeet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class PtitmeetApplication {

	public static void main(String[] args) {
		SpringApplication.run(PtitmeetApplication.class, args);
	}

}
