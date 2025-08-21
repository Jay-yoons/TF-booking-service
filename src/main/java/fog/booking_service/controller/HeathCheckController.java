package fog.booking_service.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class HeathCheckController {
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("health check");
        Map<String, Object> res = new HashMap<>();
        res.put("status", "UP");
        res.put("service", "booking-service");
        return ResponseEntity.ok(res);
    }
}
