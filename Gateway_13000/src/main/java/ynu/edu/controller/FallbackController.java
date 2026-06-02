package ynu.edu.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping("/fallback")
    public ResponseEntity<Map<String, Object>> fallback() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("message", "服务暂时不可用，已触发熔断降级");
        result.put("data", null);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }
}
