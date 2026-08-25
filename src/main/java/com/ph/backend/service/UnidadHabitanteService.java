package com.ph.backend.service;

import com.ph.backend.dto.HabitanteRequestDTO;
import com.ph.backend.model.Copropiedad;
import com.ph.backend.model.Persona;
import com.ph.backend.model.Rol;
import com.ph.backend.model.UnidadHabitante;
import com.ph.backend.model.UnidadPrivada;
import com.ph.backend.model.Usuario;
import com.ph.backend.repository.PersonaRepository;
import com.ph.backend.repository.UnidadHabitanteRepository;
import com.ph.backend.repository.UnidadPrivadaRepository;
import com.ph.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnidadHabitanteService {

    private final UnidadHabitanteRepository habitanteRepository;
    private final UnidadPrivadaRepository unidadRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final UsernameService usernameService;
    private final PasswordGeneratorService passwordGeneratorService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public List<UnidadHabitante> obtenerHabitantesPorUnidad(Long unidadId) {
        UnidadPrivada unidad = unidadRepository.findById(unidadId).orElse(null);
        List<UnidadHabitante> habitantes = habitanteRepository.findByUnidadId(unidadId);

        if (unidad != null && unidad.getPropietario() != null) {
            Persona propietario = unidad.getPropietario();
            boolean yaExiste = habitantes.stream()
                    .anyMatch(h -> h.getPersona() != null && h.getPersona().getId().equals(propietario.getId()));

            if (!yaExiste) {
                try {
                    UnidadHabitante nuevoHabitante = habitanteRepository.save(UnidadHabitante.builder()
                            .unidad(unidad)
                            .persona(propietario)
                            .esPropietarioPrincipal(true)
                            .esArrendatario(false)
                            .esApoderadoDesignado(false)
                            .parentesco("PROPIETARIO")
                            .build());
                    habitantes.add(nuevoHabitante);
                } catch (Exception e) {
                    log.warn("Observación al vincular propietario como habitante: {}", e.getMessage());
                }
            }
        }

        return habitantes;
    }

    @Transactional
    public UnidadHabitante agregarOActualizarHabitante(Long unidadId, HabitanteRequestDTO dto) {
        if (dto.getCedula() == null || dto.getCedula().trim().isEmpty()) {
            throw new IllegalArgumentException("La identificación / cédula es requerida.");
        }
        if (dto.getNombreCompleto() == null || dto.getNombreCompleto().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre completo es requerido.");
        }

        UnidadPrivada unidad = unidadRepository.findById(unidadId)
                .orElseThrow(() -> new IllegalArgumentException("Unidad privada no encontrada con id: " + unidadId));

        String cedulaClean = dto.getCedula().trim();
        Persona personaInit = personaRepository.findByCedula(cedulaClean)
                .orElseGet(() -> Persona.builder()
                        .cedula(cedulaClean)
                        .nombreCompleto(dto.getNombreCompleto().trim())
                        .email(dto.getEmail() != null ? dto.getEmail().trim() : null)
                        .telefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null)
                        .build());

        // Actualizar datos de la persona si enviaron email/teléfono
        personaInit.setNombreCompleto(dto.getNombreCompleto().trim());
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            personaInit.setEmail(dto.getEmail().trim());
        }
        if (dto.getTelefono() != null && !dto.getTelefono().trim().isEmpty()) {
            personaInit.setTelefono(dto.getTelefono().trim());
        }
        final Persona persona = personaRepository.save(personaInit);

        // LÓGICA DE PROPIETARIO TITULAR
        if (Boolean.TRUE.equals(dto.getEsPropietarioPrincipal())) {
            Persona propietarioActual = unidad.getPropietario();
            UnidadHabitante habitantePropietarioActual = habitanteRepository
                    .findByUnidadIdAndEsPropietarioPrincipalTrue(unidadId).orElse(null);

            Persona otroPropietario = null;
            if (propietarioActual != null && !propietarioActual.getId().equals(persona.getId())) {
                otroPropietario = propietarioActual;
            } else if (habitantePropietarioActual != null && habitantePropietarioActual.getPersona() != null
                    && !habitantePropietarioActual.getPersona().getId().equals(persona.getId())) {
                otroPropietario = habitantePropietarioActual.getPersona();
            }

            if (otroPropietario != null) {
                // Hay un propietario anterior registrado diferente
                boolean confirmadoProp = Boolean.TRUE.equals(dto.getConfirmarCambioPropietario())
                        || Boolean.TRUE.equals(dto.getConfirmarCambioApoderado())
                        || Boolean.TRUE.equals(dto.getConfirmarCambio());
                if (!confirmadoProp) {
                    String msg = String.format(
                            "REQUIERE_CONFIRMACION: La unidad Torre %s - Apt %s ya cuenta con el Propietario Titular '%s' (Doc: %s).\n\n"
                                    +
                                    "Al proceder con la confirmación:\n" +
                                    "• Se desmarcará a %s como propietario titular.\n" +
                                    "• Se INACTIVARÁ su usuario de acceso al sistema.\n" +
                                    "• Se asignará a '%s' como nuevo Propietario Titular y se activará/creará su usuario de acceso.\n\n"
                                    +
                                    "¿Confirmas que deseas transferir la propiedad titular?",
                            unidad.getTorre(),
                            unidad.getNumeroUnidad(),
                            otroPropietario.getNombreCompleto(),
                            otroPropietario.getCedula(),
                            otroPropietario.getNombreCompleto(),
                            persona.getNombreCompleto());
                    throw new IllegalArgumentException(msg);
                }
            }

            // GARANTÍA: Inactivar el acceso de cualquier otro usuario/habitante de esta
            // unidad previa
            List<UnidadHabitante> habitantesUnidad = habitanteRepository.findByUnidadId(unidadId);
            for (UnidadHabitante h : habitantesUnidad) {
                if (h.getPersona() != null && !h.getPersona().getId().equals(persona.getId())) {
                    if (Boolean.TRUE.equals(h.getEsPropietarioPrincipal())) {
                        h.setEsPropietarioPrincipal(false);
                        habitanteRepository.save(h);
                    }
                    usuarioRepository.findByDocumento(h.getPersona().getCedula()).ifPresent(usr -> {
                        if (Boolean.TRUE.equals(usr.getActivo())) {
                            usr.setActivo(false);
                            usuarioRepository.save(usr);
                        }
                    });
                }
            }

            if (propietarioActual != null && !propietarioActual.getId().equals(persona.getId())) {
                usuarioRepository.findByDocumento(propietarioActual.getCedula()).ifPresent(usr -> {
                    if (Boolean.TRUE.equals(usr.getActivo())) {
                        usr.setActivo(false);
                        usuarioRepository.save(usr);
                    }
                });
            }

            // 3. Asignar nuevo propietario a la unidad
            unidad.setPropietario(persona);
            unidadRepository.save(unidad);

            // 4. Crear o Activar usuario (sin reenviar correo si ya existía usuario previo)
            crearOActivarUsuarioYNotificar(unidad, persona);
        }

        // LÓGICA DE APODERADO DESIGNADO POR UNIDAD
        if (Boolean.TRUE.equals(dto.getEsApoderadoDesignado())) {
            List<UnidadHabitante> apoderadosPrevios = habitanteRepository.findByUnidadIdAndEsApoderadoDesignadoTrue(unidadId);

            UnidadHabitante otroApoderadoHabitante = apoderadosPrevios.stream()
                    .filter(h -> h.getPersona() != null && !h.getPersona().getId().equals(persona.getId()))
                    .findFirst().orElse(null);

            Persona otroApoderadoPersona = otroApoderadoHabitante != null ? otroApoderadoHabitante.getPersona()
                    : (unidad.getApoderado() != null && unidad.getApoderado().getPersona() != null && !unidad.getApoderado().getPersona().getId().equals(persona.getId())
                    ? unidad.getApoderado().getPersona() : null);

            if (otroApoderadoPersona != null) {
                boolean confirmadoApod = Boolean.TRUE.equals(dto.getConfirmarCambioApoderado())
                        || Boolean.TRUE.equals(dto.getConfirmarCambioPropietario())
                        || Boolean.TRUE.equals(dto.getConfirmarCambio());
                if (!confirmadoApod) {
                    String msg = String.format(
                            "REQUIERE_CONFIRMACION_APODERADO: La unidad Torre %s - Apt %s ya cuenta con el Apoderado registrado '%s' (Doc: %s).\n\n"
                                    + "Al proceder con la confirmación:\n"
                                    + "• Se desmarcará a %s como apoderado oficial.\n"
                                    + "• Se desarmará su poder en esta unidad.\n"
                                    + "• Se asignará a '%s' como nuevo Apoderado Oficial y se activará su usuario de acceso.\n\n"
                                    + "¿Confirmas que deseas reemplazar el apoderado actual?",
                            unidad.getTorre(),
                            unidad.getNumeroUnidad(),
                            otroApoderadoPersona.getNombreCompleto(),
                            otroApoderadoPersona.getCedula(),
                            otroApoderadoPersona.getNombreCompleto(),
                            persona.getNombreCompleto());
                    throw new IllegalArgumentException(msg);
                }

                // Inactivar/desmarcar el apoderado previo
                for (UnidadHabitante h : apoderadosPrevios) {
                    if (h.getPersona() != null && !h.getPersona().getId().equals(persona.getId())) {
                        h.setEsApoderadoDesignado(false);
                        habitanteRepository.save(h);
                        usuarioRepository.findByDocumento(h.getPersona().getCedula()).ifPresent(usr -> {
                            usr.setActivo(false);
                            usuarioRepository.save(usr);
                        });
                    }
                }
                unidad.setApoderado(null);
                unidad.setPoderAprobado(false);
                unidadRepository.save(unidad);
            }

            // Crear o Activar usuario con rol ROLE_APODERADO y asociarlo a la unidad
            Usuario usuarioApoderado = obtenerOCrearUsuarioApoderado(unidad, persona);
            unidad.setApoderado(usuarioApoderado);
            unidad.setPoderAprobado(true);
            unidadRepository.save(unidad);
        }

        // Buscar relación existente de habitante o crear nueva
        final Persona personaFinal = persona;
        UnidadHabitante habitante = habitanteRepository.findByUnidadIdAndPersonaId(unidadId, persona.getId())
                .orElseGet(() -> UnidadHabitante.builder()
                        .unidad(unidad)
                        .persona(personaFinal)
                        .build());

        habitante.setEsPropietarioPrincipal(Boolean.TRUE.equals(dto.getEsPropietarioPrincipal()));
        habitante.setEsArrendatario(Boolean.TRUE.equals(dto.getEsArrendatario()));
        habitante.setEsApoderadoDesignado(Boolean.TRUE.equals(dto.getEsApoderadoDesignado()));
        habitante.setParentesco(dto.getParentesco() != null ? dto.getParentesco().trim() : "HABITANTE");

        UnidadHabitante guardado = habitanteRepository.save(habitante);
        return guardado;
    }

    private void crearOActivarUsuarioYNotificar(UnidadPrivada unidad, Persona persona) {
        Long copId = unidad.getCopropiedad() != null ? unidad.getCopropiedad().getId() : 1L;
        Usuario usuario = usuarioRepository.findByDocumento(persona.getCedula()).orElse(null);

        String usernameGen;
        String tempPass = passwordGeneratorService.generarPasswordSegura();

        if (usuario != null) {
            // Usuario ya existía: SE ACTIVA Y SE ASIGNA ROL, PERO NO SE ENVÍA NOTIFICACIÓN
            // POR CORREO
            usuario.setActivo(true);
            if (usuario.getRoles() == null) {
                usuario.setRoles(new HashSet<>());
            }
            usuario.getRoles().add(Rol.ROLE_COPROPIETARIO);
            if (usuario.getCopropiedadesAsignadas() == null) {
                usuario.setCopropiedadesAsignadas(new HashSet<>());
            }
            if (unidad.getCopropiedad() != null) {
                usuario.getCopropiedadesAsignadas().add(unidad.getCopropiedad());
            }
            usuarioRepository.save(usuario);
            usernameGen = usuario.getUsername();
        } else {
            // No existía usuario: SE CREA NUEVO USUARIO DE ACCESO Y SE ENVÍA NOTIFICACIÓN
            // DE CREDENCIALES
            usernameGen = usernameService.generarUsernameUnico(persona.getNombreCompleto());
            Set<Copropiedad> copropiedades = new HashSet<>();
            if (unidad.getCopropiedad() != null) {
                copropiedades.add(unidad.getCopropiedad());
            }

            usuario = Usuario.builder()
                    .persona(persona)
                    .username(usernameGen)
                    .password(passwordEncoder.encode(tempPass))
                    .roles(new HashSet<>(List.of(Rol.ROLE_COPROPIETARIO)))
                    .copropiedadesAsignadas(copropiedades)
                    .debeCambiarPassword(true)
                    .activo(true)
                    .build();

            usuarioRepository.save(usuario);

            // Registrar la notificación de credenciales iniciales ÚNICAMENTE cuando se crea
            // una cuenta nueva
            String emailDestino = (persona.getEmail() != null && !persona.getEmail().isBlank())
                    ? persona.getEmail().trim()
                    : "propietario." + persona.getCedula() + "@proyectoph.com";

            notificacionService.encolarCredencialesIniciales(
                    copId,
                    emailDestino,
                    persona.getNombreCompleto(),
                    usernameGen,
                    tempPass,
                    "Acceso Propietario (Torre " + unidad.getTorre() + " - Apt " + unidad.getNumeroUnidad() + ")");
        }
    }

    private Usuario obtenerOCrearUsuarioApoderado(UnidadPrivada unidad, Persona persona) {
        Long copId = unidad.getCopropiedad() != null ? unidad.getCopropiedad().getId() : 1L;
        Usuario usuario = usuarioRepository.findByDocumento(persona.getCedula()).orElse(null);

        if (usuario == null) {
            String usernameGen = usernameService.generarUsernameUnico(persona.getNombreCompleto());
            String tempPass = passwordGeneratorService.generarPasswordSegura();
            usuario = usuarioRepository.save(Usuario.builder()
                    .persona(persona)
                    .username(usernameGen)
                    .password(passwordEncoder.encode(tempPass))
                    .roles(new HashSet<>(Set.of(Rol.ROLE_APODERADO)))
                    .copropiedadesAsignadas(new HashSet<>(Set.of(unidad.getCopropiedad())))
                    .debeCambiarPassword(true)
                    .activo(true)
                    .build());

            if (persona.getEmail() != null && !persona.getEmail().isBlank()) {
                notificacionService.encolarCredencialesIniciales(
                        copId,
                        persona.getEmail(),
                        persona.getNombreCompleto(),
                        usernameGen,
                        tempPass,
                        "Acceso Apoderado de Unidad - " + unidad.getCopropiedad().getNombre()
                );
            }
        } else {
            usuario.setActivo(true);
            if (usuario.getRoles() == null) {
                usuario.setRoles(new HashSet<>());
            }
            usuario.getRoles().add(Rol.ROLE_APODERADO);
            if (usuario.getCopropiedadesAsignadas() == null) {
                usuario.setCopropiedadesAsignadas(new HashSet<>());
            }
            usuario.getCopropiedadesAsignadas().add(unidad.getCopropiedad());
            usuario = usuarioRepository.save(usuario);
        }
        return usuario;
    }

    @Transactional
    public void cambiarDesignacionApoderado(Long unidadId, Long habitanteId, boolean esApoderado, Boolean confirmarCambio) {
        UnidadHabitante habitante = habitanteRepository.findById(habitanteId)
                .orElseThrow(() -> new IllegalArgumentException("Habitante no encontrado con id: " + habitanteId));

        UnidadPrivada unidad = habitante.getUnidad();
        if (!unidad.getId().equals(unidadId)) {
            throw new IllegalArgumentException("El habitante no pertenece a esta unidad privada.");
        }

        Persona persona = habitante.getPersona();

        if (esApoderado) {
            List<UnidadHabitante> apoderadosPrevios = habitanteRepository.findByUnidadIdAndEsApoderadoDesignadoTrue(unidadId);
            UnidadHabitante otroApoderadoHabitante = apoderadosPrevios.stream()
                    .filter(h -> h.getPersona() != null && !h.getPersona().getId().equals(persona.getId()))
                    .findFirst().orElse(null);

            Persona otroApoderadoPersona = otroApoderadoHabitante != null ? otroApoderadoHabitante.getPersona()
                    : (unidad.getApoderado() != null && unidad.getApoderado().getPersona() != null && !unidad.getApoderado().getPersona().getId().equals(persona.getId())
                    ? unidad.getApoderado().getPersona() : null);

            if (otroApoderadoPersona != null) {
                if (!Boolean.TRUE.equals(confirmarCambio)) {
                    String msg = String.format(
                            "REQUIERE_CONFIRMACION_APODERADO: La unidad Torre %s - Apt %s ya cuenta con el Apoderado registrado '%s' (Doc: %s).\n\n"
                                    + "Al proceder con la confirmación:\n"
                                    + "• Se desmarcará a %s como apoderado oficial.\n"
                                    + "• Se desarmará su poder en esta unidad.\n"
                                    + "• Se asignará a '%s' como nuevo Apoderado Oficial y se activará su usuario de acceso.\n\n"
                                    + "¿Confirmas que deseas reemplazar el apoderado actual?",
                            unidad.getTorre(),
                            unidad.getNumeroUnidad(),
                            otroApoderadoPersona.getNombreCompleto(),
                            otroApoderadoPersona.getCedula(),
                            otroApoderadoPersona.getNombreCompleto(),
                            persona.getNombreCompleto());
                    throw new IllegalArgumentException(msg);
                }

                for (UnidadHabitante h : apoderadosPrevios) {
                    if (h.getPersona() != null && !h.getPersona().getId().equals(persona.getId())) {
                        h.setEsApoderadoDesignado(false);
                        habitanteRepository.save(h);
                        usuarioRepository.findByDocumento(h.getPersona().getCedula()).ifPresent(usr -> {
                            usr.setActivo(false);
                            usuarioRepository.save(usr);
                        });
                    }
                }
                unidad.setApoderado(null);
                unidad.setPoderAprobado(false);
                unidadRepository.save(unidad);
            }

            habitante.setEsApoderadoDesignado(true);
            habitanteRepository.save(habitante);

            Usuario usuarioApoderado = obtenerOCrearUsuarioApoderado(unidad, persona);
            unidad.setApoderado(usuarioApoderado);
            unidad.setPoderAprobado(true);
            unidadRepository.save(unidad);
        } else {
            habitante.setEsApoderadoDesignado(false);
            habitanteRepository.save(habitante);
            if (unidad.getApoderado() != null && unidad.getApoderado().getPersona() != null && unidad.getApoderado().getPersona().getId().equals(persona.getId())) {
                unidad.setApoderado(null);
                unidad.setPoderAprobado(false);
                unidadRepository.save(unidad);
            }
        }
    }

    @Transactional
    public void eliminarHabitante(Long unidadId, Long habitanteId) {
        UnidadHabitante habitante = habitanteRepository.findById(habitanteId)
                .orElseThrow(() -> new IllegalArgumentException("Habitante no encontrado"));
        if (!habitante.getUnidad().getId().equals(unidadId)) {
            throw new IllegalArgumentException("El habitante no pertenece a esta unidad.");
        }
        habitanteRepository.delete(habitante);
    }
}
