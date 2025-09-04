package fog.booking_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookingServiceApplication {

	// Test comment for GitHub Actions workflow trigger - 2025-09-04
	public static void main(String[] args) {
		SpringApplication.run(BookingServiceApplication.class, args);
	}

}
