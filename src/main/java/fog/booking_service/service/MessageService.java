package fog.booking_service.service;

import fog.booking_service.domain.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    private final SnsClient snsClient;
    private final CognitoIdentityProviderClient cognitoClient;
    private final RestTemplate restTemplate;

    public void sendMessage(Booking booking, String userName) {
        // 1. Cognito에서 전화번호 조회
        String phoneNumber = getPhoneNumberFromCognito(userName);
        LocalDateTime bookingDate = booking.getBookingDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
        String date = bookingDate.format(formatter);
        int count = booking.getCount();
        String storeId = booking.getStoreId();

        // 2. 예약 완료 문자 발송
        if (phoneNumber != null) {
            String storeName = getStoreName(storeId);
            String message = String.format("🍽️ [Talking Potato]\n%s시 %d명 예약이 완료되었습니다.\n가게: %s\n감사합니다!", 
                                          date, count, storeName);
            publishSmsMessage(phoneNumber, message);
        }
    }

    private String getPhoneNumberFromCognito(String userName) {
        try {
            log.info("userPoolId={}, userName={}", userPoolId, userName);
            AdminGetUserRequest request = AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userName)
                    .build();

            AdminGetUserResponse response = cognitoClient.adminGetUser(request);

            for (AttributeType attribute : response.userAttributes()) {
                if (attribute.name().equals("phone_number")) {
                    log.info("문자를 발송할 전화번호:{}", attribute.value());
                    return attribute.value();
                }
            }
        } catch (UserNotFoundException e) {
            log.warn("Cognito User Pool에서 사용자를 찾을 수 없습니다: {}", userName);
        } catch (CognitoIdentityProviderException e) {
            // 기타 Cognito 관련 예외 처리
            log.error("Cognito에서 사용자 정보 조회 중 오류가 발생했습니다: {}", e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            // 기타 예외 처리
            log.error("예상치 못한 오류가 발생했습니다: {}", e.getMessage());
        }
        return  null; //오류 발생
    }

    private String getStoreName(String storeId) {
        try {
            // Store Service에서 가게 목록 조회
            String storeServiceUrl = "https://talkingpotato.shop/api/stores";
            log.info("Store Service 호출: {}", storeServiceUrl);
            
            Map[] stores = restTemplate.getForObject(storeServiceUrl, Map[].class);
            
            if (stores != null) {
                for (Map<String, Object> store : stores) {
                    if (storeId.equals(store.get("storeId"))) {
                        String storeName = (String) store.get("storeName");
                        log.info("가게 이름 조회 성공: {} -> {}", storeId, storeName);
                        return storeName;
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Store Service에서 가게 정보 조회 실패: storeId={}, error={}", storeId, e.getMessage());
        }
        
        // 실패 시 기본값 반환
        return "Talking Potato " + storeId;
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
