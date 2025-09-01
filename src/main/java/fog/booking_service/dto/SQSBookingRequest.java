package fog.booking_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SQSBookingRequest {
    private LocalDateTime bookingDate;  //예약 날짜
    private String userId;              //유저 ID(cognito sub - UUID)
    private String storeId;             //가게 ID
    private int count;                  //예약 좌석 수
    private int seats;                  //가게 전체 좌석 수
    private String userName;               //유저 ID(cognito username)
}