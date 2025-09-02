//package fog.booking_service.service;
//
//import fog.booking_service.domain.Booking;
//import fog.booking_service.dto.SQSBookingRequest;
//import io.awspring.cloud.sqs.annotation.SqsListener;
//import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class TestBookingService {
//
//    private final BookingService bookingService;
//
//    @SqsListener(value = "test.fifo", acknowledgementMode = "MANUAL")
//    public void TestHandleBookingRequest(SQSBookingRequest request, Acknowledgement acknowledgement) {
//
//        log.info("SQS 큐 예약 요청 메시지 수신: userId={}", request.getUserId());
//
//        try {
//            Booking savedBooking = bookingService.makeBooking(request);
//            log.info("예약 생성 완료, 문자 발송 생량");
//            acknowledgement.acknowledge();
//
//        } catch (IllegalStateException e) { // SSE로 실패 메시지 전송
//            log.warn("예약 가능한 좌석을 초과했습니다. userId={}", request.getUserId());
//            acknowledgement.acknowledge();
//        } catch (Exception e) { // SSE로 기타 오류 메시지 전송
//            log.error("예약 처리 중 오류 발생: {}", e.getMessage());
//            acknowledgement.acknowledge();
//        }
//    }
//}