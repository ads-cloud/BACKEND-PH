package com.ph.backend.service;

import com.ph.backend.config.RabbitMQConfig;
import com.ph.backend.dto.QuorumResponseDto;
import com.ph.backend.model.Asamblea;
import com.ph.backend.repository.AsambleaRepository;
import com.ph.backend.repository.AsistenciaAsambleaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuorumService {

    private final AsistenciaAsambleaRepository asistenciaRepository;
    private final AsambleaRepository asambleaRepository;
    private final com.ph.backend.repository.UnidadPrivadaRepository unidadPrivadaRepository;
    private final EventPublisherService eventPublisherService;

    @Transactional(readOnly = true)
    public QuorumResponseDto calcularQuorum(Long asambleaId) {
        Asamblea asamblea = asambleaRepository.findById(asambleaId)
                .orElseThrow(() -> new IllegalArgumentException("Asamblea no encontrada con ID: " + asambleaId));

        Double coeficienteAcumulado = asistenciaRepository.sumCoeficienteByAsambleaId(asambleaId);
        if (coeficienteAcumulado == null) {
            coeficienteAcumulado = 0.0;
        }

        Long totalAsistentes = asistenciaRepository.countAsistentesByAsambleaId(asambleaId);
        Long totalUnidadesCopropiedad = unidadPrivadaRepository.countByCopropiedadId(asamblea.getCopropiedad().getId());

        // Coeficiente total de la copropiedad (Ley 675 = 100%)
        Double totalCopropiedad = asamblea.getCopropiedad().getTotalCoeficiente();
        double porcentaje = (coeficienteAcumulado / totalCopropiedad) * 100.0;

        // Quórum deliberatorio y decisorio simple: > 50.0%
        boolean alcanzoQuorum = porcentaje > 50.0000;

        return QuorumResponseDto.builder()
                .asambleaId(asamblea.getId())
                .tituloAsamblea(asamblea.getTitulo())
                .totalUnidadesAsistentes(totalAsistentes)
                .totalUnidadesCopropiedad(totalUnidadesCopropiedad)
                .coeficienteAcumulado(coeficienteAcumulado)
                .porcentajeQuorum(Math.round(porcentaje * 10000.0) / 10000.0)
                .alcanzoQuorum(alcanzoQuorum)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public QuorumResponseDto recalcularYNotificarQuorum(Long asambleaId) {
        QuorumResponseDto dto = calcularQuorum(asambleaId);
        
        // Notificar evento en RabbitMQ
        eventPublisherService.publishEvent(
                RabbitMQConfig.ROUTING_KEY_QUORUM,
                "QUORUM_ACTUALIZADO",
                asambleaId,
                dto
        );

        return dto;
    }
}
