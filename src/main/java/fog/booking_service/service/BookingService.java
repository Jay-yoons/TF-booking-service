package fog.booking_service.service;

import fog.booking_service.controller.BookingController;
import fog.booking_service.domain.Booking;
import fog.booking_service.domain.BookingStateCode;
import fog.booking_service.dto.BookingListResponse;
import fog.booking_service.dto.BookingResponse;
import fog.booking_service.dto.BookingStatusMessage;
import fog.booking_service.dto.SQSBookingRequest;
import fog.booking_service.repositoroy.BookingRepository;
import fog.booking_service.repositoroy.BookingStateCodeRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
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
    private final MessageService messageService;

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
    예약 생성 - sqs를 위해 기존 예약 일부 수정
     */
    public Booking makeBooking(SQSBookingRequest request) {

        log.info("예약 생성");
//        log.info("전체좌석={}", request.getSeats());
//        log.info("사용중인 좌석={}", getAvailableSeats(request.getStoreId(), request.getBookingDate()));
//        log.info("예약할 좌석={}", request.getCount());
        if (request.getSeats() - getAvailableSeats(request.getStoreId(), request.getBookingDate()) < request.getCount()) {
            log.error("예약 가능한 좌석 수 초과");
            throw new IllegalStateException("예약 가능한 좌석 수를 초과하였습니다.");
        }

        BookingStateCode stateCode = stateCodeRepository.findById(0)
                .orElseThrow(() -> new EntityNotFoundException("code 0 is not found"));
        log.info("userId={}, storeId={}, bookingDate={}, count={}", request.getUserId(), request.getStoreId(), request.getBookingDate(), request.getCount());
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .storeId(request.getStoreId())
                .bookingDate(request.getBookingDate())
                .count(request.getCount())
                .stateCode(stateCode)
                .build();
        return bookingRepository.save(booking);
    }

    /**
     * 예약 생성 - SQS 사용
     */
    @SqsListener(value = "BookingService.fifo", acknowledgementMode = "MANUAL")
    public void handleBookingRequest(SQSBookingRequest request, Acknowledgement acknowledgement) {

        log.info("SQS 큐 예약 요청 메시지 수신: userId={}", request.getUserId());

        try {
            Booking savedBooking = makeBooking(request);
            log.info("예약 생성 완료, 문자 발송 시작");
            messageService.sendMessage(savedBooking, request.getUserName());

            sendSseEvent(request.getUserId(), new BookingStatusMessage("success", "예약이 성공적으로 완료되었습니다.", savedBooking.getBookingNum()));
            acknowledgement.acknowledge();

        } catch (IllegalStateException e) { // SSE로 실패 메시지 전송
            log.warn("예약 가능한 좌석을 초과했습니다. userId={}", request.getUserId());
            sendSseEvent(request.getUserId(), new BookingStatusMessage("failure", "예약 가능한 좌석 수를 초과하였습니다.", null));
            acknowledgement.acknowledge();
        } catch (Exception e) { // SSE로 기타 오류 메시지 전송
            log.error("예약 처리 중 오류 발생: {}", e.getMessage());
            sendSseEvent(request.getUserId(), new BookingStatusMessage("failure", "예약 처리 중 오류가 발생했습니다.", null));
            acknowledgement.acknowledge();
        }
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

    /**
     * SSE 이벤트 전송
     */
    private void sendSseEvent(String userId, BookingStatusMessage payload) {
        SseEmitter emitter = BookingController.emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(payload));
                // 전송 성공 시 연결을 닫고 맵에서 제거
                emitter.complete();
                BookingController.emitters.remove(userId);
            } catch (IOException e) {
                log.error("SSE send failed for userId={}: {}", userId, e.getMessage());
                BookingController.emitters.remove(userId);
            }
        } else {
            log.warn("No SseEmitter found for userId={}", userId);
        }
    }
}
