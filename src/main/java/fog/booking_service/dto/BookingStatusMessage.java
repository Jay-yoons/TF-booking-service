package fog.booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BookingStatusMessage {
    private String status;
    private String message;
    private Long bookingId;
}
