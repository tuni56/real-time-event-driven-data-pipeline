package com.pipeline.controller;

import com.pipeline.service.EventProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EventController {
    
    @Autowired
    private EventProcessingService eventProcessingService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @PostMapping("/events")
    public ResponseEntity<Map<String, String>> produceEvent(
            @RequestParam String eventType,
            @RequestParam String userId,
            @RequestBody(required = false) Map<String, String> payload) {
        
        eventProcessingService.produceEvent(eventType, userId, payload);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Event produced successfully");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/metrics/realtime")
    public ResponseEntity<Map<String, Object>> getRealTimeMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Get real-time event counts
        Object totalEvents = redisTemplate.opsForValue().get("metrics:events:total");
        Object userSignups = redisTemplate.opsForValue().get("metrics:events:user_signup");
        Object purchases = redisTemplate.opsForValue().get("metrics:events:purchase");
        Object pageViews = redisTemplate.opsForValue().get("metrics:events:page_view");
        
        metrics.put("totalEvents", totalEvents != null ? totalEvents : 0);
        metrics.put("userSignups", userSignups != null ? userSignups : 0);
        metrics.put("purchases", purchases != null ? purchases : 0);
        metrics.put("pageViews", pageViews != null ? pageViews : 0);
        
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/user/{userId}/activity")
    public ResponseEntity<Map<String, Object>> getUserActivity(@PathVariable String userId) {
        String key = "user:activity:" + userId;
        Map<Object, Object> activity = redisTemplate.opsForHash().entries(key);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("activity", activity);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "pipeline-app");
        return ResponseEntity.ok(status);
    }
}
