package com.ph.backend.service;

import com.ph.backend.dto.HealthStatusDto;
import com.ph.backend.model.Asamblea;
import com.ph.backend.model.AsambleaStatus;
import com.ph.backend.model.Copropiedad;
import com.ph.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthService {

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final CopropiedadRepository copropiedadRepository;
    private final UnidadPrivadaRepository unidadPrivadaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsambleaRepository asambleaRepository;
    private final AsistenciaAsambleaRepository asistenciaAsambleaRepository;
    private final NotificacionEmailRepository notificacionEmailRepository;
    private final com.ph.backend.repository.VotoEmitidoRepository votoEmitidoRepository;

    public HealthStatusDto checkHealth() {
        String dbStatus = "UP";
        String rabbitStatus = "UP";

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            log.error("Health check - PostgreSQL connection error: {}", e.getMessage());
            dbStatus = "DOWN: " + e.getMessage();
        }

        // Monitoreo de Colas RabbitMQ
        List<HealthStatusDto.RabbitColaMonitoreoDto> colasRabbit = new ArrayList<>();
        String[] queuesToMonitor = {
            com.ph.backend.config.RabbitMQConfig.QUEUE_QUORUM,
            com.ph.backend.config.RabbitMQConfig.QUEUE_VOTO,
            com.ph.backend.config.RabbitMQConfig.QUEUE_EVENTS
        };

        try {
            rabbitTemplate.execute(channel -> {
                channel.getConnection().getServerProperties();
                for (String qName : queuesToMonitor) {
                    try {
                        var declareOk = channel.queueDeclarePassive(qName);
                        colasRabbit.add(HealthStatusDto.RabbitColaMonitoreoDto.builder()
                                .nombreCola(qName)
                                .mensajesPendientes(declareOk.getMessageCount())
                                .consumidoresActivos(declareOk.getConsumerCount())
                                .estado("ACTIVA")
                                .build());
                    } catch (Exception qErr) {
                        colasRabbit.add(HealthStatusDto.RabbitColaMonitoreoDto.builder()
                                .nombreCola(qName)
                                .mensajesPendientes(0)
                                .consumidoresActivos(0)
                                .estado("INACTIVA / NO CREADA")
                                .build());
                    }
                }
                return true;
            });
        } catch (Exception e) {
            log.error("Health check - RabbitMQ connection error: {}", e.getMessage());
            rabbitStatus = "DOWN: " + e.getMessage();
        }

        // Monitoreo de Notificaciones por Correo
        HealthStatusDto.NotificacionesMonitoreoDto notifDto = null;
        if (dbStatus.startsWith("UP")) {
            try {
                long pend = notificacionEmailRepository.countByEstado(com.ph.backend.model.EstadoNotificacion.PENDIENTE);
                long env = notificacionEmailRepository.countByEstado(com.ph.backend.model.EstadoNotificacion.ENVIADO);
                long fal = notificacionEmailRepository.countByEstado(com.ph.backend.model.EstadoNotificacion.FALLIDO);

                notifDto = HealthStatusDto.NotificacionesMonitoreoDto.builder()
                        .pendientes(pend)
                        .enviados(env)
                        .fallidos(fal)
                        .total(pend + env + fal)
                        .build();
            } catch (Exception e) {
                log.error("Error al calcular estadísticas de notificaciones email: {}", e.getMessage());
            }
        }

        String overallStatus = (dbStatus.startsWith("UP") && rabbitStatus.startsWith("UP")) ? "UP" : "DEGRADED";

        List<HealthStatusDto.CopropiedadMonitoreoDto> monitoreoList = new ArrayList<>();

        if (dbStatus.startsWith("UP")) {
            try {
                List<Copropiedad> copropiedades = copropiedadRepository.findAll();
                List<Long> copIds = copropiedades.stream().map(Copropiedad::getId).toList();

                if (!copIds.isEmpty()) {
                    // 1. Consultas Bulk
                    List<Object[]> unidadesData = unidadPrivadaRepository.countByCopropiedadIdsIn(copIds);
                    List<Object[]> usuariosData = usuarioRepository.countUsuariosActivosByCopropiedadIdsIn(copIds);
                    List<Object[]> votosData = votoEmitidoRepository.countByCopropiedadIdsIn(copIds);
                    List<Asamblea> asambleasActivas = asambleaRepository.findByCopropiedadIdInAndEstadoIn(
                            copIds, List.of(AsambleaStatus.EN_CURSO, AsambleaStatus.EN_REGISTRO)
                    );

                    List<Long> asambleaIds = asambleasActivas.stream().map(Asamblea::getId).toList();
                    List<Object[]> quorumData = asambleaIds.isEmpty() ? List.of() : asistenciaAsambleaRepository.countAsistentesByAsambleaIdsIn(asambleaIds);

                    // 2. Mapas en memoria O(1)
                    Map<Long, Long> unidadesMap = new HashMap<>();
                    for (Object[] row : unidadesData) unidadesMap.put((Long) row[0], (Long) row[1]);

                    Map<Long, Long> usuariosMap = new HashMap<>();
                    for (Object[] row : usuariosData) usuariosMap.put((Long) row[0], (Long) row[1]);

                    Map<Long, Long> votosMap = new HashMap<>();
                    for (Object[] row : votosData) votosMap.put((Long) row[0], (Long) row[1]);

                    Map<Long, Long> quorumMap = new HashMap<>();
                    for (Object[] row : quorumData) quorumMap.put((Long) row[0], (Long) row[1]);

                    Map<Long, Asamblea> asambleaMap = new HashMap<>();
                    for (Asamblea a : asambleasActivas) {
                        Long cId = a.getCopropiedad().getId();
                        // Priorizar EN_CURSO sobre EN_REGISTRO
                        if (!asambleaMap.containsKey(cId) || a.getEstado() == AsambleaStatus.EN_CURSO) {
                            asambleaMap.put(cId, a);
                        }
                    }

                    // 3. Ensamblaje en Memoria O(N) sin SQL
                    for (Copropiedad cop : copropiedades) {
                        Long copId = cop.getId();
                        Long unidades = unidadesMap.getOrDefault(copId, 0L);
                        Long usuariosActivos = usuariosMap.getOrDefault(copId, 0L);
                        Long votosRegistrados = votosMap.getOrDefault(copId, 0L);

                        Asamblea asamblea = asambleaMap.get(copId);
                        boolean tieneAsamblea = asamblea != null;
                        Long asambleaId = tieneAsamblea ? asamblea.getId() : null;
                        String asambleaTitulo = tieneAsamblea ? asamblea.getTitulo() : null;
                        String asambleaEstado = tieneAsamblea ? asamblea.getEstado().name() : null;
                        Long asistentesQuorum = (tieneAsamblea && asambleaId != null) ? quorumMap.getOrDefault(asambleaId, 0L) : 0L;

                        monitoreoList.add(HealthStatusDto.CopropiedadMonitoreoDto.builder()
                                .copropiedadId(copId)
                                .copropiedadNombre(cop.getNombre())
                                .nit(com.ph.backend.controller.CopropiedadController.formatearNit(cop.getNit()))
                                .unidadesRegistradas(unidades)
                                .usuariosActivos(usuariosActivos)
                                .tieneAsambleaActiva(tieneAsamblea)
                                .asambleaActivaId(asambleaId)
                                .asambleaTitulo(asambleaTitulo)
                                .asambleaEstado(asambleaEstado)
                                .asistentesQuorum(asistentesQuorum)
                                .votosRegistrados(votosRegistrados)
                                .build());
                    }
                }
            } catch (Exception e) {
                log.error("Error al obtener datos de monitoreo por PH: {}", e.getMessage());
            }
        }

        return HealthStatusDto.builder()
                .status(overallStatus)
                .database(dbStatus)
                .rabbitmq(rabbitStatus)
                .timestamp(LocalDateTime.now())
                .notificacionesEmail(notifDto)
                .colasRabbit(colasRabbit)
                .copropiedadesMonitoreo(monitoreoList)
                .build();
    }
}
