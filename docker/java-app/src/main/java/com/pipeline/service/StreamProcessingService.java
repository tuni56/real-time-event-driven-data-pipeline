package com.pipeline.service;

import com.pipeline.avro.Event;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class StreamProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamProcessingService.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private final Counter streamEventsProcessed;
    
    public StreamProcessingService(MeterRegistry meterRegistry) {
        this.streamEventsProcessed = Counter.builder("stream.events.processed")
            .description("Number of events processed by Kafka Streams")
            .register(meterRegistry);
    }
    
    @Bean
    public KStream<String, Event> processEventStream(StreamsBuilder streamsBuilder) {
        
        // Main event stream
        KStream<String, Event> eventStream = streamsBuilder
            .stream("events", Consumed.with(Serdes.String(), null));
        
        // Real-time aggregations - User activity windows
        eventStream
            .filter((key, event) -> event.getUserId() != null)
            .groupBy((key, event) -> event.getUserId().toString())
            .windowedBy(TimeWindows.of(Duration.ofMinutes(5)).advanceBy(Duration.ofMinutes(1)))
            .count()
            .toStream()
            .foreach((windowedUserId, count) -> {
                String userId = windowedUserId.key();
                String window = windowedUserId.window().startTime().toString();
                
                // Store in Redis for real-time dashboards
                String redisKey = "user:activity:window:" + userId + ":" + window;
                redisTemplate.opsForValue().set(redisKey, count, 1, TimeUnit.HOURS);
                
                logger.debug("User {} activity in window {}: {} events", userId, window, count);
            });
        
        // Event type aggregations
        eventStream
            .groupBy((key, event) -> event.getEventType().toString())
            .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
            .count()
            .toStream()
            .foreach((windowedEventType, count) -> {
                String eventType = windowedEventType.key();
                
                // Update real-time metrics
                redisTemplate.opsForValue().set("metrics:stream:" + eventType, count, 5, TimeUnit.MINUTES);
                streamEventsProcessed.increment();
            });
        
        // Fraud detection stream (example of complex processing)
        KStream<String, Event> purchaseStream = eventStream
            .filter((key, event) -> "purchase".equals(event.getEventType().toString()));
        
        purchaseStream
            .groupBy((key, event) -> event.getUserId().toString())
            .windowedBy(TimeWindows.of(Duration.ofMinutes(10)))
            .count()
            .toStream()
            .filter((windowedUserId, purchaseCount) -> purchaseCount > 5) // Suspicious activity
            .foreach((windowedUserId, count) -> {
                String userId = windowedUserId.key();
                logger.warn("🚨 Potential fraud detected: User {} made {} purchases in 10 minutes", userId, count);
                
                // Store fraud alert
                redisTemplate.opsForValue().set("fraud:alert:" + userId, count, 1, TimeUnit.HOURS);
            });
        
        // High-value purchase stream
        eventStream
            .filter((key, event) -> "purchase".equals(event.getEventType().toString()))
            .filter((key, event) -> {
                String amount = event.getPayload().get("amount");
                return amount != null && Double.parseDouble(amount) > 500;
            })
            .foreach((key, event) -> {
                logger.info("💰 High-value purchase: User {} spent ${}", 
                    event.getUserId(), event.getPayload().get("amount"));
                
                // Trigger real-time notifications, recommendations, etc.
                redisTemplate.opsForList().leftPush("high_value_purchases", 
                    event.getUserId() + ":" + event.getPayload().get("amount"));
            });
        
        return eventStream;
    }
}
