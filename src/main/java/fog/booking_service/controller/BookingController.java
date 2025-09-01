package fog.booking_service.controller;

import fog.booking_service.config.CustomUserDetails;
import fog.booking_service.domain.Booking;
import fog.booking_service.dto.BookingListResponse;
import fog.booking_service.dto.BookingRequest;
import fog.booking_service.dto.BookingResponse;
import fog.booking_service.dto.SQSBookingRequest;
import fog.booking_service.service.BookingService;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;
    private final SqsTemplate sqsTemplate;

    @Value("${aws.sqs.queue.booking-request}")
    private String bookingRequestQueue;

    public static Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 연결을 위한 엔드포인트
     * @param userId
     * @return
     */
    @GetMapping("/sse/booking-status/{userId}")
    public SseEmitter connect(@PathVariable String userId) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L); // 10분 타임아웃
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            log.error("SSE connect send failed: ", e);
        }

        return emitter;
    }

    /**
     * 예약된 좌석 수 조회
     * 반드시 분, 초를 00:00으로 받을것
     * @param dateTime
     * @return
     */
    @GetMapping("/bookings/seats/{storeId}")
    public Integer getAvailableSeats(@PathVariable String storeId, @RequestParam LocalDateTime dateTime) {
        log.info("예약된 좌석수 조회");
        return bookingService.getAvailableSeats(storeId, dateTime);
    }

    /**
     * 내 예약 목록 조회
     * URL 경로에 사용자 ID를 포함하여 받도록 수정
     */
    @GetMapping("/bookings/users/{userId}")
    public List<BookingListResponse> findBookingList(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable String userId) {
        String loginId = userDetails.getUsername();
        if (!loginId.equals(userId)) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
        log.info("예약 목록 조회 요청: userId={}", loginId);
        return bookingService.getBookingList(userDetails.getSub());
    }

    /**
     * 예약 상세 조회
     */
    @GetMapping("/bookings/{bookingNum}")
    public BookingResponse findBooking(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long bookingNum) {
        // 1. JWT 토큰에서 사용자 ID 추출
        String userId = userDetails.getSub();
        log.info("예약 상세 조회 요청: bookingNum={}, userId={}", bookingNum, userId);

        // 2. 예약 정보 조회
        BookingResponse bookingResponse = bookingService.getBookingResponse(bookingNum);

        // 3. 조회된 예약 정보의 userId와 현재 로그인한 userId가 일치하는지 확인
        if (!bookingResponse.getUserId().equals(userId)) {
            log.error("예약 상세 조회 권한이 없습니다: userId={}, bookingOwnerId={}", userId, bookingResponse.getUserId());
            throw new SecurityException("접근 권한이 없습니다.");
        }

        return bookingResponse;
    }

    /**
     * 예약 생성 - 기존
     */
/*    @PostMapping("/bookings/new")
    public BookingResponse booking(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody BookingRequest requestBody) {
        String userId = userDetails.getSub();

        log.info("예약자 id 일치여부 확인: requestId={}, tokenId={}", requestBody.getUserId(), userId);
        requestBody.setUserId(userId);

        log.info("예약 요청: userId={}, storeId={}, bookingDate={}, count={}",
                requestBody.getUserId(), requestBody.getStoreId(), requestBody.getBookingDate(), requestBody.getCount());
        return bookingService.makeBooking(requestBody, userDetails.getUsername());
    }*/

    /**
     * 예약 생성 - AWS SQS 사용
     */
    @PostMapping("/bookings/new")
    public String booking(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody BookingRequest request) {
        String userId = userDetails.getSub();
        request.setUserId(userId);

        SQSBookingRequest sqsRequest = new SQSBookingRequest();
        BeanUtils.copyProperties(request, sqsRequest);
        sqsRequest.setUserName(userDetails.getUsername());

        // 고유한 메시지 그룹 ID 생성 (FIFO 큐에 필수)
        String messageGroupId = "booking-group-" + userId;

        // SQS 큐로 메시지 전송
        sqsTemplate.send(sqsSendOptions -> sqsSendOptions
                .queue(bookingRequestQueue)
                .payload(sqsRequest)
                .messageDeduplicationId(UUID.randomUUID().toString())
                .messageGroupId(messageGroupId)
        );

        log.info("예약 요청 SQS 큐 전송: userId={}", userId);
        return "예약 처리중입니다.";
    }

    /**
     * 예약 취소
     */
    @PatchMapping("/bookings/{bookingNum}")
    public BookingResponse cancelBooking(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long bookingNum) {
        String userId = userDetails.getSub();
        log.info("예약 취소 요청: userId={}, bookingNum={}", userId, bookingNum);

        // 예약 취소 시 본인 예약인지 확인하는 로직 추가
        Booking booking = bookingService.getBooking(bookingNum);
        if (!booking.getUserId().equals(userId)) {
            log.error("예약 취소 권한이 없습니다: userId={}, bookingOwnerId={}", userId, booking.getUserId());
            throw new SecurityException("예약 취소 권한이 없습니다.");
        }

        bookingService.cancelBooking(bookingNum);
        return bookingService.getBookingResponse(bookingNum);
    }
}