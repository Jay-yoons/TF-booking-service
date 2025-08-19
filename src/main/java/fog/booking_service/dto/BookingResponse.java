package fog.booking_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BookingResponse {
    private Long bookingNum;
    private LocalDateTime bookingDate;
    private String storeId;
    private String bookingState;
    private int count;
    private String userId;
}
