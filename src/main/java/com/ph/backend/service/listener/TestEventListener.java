package com.ph.backend.service.listener;

import com.ph.backend.config.RabbitMQConfig;
import com.ph.backend.dto.EventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_QUORUM)
    public void handleQuorumEvent(EventDto<?> event) {
        log.info("📢 [RabbitMQ -> WebSocket /topic/quorum] Retransmitiendo evento: {}", event.getEventType());
        messagingTemplate.convertAndSend("/topic/quorum", event);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_VOTO)
    public void handleVotoEvent(EventDto<?> event) {
        log.info("🗳️ [RabbitMQ -> WebSocket /topic/resultados] Retransmitiendo evento: {}", event.getEventType());
        messagingTemplate.convertAndSend("/topic/resultados", event);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EVENTS)
    public void handleGeneralEvent(EventDto<?> event) {
        log.info("🔔 [RabbitMQ General Event] Evento: {}", event);
    }
}
