package com.sfr.sfr_worker_region;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SfrWorkerRegionApplication {

	public static void main(String[] args) {
		SpringApplication.run(SfrWorkerRegionApplication.class, args);
	}

}
