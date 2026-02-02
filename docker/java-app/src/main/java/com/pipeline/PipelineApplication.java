package com.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PipelineApplication {
    private static final String KAFKA_SERVERS = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    private static final String POSTGRES_URL = System.getenv().getOrDefault("POSTGRES_URL", "jdbc:postgresql://localhost:5432/pipeline_db");
    private static final String POSTGRES_USER = System.getenv().getOrDefault("POSTGRES_USER", "pipeline_user");
    private static final String POSTGRES_PASSWORD = System.getenv().getOrDefault("POSTGRES_PASSWORD", "pipeline_pass");
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        System.out.println("Starting Pipeline Application...");
        
        // Start producer in a separate thread
        Thread producerThread = new Thread(PipelineApplication::runProducer);
        producerThread.start();
        
        // Start consumer
        runConsumer();
    }

    private static void runProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    String eventId = UUID.randomUUID().toString();
                    String event = String.format(
                        "{\"eventId\":\"%s\",\"eventType\":\"user_action\",\"userId\":%d,\"action\":\"click\",\"timestamp\":%d}",
                        eventId, (int)(Math.random() * 1000), System.currentTimeMillis()
                    );
                    
                    ProducerRecord<String, String> record = new ProducerRecord<>("events", eventId, event);
                    producer.send(record);
                    System.out.println("Produced event: " + eventId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 5, TimeUnit.SECONDS);
            
            // Keep producer running
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                scheduler.shutdown();
            }
        }
    }

    private static void runConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "pipeline-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList("events"));
            
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    processEvent(record.key(), record.value());
                }
            }
        }
    }

    private static void processEvent(String key, String value) {
        try (Connection conn = DriverManager.getConnection(POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD)) {
            String sql = "INSERT INTO pipeline.events (event_id, event_type, payload, source_topic) VALUES (?, ?, ?::jsonb, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                var eventData = objectMapper.readTree(value);
                
                stmt.setString(1, key);
                stmt.setString(2, eventData.get("eventType").asText());
                stmt.setString(3, value);
                stmt.setString(4, "events");
                
                stmt.executeUpdate();
                System.out.println("Processed event: " + key);
            }
        } catch (Exception e) {
            System.err.println("Error processing event: " + e.getMessage());
        }
    }
}
