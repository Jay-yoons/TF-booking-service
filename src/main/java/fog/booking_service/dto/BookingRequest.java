package fog.booking_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fog.booking_service.domain.BookingStateCode;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class BookingRequest {

    private LocalDateTime bookingDate;  //예약 날짜
    private String userId;              //유저 ID
    private String storeId;             //가게 ID
    private int count;                  //예약 좌석 수
}
