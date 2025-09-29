package fog.booking_service.repositoroy;

import fog.booking_service.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByUserId(String userId);

    @Query("SELECT COALESCE(SUM(b.count), 0) FROM Booking b WHERE b.bookingDate = :bookingDate AND b.bookingStateCode.id = :bookingStateCodeId AND b.storeId = :storeId")
    Integer sumCountByBookingDate(
            @Param("storeId") String storeId,
            @Param("bookingDate") LocalDateTime bookingDate,
            @Param("bookingStateCodeId") Integer bookingStateCodeId
    );

    List<Booking> findByBookingDateLessThanEqualAndBookingStateCode_BookingStateCode(LocalDateTime now, int bookingStateCode);
}