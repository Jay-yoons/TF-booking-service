package fog.booking_service.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "BOOKING")
@NoArgsConstructor
@Getter
@Setter
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BOOKING_NUM")
    private Long bookingNum;

    @Column(name = "BOOKING_DATE", nullable = false)
    private LocalDateTime bookingDate;

    @Column(name = "USER_ID", nullable = false, length = 50)
    private String userId;

    @Column(name = "STORE_ID", nullable = false, length = 20)
    private String storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOOKING_STATE_CODE", referencedColumnName = "BOOKING_STATE_CODE")
    private BookingStateCode bookingStateCode;

    @Column(name = "COUNT", nullable = false)
    private int count;

    @Builder
    public Booking(LocalDateTime bookingDate, String userId, String storeId, int count, BookingStateCode stateCode) {
        this.bookingDate = bookingDate;
        this.userId = userId;
        this.storeId = storeId;
        this.count = count;
        this.bookingStateCode = stateCode;
    }
}