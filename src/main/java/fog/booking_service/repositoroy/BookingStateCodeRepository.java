package fog.booking_service.repositoroy;

import fog.booking_service.domain.BookingStateCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStateCodeRepository extends JpaRepository<BookingStateCode, Integer> {
}
