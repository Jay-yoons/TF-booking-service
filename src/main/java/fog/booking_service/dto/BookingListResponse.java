package fog.booking_service.dto;

import fog.booking_service.domain.BookingStateCode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BookingListResponse {

    private Long bookingNum;
    private LocalDateTime bookingDate;
    private String storeId;
    private String bookingState;
}
