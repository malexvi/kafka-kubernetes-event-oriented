package com.sfr.sfr_orchestrator_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SfrOrchestratorApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SfrOrchestratorApiApplication.class, args);
	}

}
