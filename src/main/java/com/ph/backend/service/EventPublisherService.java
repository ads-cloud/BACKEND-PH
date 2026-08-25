package com.ph.backend.service;

import com.ph.backend.config.RabbitMQConfig;
import com.ph.backend.dto.EventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    public <T> void publishEvent(String routingKey, String eventType, Long asambleaId, T payload) {
        try {
            EventDto<T> event = EventDto.<T>builder()
                    .eventType(eventType)
                    .asambleaId(asambleaId)
                    .payload(payload)
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Publicando evento RabbitMQ -> Exchange: {}, RoutingKey: {}, EventType: {}",
                    RabbitMQConfig.EXCHANGE_NAME, routingKey, eventType);

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);
        } catch (Exception e) {
            log.error("Error al publicar evento en RabbitMQ: {}", e.getMessage(), e);
        }
    }
}
