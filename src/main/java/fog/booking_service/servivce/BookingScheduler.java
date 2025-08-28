package fog.booking_service.servivce;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingScheduler {

    private final BookingService bookingService;

    @Scheduled(cron = "0 0 * * * ?", zone = "Asia/Seoul")
    public void updateBookingState() {
        bookingService.updateBookingState();
    }

}
