package com.pipeline.service;

import com.pipeline.avro.Event;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class EventProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventProcessingService.class);
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private final Counter eventsProcessed;
    private final Counter eventsProduced;
    private final Timer processingTimer;
    
    public EventProcessingService(MeterRegistry meterRegistry) {
        this.eventsProcessed = Counter.builder("events.processed")
            .description("Number of events processed")
            .register(meterRegistry);
        this.eventsProduced = Counter.builder("events.produced")
            .description("Number of events produced")
            .register(meterRegistry);
        this.processingTimer = Timer.builder("event.processing.time")
            .description("Event processing time")
            .register(meterRegistry);
    }
    
    public void produceEvent(String eventType, String userId, Map<String, String> payload) {
        Timer.Sample sample = Timer.start();
        
        try {
            Event event = Event.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType(eventType)
                .setUserId(userId)
                .setTimestamp(Instant.now().toEpochMilli())
                .setPayload(payload != null ? payload : new HashMap<>())
                .setMetadata(com.pipeline.avro.Metadata.newBuilder()
                    .setSource("pipeline-app")
                    .setVersion("1.0")
                    .setCorrelationId(UUID.randomUUID().toString())
                    .build())
                .build();
            
            kafkaTemplate.send("events", userId, event);
            eventsProduced.increment();
            
            // Cache user activity in Redis for real-time features
            cacheUserActivity(userId, eventType);
            
            logger.info("Produced event: {} for user: {}", eventType, userId);
            
        } finally {
            sample.stop(processingTimer);
        }
    }
    
    @KafkaListener(topics = "events", groupId = "pipeline-consumer")
    @CircuitBreaker(name = "event-processing", fallbackMethod = "fallbackProcessEvent")
    public void processEvent(Event event, Acknowledgment ack) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.info("Processing event: {} for user: {}", event.getEventType(), event.getUserId());
            
            // Simulate complex processing
            processBusinessLogic(event);
            
            // Update real-time metrics in Redis
            updateRealTimeMetrics(event);
            
            eventsProcessed.increment();
            ack.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing event: {}", event.getEventId(), e);
            // Send to DLQ in production
            throw e;
        } finally {
            sample.stop(processingTimer);
        }
    }
    
    public void fallbackProcessEvent(Event event, Acknowledgment ack, Exception ex) {
        logger.warn("Circuit breaker activated for event: {}", event.getEventId());
        // Implement fallback logic
        ack.acknowledge();
    }
    
    private void processBusinessLogic(Event event) {
        // Simulate data enrichment, validation, transformation
        switch (event.getEventType().toString()) {
            case "user_signup":
                handleUserSignup(event);
                break;
            case "purchase":
                handlePurchase(event);
                break;
            case "page_view":
                handlePageView(event);
                break;
            default:
                logger.debug("Unknown event type: {}", event.getEventType());
        }
    }
    
    private void handleUserSignup(Event event) {
        // Real-time user onboarding logic
        String userId = event.getUserId().toString();
        redisTemplate.opsForValue().set("user:new:" + userId, "true", 24, TimeUnit.HOURS);
    }
    
    private void handlePurchase(Event event) {
        // Real-time purchase analytics
        String userId = event.getUserId().toString();
        redisTemplate.opsForValue().increment("user:purchases:" + userId);
    }
    
    private void handlePageView(Event event) {
        // Real-time engagement tracking
        String userId = event.getUserId().toString();
        redisTemplate.opsForValue().increment("user:pageviews:" + userId);
    }
    
    private void cacheUserActivity(String userId, String eventType) {
        String key = "user:activity:" + userId;
        redisTemplate.opsForHash().increment(key, eventType, 1);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }
    
    private void updateRealTimeMetrics(Event event) {
        String eventType = event.getEventType().toString();
        redisTemplate.opsForValue().increment("metrics:events:" + eventType);
        redisTemplate.opsForValue().increment("metrics:events:total");
    }
}
