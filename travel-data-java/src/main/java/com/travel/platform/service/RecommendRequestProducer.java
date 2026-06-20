package com.travel.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.platform.dto.RecommendRequestEvent;
import com.travel.platform.mapper.RecommendRequestEventMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnBean(KafkaTemplate.class)
public class RecommendRequestProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RecommendRequestEventMapper eventMapper;
    private final String topic;

    public RecommendRequestProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            RecommendRequestEventMapper eventMapper,
            @Value("${realtime.kafka.recommend-request-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.eventMapper = eventMapper;
        this.topic = topic;
    }

    public void publish(RecommendRequestEvent event) {
        try {
            eventMapper.insert(event);
        } catch (Exception e) {
            log.warn("Failed to persist recommend request event, requestId={}", event.getRequestId(), e);
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.getRequestId(), payload);
        } catch (Exception e) {
            log.warn("Failed to send recommend request event to Kafka, requestId={}", event.getRequestId(), e);
        }
    }
}
