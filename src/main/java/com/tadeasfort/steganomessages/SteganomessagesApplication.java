package com.tadeasfort.steganomessages;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SteganomessagesApplication {

	public static void main(String[] args) {
		SpringApplication.run(SteganomessagesApplication.class, args);
	}

}
