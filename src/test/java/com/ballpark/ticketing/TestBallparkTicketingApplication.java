package com.ballpark.ticketing;

import org.springframework.boot.SpringApplication;

public class TestBallparkTicketingApplication {

	public static void main(String[] args) {
		SpringApplication.from(BallparkTicketingApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
