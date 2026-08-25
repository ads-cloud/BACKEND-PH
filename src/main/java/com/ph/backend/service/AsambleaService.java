package com.ph.backend.service;

import com.ph.backend.dto.AsistenciaRequestDto;
import com.ph.backend.dto.AuthResponseDto;
import com.ph.backend.dto.ConfirmarPinAsambleaDto;
import com.ph.backend.dto.IngresoCopropietarioDto;
import com.ph.backend.model.*;
import com.ph.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import com.ph.backend.model.Rol;
import com.ph.backend.model.UnidadHabitante;
import com.ph.backend.repository.UnidadHabitanteRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsambleaService {

    private final AsambleaRepository asambleaRepository;
    private final AsistenciaAsambleaRepository asistenciaRepository;
    private final UnidadPrivadaRepository unidadPrivadaRepository;
    private final UnidadHabitanteRepository habitanteRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PreguntaRepository preguntaRepository;
    private final VotoEmitidoRepository votoEmitidoRepository;
    private final CopropiedadRepository copropiedadRepository;
    private final PuntoOrdenDiaRepository puntoOrdenDiaRepository;
    private final QuorumService quorumService;
    private final NotificacionService notificacionService;

    @Transactional
    public Asamblea crearAsamblea(com.ph.backend.dto.CrearAsambleaDto dto) {
        log.info("[BACKEND ASAMBLEA] 🏛️ Creando nueva asamblea: '{}'", dto.getTitulo());

        Long copId = dto.getCopropiedadId() != null ? dto.getCopropiedadId()
                : com.ph.backend.config.tenant.TenantContext.getCurrentTenant();
        Copropiedad copropiedad = copropiedadRepository.findById(copId)
                .orElseThrow(() -> new IllegalArgumentException("Copropiedad no encontrada con ID: " + copId));

        if (asambleaRepository.existsByCopropiedadIdAndEstadoIn(copId, List.of(AsambleaStatus.PROGRAMADA, AsambleaStatus.EN_REGISTRO, AsambleaStatus.EN_CURSO))) {
            throw new IllegalStateException(
                    "No se puede crear una nueva asamblea porque ya existe una asamblea activa (programada, en registro o en curso) para esta copropiedad. Debe finalizar la asamblea actual primero.");
        }

        if (dto.getOrdenDia() == null || dto.getOrdenDia().stream().noneMatch(p -> p.getTitulo() != null && !p.getTitulo().trim().isEmpty())) {
            throw new IllegalArgumentException("Es obligatorio incluir al menos un punto en el Orden del Día para crear la asamblea.");
        }

        Asamblea asamblea = Asamblea.builder()
                .copropiedad(copropiedad)
                .titulo(dto.getTitulo().trim())
                .fecha(dto.getFecha() != null ? dto.getFecha() : LocalDateTime.now())
                .tipo(dto.getTipo() != null ? dto.getTipo() : AsambleaType.ORDINARIA)
                .estado(AsambleaStatus.PROGRAMADA)
                .ordenDiaDefinitivo(false)
                .build();

        Asamblea guardada = asambleaRepository.save(asamblea);

        // Guardar puntos del orden del día
        int ordenIdx = 1;
        for (com.ph.backend.dto.GuardarOrdenDiaRequestDto.PuntoOrdenDiaItemInputDto item : dto.getOrdenDia()) {
            if (item.getTitulo() == null || item.getTitulo().trim().isEmpty()) continue;
            PuntoOrdenDia punto = PuntoOrdenDia.builder()
                    .asamblea(guardada)
                    .orden(ordenIdx++)
                    .titulo(item.getTitulo().trim())
                    .descripcion(item.getDescripcion() != null ? item.getDescripcion().trim() : null)
                    .completado(false)
                    .build();
            puntoOrdenDiaRepository.save(punto);
        }

        log.info("[BACKEND ASAMBLEA] ✅ Asamblea creada exitosamente en estado PROGRAMADA con {} puntos en Orden del Día (ID: {})", ordenIdx - 1, guardada.getId());
        return guardada;
    }

    @Transactional
    public Asamblea cambiarEstadoAsamblea(Long id, AsambleaStatus nuevoEstado) {
        Asamblea asamblea = asambleaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asamblea no encontrada con ID: " + id));

        if (nuevoEstado == AsambleaStatus.EN_CURSO) {
            if (!Boolean.TRUE.equals(asamblea.getRegistroCerrado())) {
                throw new IllegalStateException("Debes cerrar el registro de asistencia presencial antes de dar inicio a la asamblea.");
            }
            com.ph.backend.dto.QuorumResponseDto qDto = quorumService.calcularQuorum(id);
            if (!Boolean.TRUE.equals(qDto.getAlcanzoQuorum())) {
                throw new IllegalStateException(
                    "No se puede iniciar la asamblea porque no se ha alcanzado el quórum deliberatorio (Quórum actual: " + 
                    String.format("%.2f", qDto.getPorcentajeQuorum()) + "%, mínimo requerido: > 50.00%)."
                );
            }
        }

        if (nuevoEstado == AsambleaStatus.FINALIZADA) {
            com.ph.backend.dto.QuorumResponseDto qDto = quorumService.calcularQuorum(id);
            boolean alcanzoQuorum = Boolean.TRUE.equals(qDto.getAlcanzoQuorum());
            long pendientes = puntoOrdenDiaRepository.countByAsambleaIdAndCompletadoFalse(id);
            long totalPuntos = puntoOrdenDiaRepository.countByAsambleaId(id);

            // Si se alcanzó el quórum, exige completar el orden del día. Si NO hubo quórum, permite finalizar por falta de quórum.
            if (alcanzoQuorum && totalPuntos > 0 && pendientes > 0) {
                throw new IllegalStateException(
                    "No es posible finalizar la asamblea porque aún existen " + pendientes + " punto(s) pendiente(s) en el Orden del Día. Por favor marca todos los puntos como completados."
                );
            }
        }

        asamblea.setEstado(nuevoEstado);
        log.info("[BACKEND ASAMBLEA] 🏛️ Asamblea ID {} cambió su estado a {}", id, nuevoEstado);

        if (nuevoEstado == AsambleaStatus.FINALIZADA) {
            Long copId = asamblea.getCopropiedad() != null ? asamblea.getCopropiedad().getId() : null;
            if (copId != null) {
                inactivarApoderadosAlFinalizarAsamblea(copId);
            }
        }

        return asambleaRepository.save(asamblea);
    }

    private void inactivarApoderadosAlFinalizarAsamblea(Long copropiedadId) {
        log.info("🔒 Finalizando asamblea en copropiedad ID {}: Inactivando usuarios apoderados y poderes...", copropiedadId);
        try {
            List<UnidadPrivada> unidades = unidadPrivadaRepository.findByCopropiedadId(copropiedadId);
            for (UnidadPrivada u : unidades) {
                if (u.getApoderado() != null || Boolean.TRUE.equals(u.getPoderAprobado())) {
                    u.setApoderado(null);
                    u.setPoderAprobado(false);
                    unidadPrivadaRepository.save(u);
                }
                List<UnidadHabitante> habitantes = habitanteRepository.findByUnidadId(u.getId());
                for (UnidadHabitante h : habitantes) {
                    if (Boolean.TRUE.equals(h.getEsApoderadoDesignado())) {
                        h.setEsApoderadoDesignado(false);
                        habitanteRepository.save(h);
                    }
                }
            }

            List<Usuario> apoderados = usuarioRepository.findAll().stream()
                    .filter(u -> u.getRoles() != null && u.getRoles().contains(Rol.ROLE_APODERADO))
                    .filter(u -> u.getCopropiedadesAsignadas() != null && u.getCopropiedadesAsignadas().stream().anyMatch(c -> c.getId().equals(copropiedadId)))
                    .toList();

            for (Usuario usr : apoderados) {
                usr.setActivo(false);
                usuarioRepository.save(usr);
            }
            log.info("🔒 {} usuarios apoderados inactivados tras finalizar la asamblea.", apoderados.size());
        } catch (Exception e) {
            log.error("Error al inactivar apoderados tras finalizar la asamblea", e);
        }
    }

    @Transactional(readOnly = true)
    public com.ph.backend.dto.OrdenDiaResponseDto obtenerOrdenDia(Long asambleaId) {
        Asamblea asamblea = asambleaRepository.findById(asambleaId)
                .orElseThrow(() -> new IllegalArgumentException("Asamblea no encontrada con ID: " + asambleaId));

        List<PuntoOrdenDia> puntos = puntoOrdenDiaRepository.findByAsambleaIdOrderByOrdenAsc(asambleaId);

        List<com.ph.backend.dto.PuntoOrdenDiaDto> dtos = puntos.stream().map(p -> com.ph.backend.dto.PuntoOrdenDiaDto.builder()
                .id(p.getId())
                .orden(p.getOrden())
                .titulo(p.getTitulo())
                .descripcion(p.getDescripcion())
                .completado(p.getCompletado())
                .fechaCompletado(p.getFechaCompletado())
                .build()
        ).toList();

        return com.ph.backend.dto.OrdenDiaResponseDto.builder()
                .asambleaId(asambleaId)
                .ordenDiaDefinitivo(Boolean.TRUE.equals(asamblea.getOrdenDiaDefinitivo()))
                .puntos(dtos)
                .build();
    }

    @Transactional
    public com.ph.backend.dto.OrdenDiaResponseDto guardarOrdenDia(com.ph.backend.dto.GuardarOrdenDiaRequestDto dto) {
        Asamblea asamblea = asambleaRepository.findById(dto.getAsambleaId())
                .orElseThrow(() -> new IllegalArgumentException("Asamblea no encontrada con ID: " + dto.getAsambleaId()));

        if (Boolean.TRUE.equals(asamblea.getOrdenDiaDefinitivo())) {
            throw new IllegalStateException("El Orden del Día ya fue guardado como definitivo y no se puede modificar.");
        }

        puntoOrdenDiaRepository.deleteByAsambleaId(asamblea.getId());

        if (dto.getPuntos() != null && !dto.getPuntos().isEmpty()) {
            int ordenIdx = 1;
            for (com.ph.backend.dto.GuardarOrdenDiaRequestDto.PuntoOrdenDiaItemInputDto item : dto.getPuntos()) {
                if (item.getTitulo() == null || item.getTitulo().trim().isEmpty()) continue;
                PuntoOrdenDia punto = PuntoOrdenDia.builder()
                        .asamblea(asamblea)
                        .orden(ordenIdx++)
                        .titulo(item.getTitulo().trim())
                        .descripcion(item.getDescripcion() != null ? item.getDescripcion().trim() : null)
                        .completado(false)
                        .build();
                puntoOrdenDiaRepository.save(punto);
            }
        }

        if (Boolean.TRUE.equals(dto.getEsDefinitivo())) {
            asamblea.setOrdenDiaDefinitivo(true);
            asambleaRepository.save(asamblea);
            log.info("[BACKEND ASAMBLEA] 📋 Orden del Día guardado como DEFINITIVO para asamblea ID {}", asamblea.getId());
        } else {
            log.info("[BACKEND ASAMBLEA] 📝 Orden del Día guardado como BORRADOR para asamblea ID {}", asamblea.getId());
        }

        return obtenerOrdenDia(asamblea.getId());
    }

    @Transactional
    public com.ph.backend.dto.PuntoOrdenDiaDto marcarPuntoOrdenDiaCompletado(Long puntoId, Boolean completado) {
        PuntoOrdenDia punto = puntoOrdenDiaRepository.findById(puntoId)
                .orElseThrow(() -> new IllegalArgumentException("Punto del orden del día no encontrado con ID: " + puntoId));

        punto.setCompletado(Boolean.TRUE.equals(completado));
        punto.setFechaCompletado(Boolean.TRUE.equals(completado) ? LocalDateTime.now() : null);

        PuntoOrdenDia guardado = puntoOrdenDiaRepository.save(punto);
        log.info("[BACKEND ASAMBLEA] 📋 Punto ID {} del Orden del Día marcado como completado: {}", puntoId, completado);

        return com.ph.backend.dto.PuntoOrdenDiaDto.builder()
                .id(guardado.getId())
                .orden(guardado.getOrden())
                .titulo(guardado.getTitulo())
                .descripcion(guardado.getDescripcion())
                .completado(guardado.getCompletado())
                .fechaCompletado(guardado.getFechaCompletado())
                .build();
    }

    @Transactional
    public Pregunta crearPregunta(com.ph.backend.dto.CrearPreguntaDto dto) {
        log.info("[BACKEND ASAMBLEA] ❓ Creando nueva pregunta para asamblea ID {}: '{}'", dto.getAsambleaId(),
                dto.getEnunciado());

        Asamblea asamblea = asambleaRepository.findById(dto.getAsambleaId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Asamblea no encontrada con ID: " + dto.getAsambleaId()));

        boolean tienePreguntaSinFinalizar = preguntaRepository.existsByAsambleaIdAndEstadoIn(
                asamblea.getId(),
                List.of(PreguntaStatus.PENDIENTE, PreguntaStatus.ABIERTA, PreguntaStatus.CERRADA));

        if (tienePreguntaSinFinalizar) {
            throw new IllegalStateException("No es posible crear una nueva pregunta.");
        }

        Pregunta pregunta = Pregunta.builder()
                .asamblea(asamblea)
                .enunciado(dto.getEnunciado().trim())
                .tipoMayoria(dto.getTipoMayoria() != null ? dto.getTipoMayoria() : PreguntaType.MAYORIA_SIMPLE)
                .duracionMinutos(
                        dto.getDuracionMinutos() != null && dto.getDuracionMinutos() > 0 ? dto.getDuracionMinutos() : 3)
                .estado(PreguntaStatus.PENDIENTE)
                .opciones(new java.util.ArrayList<>())
                .build();

        if (dto.getOpciones() != null && !dto.getOpciones().isEmpty()) {
            int orden = 1;
            for (String txt : dto.getOpciones()) {
                if (txt != null && !txt.isBlank()) {
                    pregunta.getOpciones().add(OpcionVoto.builder()
                            .pregunta(pregunta)
                            .texto(txt.trim())
                            .orden(orden++)
                            .build());
                }
            }
        } else {
            pregunta.getOpciones().add(OpcionVoto.builder().pregunta(pregunta).texto("Aprobar").orden(1).build());
            pregunta.getOpciones().add(OpcionVoto.builder().pregunta(pregunta).texto("Rechazar").orden(2).build());
            pregunta.getOpciones().add(OpcionVoto.builder().pregunta(pregunta).texto("Abstenerse").orden(3).build());
        }

        Pregunta guardada = preguntaRepository.save(pregunta);
        log.info("[BACKEND ASAMBLEA] ✅ Pregunta creada exitosamente con ID: {}", guardada.getId());
        return guardada;
    }

    @Transactional
    public AsistenciaAsamblea registrarAsistencia(AsistenciaRequestDto dto) {
        Asamblea asamblea = asambleaRepository.findById(dto.getAsambleaId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Asamblea no encontrada con ID: " + dto.getAsambleaId()));

        if (asamblea.getEstado() == AsambleaStatus.PROGRAMADA) {
            throw new IllegalStateException("La asamblea se encuentra en estado PROGRAMADA. El registro de asistencia solo estará disponible cuando el administrador active la asamblea (Pase a EN REGISTRO).");
        }

        if (Boolean.TRUE.equals(asamblea.getRegistroCerrado())) {
            throw new IllegalStateException("El registro de asistencia para esta asamblea ha sido CERRADO por la administración.");
        }

        UnidadPrivada unidad = unidadPrivadaRepository.findById(dto.getUnidadPrivadaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unidad Privada no encontrada con ID: " + dto.getUnidadPrivadaId()));

        Persona persona = personaRepository.findByCedula(dto.getCedulaPersona())
                .orElseGet(() -> {
                    log.info(
                            "[BACKEND ASAMBLEA] ⚠️ Persona no encontrada para la cédula: {}. Intentando autocreación desde Usuario...",
                            dto.getCedulaPersona());
                    Usuario u = usuarioRepository.findByDocumento(dto.getCedulaPersona()).orElse(null);
                    if (u != null) {
                        return personaRepository.save(Persona.builder()
                                .cedula(u.getDocumento())
                                .nombreCompleto(u.getNombreCompleto())
                                .email(u.getEmail())
                                .telefono("3000000000")
                                .build());
                    }
                    throw new IllegalArgumentException(
                            "Persona o Usuario no encontrado con cédula: " + dto.getCedulaPersona());
                });

        if (asistenciaRepository.existsByAsambleaIdAndUnidadPrivadaId(asamblea.getId(), unidad.getId())) {
            log.info("[BACKEND ASAMBLEA] 🔄 Actualizando registro de asistencia previo para unidad {}-{}. Generando nuevo PIN...",
                    unidad.getTorre(), unidad.getNumeroUnidad());
            AsistenciaAsamblea existente = asistenciaRepository.findByAsambleaIdAndUnidadPrivadaId(asamblea.getId(), unidad.getId()).get();
            
            String pinGenerado = String.format("%06d", new java.util.Random().nextInt(999999));
            existente.setPinAsamblea(pinGenerado);
            existente.setAsistenciaConfirmada(false);
            existente.setPersona(persona);
            existente.setEsApoderado(dto.getEsApoderado() != null && dto.getEsApoderado());
            
            AsistenciaAsamblea guardada = asistenciaRepository.save(existente);
            if (persona.getEmail() != null && !persona.getEmail().isBlank()) {
                Long copropiedadId = asamblea.getCopropiedad().getId();
                notificacionService.encolarPinAsamblea(copropiedadId, persona.getEmail(), persona.getNombreCompleto(), pinGenerado);
            }
            return guardada;
        }

        // Generar PIN de 6 dígitos para la asamblea
        String pinGenerado = String.format("%06d", new java.util.Random().nextInt(999999));

        AsistenciaAsamblea asistencia = AsistenciaAsamblea.builder()
                .asamblea(asamblea)
                .unidadPrivada(unidad)
                .persona(persona)
                .fechaHoraIngreso(LocalDateTime.now())
                .esApoderado(dto.getEsApoderado() != null && dto.getEsApoderado())
                .pinAsamblea(pinGenerado)
                .asistenciaConfirmada(false)
                .build();

        AsistenciaAsamblea guardada = asistenciaRepository.save(asistencia);

        // Encolar correo automático en la tabla notificaciones_email para despacho en segundo plano
        if (persona.getEmail() != null && !persona.getEmail().isBlank()) {
            Long copropiedadId = asamblea.getCopropiedad().getId();
            notificacionService.encolarPinAsamblea(copropiedadId, persona.getEmail(), persona.getNombreCompleto(), pinGenerado);
        }

        log.info(
                "[BACKEND ASAMBLEA] 🎟️ Asistencia presencial registrada (Estado: PENDIENTE_PIN). PIN generado: {} enviado a {}",
                pinGenerado, persona.getEmail());

        return guardada;
    }

    @Transactional
    public AsistenciaAsamblea confirmarPinAsamblea(ConfirmarPinAsambleaDto dto) {
        log.info("[BACKEND ASAMBLEA] 🔑 Validando PIN para unidad ID: {} | Persona: {}", dto.getUnidadPrivadaId(),
                dto.getCedulaPersona());

        AsistenciaAsamblea asistencia = asistenciaRepository
                .findByAsambleaIdAndUnidadPrivadaId(dto.getAsambleaId(), dto.getUnidadPrivadaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró registro de asistencia presencial previo para esta unidad."));

        if (Boolean.TRUE.equals(asistencia.getAsistenciaConfirmada())) {
            log.info("[BACKEND ASAMBLEA] ✅ Asistencia ya había sido confirmada anteriormente.");
            return asistencia;
        }

        // Permitir PIN demo "849201" o verificar PIN exacto
        boolean esPinDemo = "849201".equals(dto.getPin().trim());
        boolean esPinValido = asistencia.getPinAsamblea() != null
                && asistencia.getPinAsamblea().equals(dto.getPin().trim());

        if (!esPinDemo && !esPinValido) {
            throw new IllegalArgumentException(
                    "El PIN de acceso ingresado es incorrecto. Verifica el correo enviado a tu bandeja.");
        }

        asistencia.setAsistenciaConfirmada(true);
        AsistenciaAsamblea guardada = asistenciaRepository.save(asistencia);

        // Recalcular y emitir evento de quórum actualizado en RabbitMQ
        quorumService.recalcularYNotificarQuorum(dto.getAsambleaId());
        log.info("[BACKEND ASAMBLEA] 🎉 PIN verificado exitosamente. Asistencia y Quórum CONFIRMADOS.");

        return guardada;
    }

    @Transactional
    public AuthResponseDto autenticarCopropietario(IngresoCopropietarioDto dto) {
        Persona persona = personaRepository.findByCedula(dto.getCedula().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún propietario o apoderado registrado con la cédula: " + dto.getCedula()));

        UnidadPrivada unidad = unidadPrivadaRepository
                .findByTorreAndNumeroUnidad(dto.getTorre().trim(), dto.getNumeroUnidad().trim())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la unidad inmueble Torre "
                        + dto.getTorre() + " Apt/Local " + dto.getNumeroUnidad()));

        // Buscar asamblea en curso, en registro o programada
        Asamblea asamblea = asambleaRepository
                .findFirstByCopropiedadIdAndEstadoOrderByFechaDesc(unidad.getCopropiedad().getId(),
                        AsambleaStatus.EN_CURSO)
                .orElseGet(() -> asambleaRepository
                        .findFirstByCopropiedadIdAndEstadoOrderByFechaDesc(unidad.getCopropiedad().getId(),
                                AsambleaStatus.EN_REGISTRO)
                        .orElseGet(() -> asambleaRepository
                                .findFirstByCopropiedadIdAndEstadoOrderByFechaDesc(unidad.getCopropiedad().getId(),
                                        AsambleaStatus.PROGRAMADA)
                                .orElseThrow(() -> new IllegalStateException(
                                        "No hay asambleas activas en este momento para la copropiedad."))));

        if (asamblea.getEstado() == AsambleaStatus.PROGRAMADA) {
            throw new IllegalStateException("La asamblea '" + asamblea.getTitulo() + "' está en estado PROGRAMADA. El ingreso y registro de asistencia estará disponible una vez el Administrador cambie su estado a EN REGISTRO.");
        }

        // Registrar asistencia automáticamente al ingresar el copropietario cuando esté EN REGISTRO o EN CURSO
        boolean yaAsistio = asistenciaRepository.existsByAsambleaIdAndUnidadPrivadaId(asamblea.getId(), unidad.getId());
        if (!yaAsistio) {
            AsistenciaRequestDto astDto = AsistenciaRequestDto.builder()
                    .asambleaId(asamblea.getId())
                    .unidadPrivadaId(unidad.getId())
                    .cedulaPersona(persona.getCedula())
                    .esApoderado(false)
                    .build();
            registrarAsistencia(astDto);
        }

        return AuthResponseDto.builder()
                .personaId(persona.getId())
                .unidadPrivadaId(unidad.getId())
                .nombreCompleto(persona.getNombreCompleto())
                .torre(unidad.getTorre())
                .numeroUnidad(unidad.getNumeroUnidad())
                .coeficiente(unidad.getCoeficiente())
                .asambleaActivaId(asamblea.getId())
                .tituloAsamblea(asamblea.getTitulo())
                .asistenciaRegistrada(true)
                .build();
    }

    @Transactional
    public Pregunta cambiarEstadoPregunta(Long preguntaId, PreguntaStatus nuevoEstado) {
        return cambiarEstadoPregunta(preguntaId, nuevoEstado, null);
    }

    @Transactional
    public Pregunta cambiarEstadoPregunta(Long preguntaId, PreguntaStatus nuevoEstado, Integer duracionMinutosCustom) {
        Pregunta pregunta = preguntaRepository.findById(preguntaId)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada con ID: " + preguntaId));

        if (pregunta.getEstado() == PreguntaStatus.FINALIZADA) {
            throw new IllegalStateException("No se puede modificar una pregunta que ya ha sido FINALIZADA.");
        }

        if (nuevoEstado == PreguntaStatus.ANULADA) {
            votoEmitidoRepository.deleteByPreguntaId(preguntaId);
            pregunta.setEstado(PreguntaStatus.ANULADA);
            log.info("[BACKEND ASAMBLEA] 🚫 Pregunta ID {} ha sido ANULADA. Votos eliminados.", preguntaId);
            return preguntaRepository.save(pregunta);
        }

        if (nuevoEstado == PreguntaStatus.FINALIZADA) {
            if (pregunta.getEstado() != PreguntaStatus.CERRADA) {
                throw new IllegalStateException("Solo se puede finalizar una pregunta que se encuentre CERRADA.");
            }
            pregunta.setEstado(PreguntaStatus.FINALIZADA);
            log.info("[BACKEND ASAMBLEA] 🏁 Pregunta ID {} ha sido FINALIZADA definitivamente.", preguntaId);
            return preguntaRepository.save(pregunta);
        }

        if (nuevoEstado == PreguntaStatus.ABIERTA) {
            if (pregunta.getEstado() == PreguntaStatus.ANULADA) {
                throw new IllegalStateException("No se puede abrir una pregunta que ha sido ANULADA.");
            }
            LocalDateTime ahora = LocalDateTime.now();
            pregunta.setFechaHoraApertura(ahora);
            int mins = (duracionMinutosCustom != null && duracionMinutosCustom > 0)
                    ? duracionMinutosCustom
                    : ((pregunta.getDuracionMinutos() != null && pregunta.getDuracionMinutos() > 0)
                            ? pregunta.getDuracionMinutos()
                            : 3);
            pregunta.setDuracionMinutos(mins);
            pregunta.setFechaHoraCierre(ahora.plusMinutes(mins));
            pregunta.setEstado(PreguntaStatus.ABIERTA);
            log.info(
                    "[BACKEND ASAMBLEA] ⏱️ Votación ABIERTA/REABIERTA para pregunta ID {}. Cierre programado en {} minutos ({})",
                    preguntaId, mins, pregunta.getFechaHoraCierre());
        } else if (nuevoEstado == PreguntaStatus.CERRADA) {
            pregunta.setFechaHoraCierre(LocalDateTime.now());
            pregunta.setEstado(PreguntaStatus.CERRADA);
            log.info("[BACKEND ASAMBLEA] ⏹️ Votación CERRADA para pregunta ID {}", preguntaId);
        }

        Pregunta actualizada = preguntaRepository.save(pregunta);
        return actualizada;
    }

    @Transactional
    public Pregunta editarPregunta(Long preguntaId, com.ph.backend.dto.CrearPreguntaDto dto) {
        Pregunta pregunta = preguntaRepository.findById(preguntaId)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada con ID: " + preguntaId));

        if (pregunta.getEstado() != PreguntaStatus.PENDIENTE) {
            throw new IllegalStateException(
                    "Solo se puede editar una pregunta antes de iniciar la votación (en estado PENDIENTE). Estado actual: "
                            + pregunta.getEstado());
        }

        pregunta.setEnunciado(dto.getEnunciado().trim());
        pregunta.setTipoMayoria(dto.getTipoMayoria() != null ? dto.getTipoMayoria() : PreguntaType.MAYORIA_SIMPLE);
        pregunta.setDuracionMinutos(
                dto.getDuracionMinutos() != null && dto.getDuracionMinutos() > 0 ? dto.getDuracionMinutos() : 5);

        if (dto.getOpciones() != null && !dto.getOpciones().isEmpty()) {
            pregunta.getOpciones().clear();
            int orden = 1;
            for (String txt : dto.getOpciones()) {
                if (txt != null && !txt.isBlank()) {
                    pregunta.getOpciones().add(OpcionVoto.builder()
                            .pregunta(pregunta)
                            .texto(txt.trim())
                            .orden(orden++)
                            .build());
                }
            }
        }

        log.info("[BACKEND ASAMBLEA] ✏️ Pregunta ID {} editada exitosamente antes de iniciar.", preguntaId);
        return preguntaRepository.save(pregunta);
    }

    @Transactional
    public List<Pregunta> obtenerPreguntasPorAsamblea(Long asambleaId) {
        List<Pregunta> lista = preguntaRepository.findByAsambleaIdOrderByIdDesc(asambleaId);
        LocalDateTime ahora = LocalDateTime.now();
        for (Pregunta p : lista) {
            if (p.getEstado() == PreguntaStatus.ABIERTA && p.getFechaHoraCierre() != null
                    && ahora.isAfter(p.getFechaHoraCierre())) {
                p.setEstado(PreguntaStatus.CERRADA);
                preguntaRepository.save(p);
                log.info("[BACKEND ASAMBLEA] ⌛ Pregunta ID {} cerrada automáticamente por fin del tiempo de votación.",
                        p.getId());
            }
        }
        return lista;
    }

    @Transactional(readOnly = true)
    public List<AsistenciaAsamblea> obtenerAsistentes(Long asambleaId) {
        return asistenciaRepository.findByAsambleaId(asambleaId);
    }

    @Transactional
    public Asamblea cambiarEstadoRegistro(Long asambleaId, boolean cerrado) {
        Asamblea asamblea = asambleaRepository.findById(asambleaId)
                .orElseThrow(() -> new IllegalArgumentException("Asamblea no encontrada con ID: " + asambleaId));
        asamblea.setRegistroCerrado(cerrado);
        log.info("[BACKEND ASAMBLEA] 🔒 Estado de registro de asistencia actualizado a cerrado={} para Asamblea ID {}", cerrado, asambleaId);
        return asambleaRepository.save(asamblea);
    }
}
