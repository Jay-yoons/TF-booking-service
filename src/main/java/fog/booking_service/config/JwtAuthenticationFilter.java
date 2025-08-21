package fog.booking_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 토큰의 페이로드 부분 디코딩
                String payloadJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));

                // ObjectMapper를 사용하여 JSON 파싱
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> payloadMap = mapper.readValue(payloadJson, HashMap.class);

                // "cognito:username"과 "sub" 클레임 추출
                String cognitoUsername = (String) payloadMap.get("cognito:username");
                String sub = (String) payloadMap.get("sub");

                if (cognitoUsername != null && sub != null) {
                    // CustomUserDetails 객체 생성 (username에 cognitoUsername, sub에 sub 값 저장)
                    UserDetails userDetails = new CustomUserDetails(cognitoUsername, sub, Collections.emptyList());

                    // Authentication 객체 생성 및 SecurityContext에 저장
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (Exception e) {
                log.error("JWT 토큰 처리 실패", e);
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}