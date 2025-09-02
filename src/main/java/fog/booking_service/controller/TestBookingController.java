//package fog.booking_service.controller;
//
//import fog.booking_service.dto.BookingRequest;
//import fog.booking_service.dto.SQSBookingRequest;
//import io.awspring.cloud.sqs.operations.SqsTemplate;
//import lombok.AllArgsConstructor;
//import org.springframework.beans.BeanUtils;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.UUID;
//
//@AllArgsConstructor
//@RestController
//public class TestBookingController {
//
//    private final SqsTemplate sqsTemplate;  // SQS 전송용
//    private final String bookingRequestQueue = "test.fifo"; // FIFO 큐 이름 (LocalStack에 맞게 설정)
//
//    @PostMapping("/bookings/test-new")
//    public ResponseEntity<String> testBookingViaSqs(@RequestBody BookingRequest request) {
//        // userId 강제 입력 또는 임의 생성
//        String userId = request.getUserId();
//        if (userId == null) {
//            userId = UUID.randomUUID().toString();
//            request.setUserId(userId);
//        }
//
//        // SQS 메시지 DTO 변환
//        SQSBookingRequest sqsRequest = new SQSBookingRequest();
//        BeanUtils.copyProperties(request, sqsRequest);
//        sqsRequest.setUserName("test-user");
//
//        // 메시지 그룹 ID
//        String messageGroupId = sqsRequest.getStoreId() + "-" + sqsRequest.getBookingDate().toLocalDate().toString();
//
//        try {
//            // 메시지 중복 제거 ID (UUID 활용)
//            String messageDeduplicationId = UUID.randomUUID().toString();
//
//            // SQS 큐에 메시지 전송 (FIFO 큐 속성 포함)
//            sqsTemplate.send(sqsSendOptions -> sqsSendOptions
//                    .queue(bookingRequestQueue)
//                    .payload(sqsRequest)
//                    .messageGroupId(messageGroupId)
//                    .messageDeduplicationId(messageDeduplicationId)
//            );
//
//            return ResponseEntity.ok("예약 요청 SQS 전송 성공");
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("예약 요청 전송 중 오류: " + e.getMessage());
//        }
//    }
//}