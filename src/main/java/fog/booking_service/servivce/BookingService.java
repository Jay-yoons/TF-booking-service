package fog.booking_service.servivce;

import fog.booking_service.domain.Booking;
import fog.booking_service.domain.BookingStateCode;
import fog.booking_service.dto.BookingListResponse;
import fog.booking_service.dto.BookingRequest;
import fog.booking_service.dto.BookingResponse;
import fog.booking_service.repositoroy.BookingRepository;
import fog.booking_service.repositoroy.BookingStateCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingStateCodeRepository stateCodeRepository;

    /*
    예약 리스트 조회
     */
    public List<BookingListResponse> getBookingList(String userId) {
        log.info("예약 리스트 조회");
        List<Booking> bookings = bookingRepository.findAllByUserId(userId);
        for (Booking booking : bookings) {
            log.info("bookingNum={}", booking.getBookingNum());
        }
        return bookings.stream()
                .map(b -> BookingListResponse.builder()
                        .bookingNum(b.getBookingNum())
                        .bookingDate(b.getBookingDate())
                        .storeId(b.getStoreId())
                        .bookingState(b.getBookingStateCode().getStateName())
                        .build())
                .collect(Collectors.toList());
    }

    /*
    예약 상세 조회 - Booking
     */
    public Booking getBooking(Long bookingNum) {
        return bookingRepository.findById(bookingNum)
                .orElseThrow(() -> new EntityNotFoundException("Booking is not found"));
    }

    /*
    예약 상세 조회 - BookingResponse
     */
    public BookingResponse getBookingResponse(Long bookingNum) {
        log.info("예약 상세 조회");
        Booking booking = bookingRepository.findById(bookingNum)
                .orElseThrow(() -> new EntityNotFoundException("Booking is not found"));
        return BookingResponse.builder()
                .bookingNum(booking.getBookingNum())
                .bookingDate(booking.getBookingDate())
                .storeId(booking.getStoreId())
                .bookingState(booking.getBookingStateCode().getStateName())
                .count(booking.getCount())
                .userId(booking.getUserId())
                .build();
    }

    /*
    예약 생성
     */
    public BookingResponse makeBooking(BookingRequest request) {

        log.info("예약 생성");
        BookingStateCode stateCode = stateCodeRepository.findById(0)
                .orElseThrow(() -> new EntityNotFoundException("code 0 is not found"));
        log.info("userId={}", request.getUserId());
        log.info("storeId={}", request.getStoreId());
        log.info("bookingDate={}", request.getBookingDate());
        log.info("count={}", request.getCount());
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .storeId(request.getStoreId())
                .bookingDate(request.getBookingDate())
                .count(request.getCount())
                .stateCode(stateCode)
                .build();
        Long bookingNum = bookingRepository.save(booking).getBookingNum();
        return getBookingResponse(bookingNum);
    }

    /*
    예약 취소
     */
    public void cancelBooking(Long bookingNum) {
        log.info("예약 취소");
        Booking booking = bookingRepository.findById(bookingNum)
                .orElseThrow(() -> new EntityNotFoundException("Booking is not found"));
        BookingStateCode stateCode = stateCodeRepository.findById(1)
                .orElseThrow(() -> new EntityNotFoundException("code 1 is not found"));
        booking.setBookingStateCode(stateCode);
    }
}
