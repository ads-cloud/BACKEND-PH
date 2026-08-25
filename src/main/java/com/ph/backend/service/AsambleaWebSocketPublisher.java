package com.ph.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsambleaWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notifica a todos los clientes suscritos que la lista o estado de preguntas de una asamblea ha cambiado.
     */
    public void notifyPreguntasUpdated(Long asambleaId) {
        log.info("📡 [WEBSOCKET BROADCAST] Notificando actualización de preguntas para asambleaId: {}", asambleaId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "PREGUNTAS_UPDATED");
        payload.put("asambleaId", asambleaId);
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/asamblea/" + asambleaId + "/preguntas", payload);
        messagingTemplate.convertAndSend("/topic/preguntas", payload);
    }

    /**
     * Notifica actualización en vivo del quórum de la asamblea.
     */
    public void notifyQuorumUpdated(Long asambleaId) {
        log.info("📡 [WEBSOCKET BROADCAST] Notificando cambio de quórum para asambleaId: {}", asambleaId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "QUORUM_UPDATED");
        payload.put("asambleaId", asambleaId);
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/asamblea/" + asambleaId + "/quorum", payload);
        messagingTemplate.convertAndSend("/topic/quorum", payload);
    }

    /**
     * Notifica que se registró un nuevo voto en una pregunta.
     */
    public void notifyVotoRegistrado(Long asambleaId, Long preguntaId) {
        log.info("📡 [WEBSOCKET BROADCAST] Notificando nuevo voto en preguntaId: {} (asambleaId: {})", preguntaId, asambleaId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "VOTO_REGISTRADO");
        payload.put("asambleaId", asambleaId);
        payload.put("preguntaId", preguntaId);
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/asamblea/" + asambleaId + "/votos", payload);
        messagingTemplate.convertAndSend("/topic/resultados", payload);
    }
}
