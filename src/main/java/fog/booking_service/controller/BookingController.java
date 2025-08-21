package fog.booking_service.controller;

import fog.booking_service.domain.Booking;
import fog.booking_service.dto.BookingListResponse;
import fog.booking_service.dto.BookingRequest;
import fog.booking_service.dto.BookingResponse;
import fog.booking_service.servivce.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;

@RestController
@Slf4j
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final HttpServletRequest request; // HttpServletRequest 의존성 추가

    /**
     * 예약 된 좌석 수 조회
     * 반드시 분, 초를 00:00으로 받을것
     * @param dateTime
     * @return
     */
    @GetMapping("/bookings/seats")
    public Integer getAvailableSeats(LocalDateTime dateTime) {
        return bookingService.getAvailableSeats(dateTime);
    }

    /**
     * 내 예약 목록 조회
     * URL 경로에 사용자 ID를 포함하여 받도록 수정
     */
    @GetMapping("/bookings/users/{userId}")
    public List<BookingListResponse> findBookingList() {
        // 토큰에서 추출한 userId와 경로의 userId가 일치하는지 검증하는 로직 추가
        String userId = getCurrentUserIdFromToken();
        if (userId == null) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
        log.info("예약 목록 조회 요청: userId={}", userId);
        return bookingService.getBookingList(userId);
    }

    /**
     * 예약 상세 조회
     */
    @GetMapping("/bookings/{bookingNum}")
    public BookingResponse findBooking(@PathVariable Long bookingNum) {
        // 1. JWT 토큰에서 사용자 ID 추출
        String userId = getCurrentUserIdFromToken();
        if (userId == null) {
            log.error("JWT 토큰에서 userId를 추출할 수 없습니다.");
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
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
     * 예약 생성
     */
    @PostMapping("/bookings/new")
    public BookingResponse booking(@RequestBody BookingRequest requestBody) {
        String userId = getCurrentUserIdFromToken();
        if (userId == null) {
            log.error("JWT 토큰에서 userId를 추출할 수 없습니다.");
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        // 요청 본문(RequestBody)의 userId를 토큰에서 가져온 userId로 덮어쓰기
        // 이렇게 하면 프론트에서 보낸 userId가 아닌, 로그인된 실제 사용자 ID가 사용됩니다.
        requestBody.setUserId(userId);

        log.info("예약 요청: userId={}, storeId={}, bookingDate={}, count={}",
                requestBody.getUserId(), requestBody.getStoreId(), requestBody.getBookingDate(), requestBody.getCount());
        return bookingService.makeBooking(requestBody);
    }

    /**
     * 예약 취소
     */
    @PatchMapping("/bookings/{bookingNum}")
    public BookingResponse cancelBooking(@PathVariable Long bookingNum) {
        String userId = getCurrentUserIdFromToken();
        if (userId == null) {
            log.error("JWT 토큰에서 userId를 추출할 수 없습니다.");
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
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

    /**
     * 요청 헤더의 JWT 토큰에서 사용자 ID를 추출하는 헬퍼 메소드
     */
    private String getCurrentUserIdFromToken() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Authorization 헤더가 없거나 형식이 올바르지 않습니다.");
            return null;
        }

        try {
            String token = authHeader.substring(7);
            log.info("추출된 토큰: {}", token);

            String[] tokenParts = token.split("\\.");
            if (tokenParts.length < 2) {
                log.error("JWT 토큰 형식이 올바르지 않습니다. (부분: {})", tokenParts.length);
                return null;
            }

            String payloadBase64 = tokenParts[1];
            String base64 = payloadBase64.replace('-', '+').replace('_', '/');
            while (base64.length() % 4 != 0) {
                base64 += "=";
            }

            String payloadJson = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            log.info("디코딩된 페이로드: {}", payloadJson);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payloadMap = mapper.readValue(payloadJson, HashMap.class);

            log.info("페이로드 맵: {}", payloadMap);

            // 'sub' 클레임을 읽도록 수정
            Object userId = payloadMap.get("sub");
            if (userId instanceof String) {
                return (String) userId;
            } else {
                log.error("'sub' 키를 찾을 수 없거나 String 타입이 아닙니다.");
                return null;
            }

        } catch (Exception e) {
            log.error("JWT 토큰에서 사용자 ID 추출 실패", e);
            return null;
        }
    }
}