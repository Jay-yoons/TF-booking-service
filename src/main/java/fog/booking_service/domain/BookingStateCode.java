package fog.booking_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "BOOKING_STATE_CODE")
@Getter
@Setter
public class BookingStateCode {

    @Id
    @Column(name = "BOOKING_STATE_CODE")
    private int bookingStateCode;   //0, 1, 2

    @Column(name = "STATE_NAME", length = 10)
    private String stateName;   //CONFIRMED, CANCELED, COMPLETED

    // (양방향 매핑)
    @OneToMany(mappedBy = "bookingStateCode")
    private List<Booking> bookings;
}