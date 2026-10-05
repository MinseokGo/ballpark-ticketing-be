package com.ballpark.ticketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BallparkTicketingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BallparkTicketingApplication.class, args);
	}

}
