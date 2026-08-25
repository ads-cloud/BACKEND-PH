package com.ph.backend.controller;

import com.ph.backend.dto.CrearGestorDto;
import com.ph.backend.model.Copropiedad;
import com.ph.backend.model.Persona;
import com.ph.backend.model.Rol;
import com.ph.backend.model.Usuario;
import com.ph.backend.repository.CopropiedadRepository;
import com.ph.backend.repository.PersonaRepository;
import com.ph.backend.repository.UsuarioRepository;
import com.ph.backend.service.NotificacionService;
import com.ph.backend.service.PasswordGeneratorService;
import com.ph.backend.service.UsernameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping({ "/api/v1/gestores", "/gestores" })
@RequiredArgsConstructor
@Slf4j
public class GestorController {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final CopropiedadRepository copropiedadRepository;
    private final UsernameService usernameService;
    private final PasswordGeneratorService passwordGeneratorService;
    private final NotificacionService notificacionService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<List<Usuario>> listarGestores(
            @RequestParam(value = "copropiedadId", required = false) Long copropiedadId) {
        Long copId = copropiedadId != null ? copropiedadId : com.ph.backend.config.tenant.TenantContext.getCurrentTenant();
        List<Usuario> gestores = usuarioRepository.findGestoresByCopropiedadId(copId);
        return ResponseEntity.ok(gestores);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Usuario> crearGestor(@RequestBody CrearGestorDto dto) {
        Long copId = dto.getCopropiedadId() != null ? dto.getCopropiedadId()
                : com.ph.backend.config.tenant.TenantContext.getCurrentTenant();

        if (copId == null) {
            throw new IllegalArgumentException("La copropiedad es obligatoria para asociar el gestor.");
        }

        Copropiedad copropiedad = copropiedadRepository.findById(copId)
                .orElseThrow(() -> new IllegalArgumentException("Copropiedad no encontrada con ID: " + copId));

        String cedula = dto.getCedula().trim();
        String email = dto.getEmail() != null ? dto.getEmail().trim() : null;
        String nombre = dto.getNombreCompleto().trim();

        // 1. Buscar si la cuenta de usuario ya existe por documento
        Usuario usuarioExistente = usuarioRepository.findByDocumento(cedula).orElse(null);

        if (usuarioExistente != null) {
            // Regla 1: Si la cédula pertenece a un Propietario/Copropietario, NO se puede registrar como Gestor
            if (usuarioExistente.getRoles() != null && usuarioExistente.getRoles().contains(Rol.ROLE_COPROPIETARIO)) {
                throw new IllegalArgumentException(
                        "El usuario con la cédula '" + cedula + "' ya está registrado como propietario/copropietario y no se puede registrar como gestor.");
            }

            boolean estaEnEstaCopropiedad = usuarioExistente.getCopropiedadesAsignadas() != null &&
                    usuarioExistente.getCopropiedadesAsignadas().contains(copropiedad);
            boolean esGestor = usuarioExistente.getRoles() != null && usuarioExistente.getRoles().contains(Rol.ROLE_GESTOR);

            // Regla 2: Si la cédula ya está registrada como GESTOR en la MISMA PH
            if (estaEnEstaCopropiedad && esGestor) {
                throw new IllegalArgumentException(
                        "El usuario con la cédula '" + cedula + "' ya se encuentra registrado como gestor en esta copropiedad. Debe recuperar su acceso mediante la opción de recuperación de contraseña.");
            }

            // Regla 3: Si existe pero NO está asociada a esta misma PH, se reutiliza la persona y usuario asociándole la nueva PH
            if (usuarioExistente.getCopropiedadesAsignadas() == null) {
                usuarioExistente.setCopropiedadesAsignadas(new HashSet<>());
            }
            usuarioExistente.getCopropiedadesAsignadas().add(copropiedad);
            if (usuarioExistente.getRoles() == null) {
                usuarioExistente.setRoles(new HashSet<>());
            }
            usuarioExistente.getRoles().add(Rol.ROLE_GESTOR);

            // NOTA: NO se actualizan los datos de la Persona (Regla: no actualizar datos de la cédula encontrada)
            Usuario guardado = usuarioRepository.save(usuarioExistente);

            String emailDestino = email != null && !email.isBlank()
                    ? email
                    : (guardado.getPersona() != null ? guardado.getPersona().getEmail() : null);

            if (emailDestino != null && !emailDestino.isBlank()) {
                notificacionService.encolarCredencialesIniciales(
                        copId,
                        emailDestino,
                        guardado.getNombreCompleto(),
                        guardado.getUsername(),
                        "Utilice su contraseña actual o recupérela desde la opción de restablecimiento de contraseña",
                        "Asignación de rol Gestor - " + copropiedad.getNombre()
                );
            }

            return ResponseEntity.ok(guardado);
        }

        // 2. Si el usuario es TOTALMENTE NUEVO: buscar o crear la Persona (SIN actualizar sus datos si ya existía la Persona)
        Persona persona = personaRepository.findByCedula(cedula).orElse(null);
        if (persona == null) {
            persona = personaRepository.save(Persona.builder()
                    .cedula(cedula)
                    .nombreCompleto(nombre)
                    .email(email)
                    .telefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null)
                    .build());
        }

        // 3. Crear nuevo Usuario Gestor
        String rawPassword = passwordGeneratorService.generarPasswordSegura();
        String passwordEncoded = passwordEncoder.encode(rawPassword);
        String usernameGenerated = usernameService.generarUsernameUnico(nombre);

        Usuario nuevoUsuario = Usuario.builder()
                .persona(persona)
                .username(usernameGenerated)
                .password(passwordEncoded)
                .debeCambiarPassword(true)
                .activo(true)
                .roles(new HashSet<>(Set.of(Rol.ROLE_GESTOR)))
                .copropiedadesAsignadas(new HashSet<>(Set.of(copropiedad)))
                .build();

        Usuario guardado = usuarioRepository.save(nuevoUsuario);

        // 4. Si es TOTALMENTE NUEVO, se registra la notificación en la BD (tabla notificaciones_email)
        // con la plantilla CREDENCIALES_INICIALES conteniendo su username y la contraseña aleatoria generada.
        if (email != null && !email.isBlank()) {
            notificacionService.encolarCredencialesIniciales(
                    copId,
                    email,
                    nombre,
                    guardado.getUsername(),
                    rawPassword,
                    "Acceso Gestor de Registro - " + copropiedad.getNombre()
            );
            log.info("📧 Notificación de credenciales iniciales para gestor '{}' guardada en BD en estado PENDIENTE.", guardado.getUsername());
        }

        return ResponseEntity.ok(guardado);
    }

    @PostMapping("/{usuarioId}/reset-password")
    @Transactional
    public ResponseEntity<Map<String, String>> resetearPasswordGestor(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario gestor no encontrado con ID: " + usuarioId));

        String rawPassword = passwordGeneratorService.generarPasswordSegura();
        usuario.setPassword(passwordEncoder.encode(rawPassword));
        usuario.setDebeCambiarPassword(true);
        usuarioRepository.save(usuario);

        String email = usuario.getPersona() != null ? usuario.getPersona().getEmail() : null;
        Long copId = com.ph.backend.config.tenant.TenantContext.getCurrentTenant();

        if (email != null && !email.isBlank()) {
            notificacionService.encolarCredencialesIniciales(
                    copId,
                    email,
                    usuario.getNombreCompleto(),
                    usuario.getUsername(),
                    rawPassword,
                    "Restablecimiento de Contraseña Gestor"
            );
        }

        return ResponseEntity.ok(Map.of(
                "message", "La contraseña del gestor ha sido restablecida exitosamente. Se ha enviado al correo del usuario."));
    }
}
