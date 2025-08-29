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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingStateCodeRepository stateCodeRepository;

    /**
     * 예약된 좌석 수 조회
     * @param dateTime
     * @return
     */
    public Integer getAvailableSeats(String storeId, LocalDateTime dateTime) {
        log.info("예약된 좌석 수 조회");
        return bookingRepository.sumCountByBookingDate(storeId, dateTime, 0);
    }

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
        log.info("전체좌석={}", request.getSeats());
        log.info("사용중인 좌석={}", getAvailableSeats(request.getStoreId(), request.getBookingDate()));
        log.info("예약할 좌석={}", request.getCount());
        if (request.getSeats() - getAvailableSeats(request.getStoreId(), request.getBookingDate()) < request.getCount()) {
            throw new IllegalStateException("예약 가능한 좌석 수를 초과하였습니다.");
        }

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

    /**
     * 1시간마다 배치작업으로 상태코드 변경
     */
    public void updateBookingState() {
        log.info("배치작업 시작");
        // 한국 시간(Asia/Seoul)으로 ZonedDateTime 객체 생성
        ZonedDateTime nowInSeoul = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

        // ZonedDateTime에서 LocalDateTime으로 변환
        LocalDateTime now = nowInSeoul.toLocalDateTime();
        log.info("배치작업 기준 현재 시간: {}", now);

        // 1. 현재 시간이 지난 CONFIRMED 상태의 예약 조회
        List<Booking> bookingsToUpdate = bookingRepository.
                findByBookingDateLessThanEqualAndBookingStateCode_BookingStateCode(now, 0);

        if (bookingsToUpdate.isEmpty()) {
            log.info("업데이트할 예약 없음");
            return;
        }

        // 2. COMPLETED 상태 코드 엔티티 가져오기
        BookingStateCode completedStateCode = stateCodeRepository.findById(2)
                .orElseThrow(() -> new RuntimeException("COMPLETED state code not found"));

        // 3. 상태 업데이트
        for (Booking booking : bookingsToUpdate) {
            booking.setBookingStateCode(completedStateCode);
        }

        log.info("업데이트된 예약 수: {}", bookingsToUpdate.size());
    }
}
