package com.ph.backend.service;

import com.ph.backend.config.RabbitMQConfig;
import com.ph.backend.dto.ResultadoOpcionDto;
import com.ph.backend.dto.ResultadoPreguntaDto;
import com.ph.backend.dto.VotoRequestDto;
import com.ph.backend.model.*;
import com.ph.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VotacionService {

    private final PreguntaRepository preguntaRepository;
    private final OpcionVotoRepository opcionVotoRepository;
    private final UnidadPrivadaRepository unidadPrivadaRepository;
    private final VotoEmitidoRepository votoEmitidoRepository;
    private final AsistenciaAsambleaRepository asistenciaRepository;
    private final EventPublisherService eventPublisherService;

    public java.util.Map<Long, Long> obtenerVotosPorUnidad(Long unidadPrivadaId) {
        List<VotoEmitido> votos = votoEmitidoRepository.findAll().stream()
                .filter(v -> v.getUnidadPrivada().getId().equals(unidadPrivadaId))
                .toList();

        java.util.Map<Long, Long> mapa = new java.util.HashMap<>();
        for (VotoEmitido v : votos) {
            mapa.put(v.getPregunta().getId(), v.getOpcionVoto().getId());
        }
        return mapa;
    }

    @Transactional
    public VotoEmitido registrarVoto(VotoRequestDto dto) {
        Pregunta pregunta = preguntaRepository.findById(dto.getPreguntaId())
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada con ID: " + dto.getPreguntaId()));

        if (pregunta.getEstado() != PreguntaStatus.ABIERTA) {
            throw new IllegalStateException("La votación para esta pregunta no está ABIERTA. Estado actual: " + pregunta.getEstado());
        }

        if (pregunta.getFechaHoraCierre() != null && LocalDateTime.now().isAfter(pregunta.getFechaHoraCierre())) {
            pregunta.setEstado(PreguntaStatus.CERRADA);
            preguntaRepository.save(pregunta);
            throw new IllegalStateException("El tiempo para responder a esta pregunta ha expirado. Votación CERRADA.");
        }

        UnidadPrivada unidad = unidadPrivadaRepository.findById(dto.getUnidadPrivadaId())
                .orElseThrow(() -> new IllegalArgumentException("Unidad Privada no encontrada con ID: " + dto.getUnidadPrivadaId()));

        // Validar que la unidad privada registró asistencia a la asamblea
        Long asambleaId = pregunta.getAsamblea().getId();
        boolean asistio = asistenciaRepository.existsByAsambleaIdAndUnidadPrivadaId(asambleaId, unidad.getId());
        if (!asistio) {
            throw new IllegalStateException("La unidad " + unidad.getTorre() + "-" + unidad.getNumeroUnidad() + 
                    " no tiene asistencia registrada a la asamblea.");
        }

        // Validar unicidad de voto por unidad e inmueble por pregunta (Ley 675)
        boolean yaVoto = votoEmitidoRepository.existsByPreguntaIdAndUnidadPrivadaId(pregunta.getId(), unidad.getId());
        if (yaVoto) {
            throw new IllegalStateException("La unidad " + unidad.getTorre() + "-" + unidad.getNumeroUnidad() + 
                    " ya emitió un voto para esta pregunta.");
        }

        OpcionVoto opcion = opcionVotoRepository.findById(dto.getOpcionVotoId())
                .orElseThrow(() -> new IllegalArgumentException("Opción de voto no encontrada con ID: " + dto.getOpcionVotoId()));

        VotoEmitido voto = VotoEmitido.builder()
                .pregunta(pregunta)
                .opcionVoto(opcion)
                .unidadPrivada(unidad)
                .coeficienteAplicado(unidad.getCoeficiente()) // Voto ponderado por coeficiente
                .fechaHoraVoto(LocalDateTime.now())
                .build();

        VotoEmitido votoGuardado = votoEmitidoRepository.save(voto);

        log.info("Voto registrado con éxito: Pregunta {}, Unidad {}-{}, Coeficiente: {}%, Opción: {}",
                pregunta.getId(), unidad.getTorre(), unidad.getNumeroUnidad(), unidad.getCoeficiente(), opcion.getTexto());

        // Emitir evento RabbitMQ
        eventPublisherService.publishEvent(
                RabbitMQConfig.ROUTING_KEY_VOTO,
                "VOTO_REGISTRADO",
                asambleaId,
                dto
        );

        return votoGuardado;
    }

    @Transactional(readOnly = true)
    public ResultadoPreguntaDto obtenerResultados(Long preguntaId) {
        Pregunta pregunta = preguntaRepository.findById(preguntaId)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada con ID: " + preguntaId));

        Long asambleaId = pregunta.getAsamblea().getId();
        Double quorumPresente = asistenciaRepository.sumCoeficienteByAsambleaId(asambleaId);
        if (quorumPresente == null) quorumPresente = 0.0;

        Long totalAsistentes = asistenciaRepository.countAsistentesByAsambleaId(asambleaId);
        Long totalUnidadesVotaron = votoEmitidoRepository.countByPreguntaId(preguntaId);

        Double totalCoeficienteVotado = votoEmitidoRepository.sumTotalCoeficienteByPreguntaId(preguntaId);
        if (totalCoeficienteVotado == null) totalCoeficienteVotado = 0.0;

        List<OpcionVoto> opciones = pregunta.getOpciones();
        List<ResultadoOpcionDto> listaResultadosOpciones = new ArrayList<>();

        OpcionVoto opcionGanadora = null;
        double maxCoeficienteOpcion = -1.0;

        // Base de cálculo ponderada: Coeficiente total de asistentes presentes en la asamblea
        double baseCoeficienteTotal = (quorumPresente > 0) ? quorumPresente : (totalCoeficienteVotado > 0 ? totalCoeficienteVotado : 100.0);

        for (OpcionVoto opcion : opciones) {
            Long countVotos = votoEmitidoRepository.countVotosByOpcionId(opcion.getId());
            Double sumaCoef = votoEmitidoRepository.sumCoeficienteByOpcionId(opcion.getId());
            if (sumaCoef == null) sumaCoef = 0.0;

            // Porcentaje real sobre el quórum/coeficiente total presente en la asamblea
            double pctPonderado = (sumaCoef / baseCoeficienteTotal) * 100.0;

            if (sumaCoef > maxCoeficienteOpcion) {
                maxCoeficienteOpcion = sumaCoef;
                opcionGanadora = opcion;
            }

            listaResultadosOpciones.add(ResultadoOpcionDto.builder()
                    .opcionId(opcion.getId())
                    .textoOpcion(opcion.getTexto())
                    .totalVotosUnidades(countVotos)
                    .sumaSistemaCoeficientes(sumaCoef)
                    .sumaCoeficiente(sumaCoef)
                    .porcentajePonderado(Math.round(pctPonderado * 100.0) / 100.0)
                    .build());
        }

        // Métrica Virtual de Abstención (Inmuebles presentes que no han registrado su voto aún)
        long countAbstencionUnidades = Math.max(0L, totalAsistentes - totalUnidadesVotaron);
        double coefAbstencion = Math.max(0.0, baseCoeficienteTotal - totalCoeficienteVotado);
        double pctAbstencion = (coefAbstencion / baseCoeficienteTotal) * 100.0;

        listaResultadosOpciones.add(ResultadoOpcionDto.builder()
                .opcionId(-1L) // ID especial virtual para abstención
                .textoOpcion("Abstención (Sin Votar)")
                .totalVotosUnidades(countAbstencionUnidades)
                .sumaSistemaCoeficientes(coefAbstencion)
                .sumaCoeficiente(coefAbstencion)
                .porcentajePonderado(Math.round(pctAbstencion * 100.0) / 100.0)
                .build());

        // Evaluación de la Aprobación conforme Ley 675 de 2001 Colombia
        boolean aprobado = false;
        String mensajeResultado;

        if (totalCoeficienteVotado == 0.0) {
            mensajeResultado = "Sin votos registrados hasta el momento.";
        } else if (pregunta.getTipoMayoria() == PreguntaType.MAYORIA_CALIFICADA) {
            // Ley 675 Art. 46: Requiere mayoría calificada del 70% del total de coeficientes de la copropiedad
            Double totalCopropiedad = pregunta.getAsamblea().getCopropiedad().getTotalCoeficiente();
            double pctSobreTotal = (maxCoeficienteOpcion / totalCopropiedad) * 100.0;

            aprobado = (pctSobreTotal >= 70.0000);
            mensajeResultado = aprobado
                    ? String.format("APROBADO por Mayoría Calificada (%.2f%% del total de la copropiedad >= 70%%)", pctSobreTotal)
                    : String.format("RECHAZADO. Alcanzó %.2f%% del total de la copropiedad (Requerido: 70%% Ley 675)", pctSobreTotal);
        } else {
            // MAYORIA_SIMPLE: Mayoría de los votos presentes ( > 50% del total coeficiente votado)
            double pctPonderadoVotado = (maxCoeficienteOpcion / totalCoeficienteVotado) * 100.0;
            aprobado = (pctPonderadoVotado > 50.0) && (opcionGanadora != null && !opcionGanadora.getTexto().toLowerCase().contains("rechaz"));

            mensajeResultado = aprobado
                    ? String.format("APROBADO por Mayoría Simple (%s con %.2f%% de los votos válidos)", 
                        opcionGanadora != null ? opcionGanadora.getTexto() : "", pctPonderadoVotado)
                    : String.format("NO APROBADO por Mayoría Simple (Opción predominante: %s con %.2f%%)", 
                        opcionGanadora != null ? opcionGanadora.getTexto() : "N/A", pctPonderadoVotado);
        }

        return ResultadoPreguntaDto.builder()
                .preguntaId(pregunta.getId())
                .enunciado(pregunta.getEnunciado())
                .estado(pregunta.getEstado())
                .tipoMayoria(pregunta.getTipoMayoria())
                .quorumPresenteAtVoto(quorumPresente)
                .totalCoeficienteVotado(totalCoeficienteVotado)
                .totalUnidadesVotaron(totalUnidadesVotaron)
                .totalUnidadesAsistentes(totalAsistentes)
                .aprobado(aprobado)
                .mensajeResultado(mensajeResultado)
                .opciones(listaResultadosOpciones)
                .build();
    }
}
