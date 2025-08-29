package fog.booking_service.servivce;

import fog.booking_service.domain.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    private final SnsClient snsClient;
    private final CognitoIdentityProviderClient cognitoClient;

    public void sendMessage(Booking booking) {
        // 1. Cognito에서 전화번호 조회
        String phoneNumber = getPhoneNumberFromCognito(booking.getUserId());
        LocalDateTime bookingDate = booking.getBookingDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
        String date = bookingDate.format(formatter);
        int count = booking.getCount();

        // 2. 예약 완료 문자 발송
        if (phoneNumber != null) {
            String message = date + "시 " + count + "좌석 예약이 완료되었습니다.";
            publishSmsMessage(phoneNumber, message);
        }
    }

    private String getPhoneNumberFromCognito(String userId) {
        try {
            log.info("userPoolId={}", userPoolId);
            AdminGetUserRequest request = AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userId)
                    .build();

            AdminGetUserResponse response = cognitoClient.adminGetUser(request);

            for (AttributeType attribute : response.userAttributes()) {
                if (attribute.name().equals("phone_number")) {
                    log.info("문자를 발송할 전화번호:{}", attribute.value());
                    return attribute.value();
                }
            }
        } catch (UserNotFoundException e) {
            log.warn("Cognito User Pool에서 사용자를 찾을 수 없습니다: {}", userId);
        } catch (CognitoIdentityProviderException e) {
            // 기타 Cognito 관련 예외 처리
            log.error("Cognito에서 사용자 정보 조회 중 오류가 발생했습니다: {}", e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            // 기타 예외 처리
            log.error("예상치 못한 오류가 발생했습니다: {}", e.getMessage());
        }
        return  null; //오류 발생
    }

    private void publishSmsMessage(String phoneNumber, String message) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .phoneNumber(phoneNumber)
                    .build();

            snsClient.publish(request);
            log.info("문자 메시지 발송 완료: " + phoneNumber);

        } catch (Exception e) {
            log.info("문자 메시지 발송 실패: " + e.getMessage());
        }
    }
}
