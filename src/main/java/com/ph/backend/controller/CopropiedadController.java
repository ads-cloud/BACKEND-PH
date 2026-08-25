package com.ph.backend.controller;

import com.ph.backend.dto.CrearAdminDto;
import com.ph.backend.dto.CopropiedadResponseDto;
import com.ph.backend.model.Copropiedad;
import com.ph.backend.model.Rol;
import com.ph.backend.model.Usuario;
import com.ph.backend.model.Persona;
import com.ph.backend.repository.CopropiedadRepository;
import com.ph.backend.repository.PersonaRepository;
import com.ph.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.ph.backend.service.UsernameService;

import java.util.HashSet;
import java.util.List;

@RestController
@RequestMapping("/api/v1/copropiedades")
@RequiredArgsConstructor
@Slf4j
public class CopropiedadController {

    private final CopropiedadRepository copropiedadRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final com.ph.backend.repository.UnidadPrivadaRepository unidadPrivadaRepository;
    private final com.ph.backend.repository.CopropiedadModuloRepository copropiedadModuloRepository;
    private final com.ph.backend.repository.ModuloRepository moduloRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsernameService usernameService;
    private final com.ph.backend.service.PasswordGeneratorService passwordGeneratorService;
    private final com.ph.backend.service.NotificacionService notificacionService;
    private final com.ph.backend.service.EmailService emailService;

    public static String formatearNit(String rawNit) {
        if (rawNit == null || rawNit.isBlank()) return "";
        String digits = rawNit.replaceAll("\\D+", "");
        if (digits.length() == 10) {
            return digits.replaceAll("^(\\d{3})(\\d{3})(\\d{3})(\\d{1})$", "$1.$2.$3-$4");
        } else if (digits.length() == 9) {
            return digits.replaceAll("^(\\d{3})(\\d{3})(\\d{3})$", "$1.$2.$3");
        }
        return digits;
    }

    @GetMapping
    public ResponseEntity<List<CopropiedadResponseDto>> listarCopropiedades() {
        List<Copropiedad> copropiedades = copropiedadRepository.findAll();
        List<Long> copIds = copropiedades.stream().map(Copropiedad::getId).toList();

        // 1. Bulk Fetches (Solo 4 consultas a la base de datos en total)
        List<Object[]> adminsData = copIds.isEmpty() ? List.of() : usuarioRepository.findAdminsByCopropiedadIdsIn(copIds);
        List<Object[]> countsData = copIds.isEmpty() ? List.of() : unidadPrivadaRepository.countByCopropiedadIdsIn(copIds);
        List<com.ph.backend.model.CopropiedadModulo> modulosData = copIds.isEmpty() ? List.of() : copropiedadModuloRepository.findByCopropiedadIdInAndActivoTrue(copIds);
        int totalModulosSistema = (int) moduloRepository.countByActivoTrue();

        // 2. Indexación en Memoria (O(1))
        java.util.Map<Long, List<Usuario>> adminsMap = new java.util.HashMap<>();
        for (Object[] row : adminsData) {
            Long copId = (Long) row[0];
            Usuario u = (Usuario) row[1];
            adminsMap.computeIfAbsent(copId, k -> new java.util.ArrayList<>()).add(u);
        }

        java.util.Map<Long, Integer> countsMap = new java.util.HashMap<>();
        for (Object[] row : countsData) {
            Long copId = (Long) row[0];
            Long count = (Long) row[1];
            countsMap.put(copId, count.intValue());
        }

        java.util.Map<Long, List<com.ph.backend.model.CopropiedadModulo>> modulosMap = new java.util.HashMap<>();
        for (com.ph.backend.model.CopropiedadModulo cm : modulosData) {
            Long copId = cm.getCopropiedad().getId();
            modulosMap.computeIfAbsent(copId, k -> new java.util.ArrayList<>()).add(cm);
        }

        // 3. Ensamblaje en Memoria (Sin consultas SQL)
        List<CopropiedadResponseDto> dtos = copropiedades.stream().map(cop -> {
            Long copId = cop.getId();
            List<Usuario> usuariosAdmins = adminsMap.getOrDefault(copId, List.of());
            List<CopropiedadResponseDto.AdminInfoDto> admins = usuariosAdmins.stream()
                    .map(u -> CopropiedadResponseDto.AdminInfoDto.builder()
                            .id(u.getId())
                            .documento(u.getDocumento())
                            .username(u.getUsername())
                            .nombreCompleto(u.getNombreCompleto())
                            .email(u.getEmail())
                            .telefono(u.getTelefono())
                            .build())
                    .toList();

            int totalUnidades = countsMap.getOrDefault(copId, 0);
            List<com.ph.backend.model.CopropiedadModulo> modulosPh = modulosMap.getOrDefault(copId, List.of());
            int totalModulos = modulosPh.isEmpty() ? totalModulosSistema : modulosPh.size();

            return CopropiedadResponseDto.builder()
                    .id(cop.getId())
                    .nombre(cop.getNombre())
                    .nit(formatearNit(cop.getNit()))
                    .direccion(cop.getDireccion())
                    .totalCoeficiente(cop.getTotalCoeficiente())
                    .totalUnidades(totalUnidades)
                    .totalModulosPermitidos(totalModulos)
                    .administradores(admins)
                    .build();
        }).toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Copropiedad> obtenerCopropiedad(@PathVariable Long id) {
        return copropiedadRepository.findById(id)
                .map(cop -> {
                    cop.setNit(formatearNit(cop.getNit()));
                    return ResponseEntity.ok(cop);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crearCopropiedad(@RequestBody Copropiedad copropiedad) {
        if (copropiedad.getNombre() == null || copropiedad.getNombre().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "El nombre de la copropiedad es obligatorio."));
        }

        if (copropiedad.getNit() == null || copropiedad.getNit().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "El NIT es obligatorio."));
        }

        if (copropiedad.getDireccion() == null || copropiedad.getDireccion().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "La dirección de la copropiedad es obligatoria."));
        }

        if (copropiedad.getTotalCoeficiente() == null || copropiedad.getTotalCoeficiente() <= 0) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "El coeficiente total es obligatorio y debe ser mayor a 0."));
        }

        String nitLimpio = copropiedad.getNit().replaceAll("\\D+", "");
        if (nitLimpio.isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "El NIT ingresado no contiene dígitos válidos."));
        }

        if (copropiedadRepository.findByNit(nitLimpio).isPresent()) {
            String nitFormateado = formatearNit(nitLimpio);
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Ya existe una copropiedad registrada con el NIT " + nitFormateado));
        }

        copropiedad.setNit(nitLimpio);
        copropiedad.setNombre(copropiedad.getNombre().trim());
        copropiedad.setDireccion(copropiedad.getDireccion().trim());
        Copropiedad guardada = copropiedadRepository.save(copropiedad);
        guardada.setNit(formatearNit(guardada.getNit()));
        return ResponseEntity.ok(guardada);
    }

    @PostMapping("/{id}/asignar-admin")
    public ResponseEntity<Usuario> asignarAdminACopropiedad(
            @PathVariable Long id,
            @RequestParam(required = false) String documentoAdmin,
            @RequestBody(required = false) CrearAdminDto dto) {

        String doc = (dto != null && dto.getDocumento() != null && !dto.getDocumento().isBlank())
                ? dto.getDocumento().trim()
                : (documentoAdmin != null ? documentoAdmin.trim() : "");

        if (doc.isBlank()) {
            throw new IllegalArgumentException("El número de documento del administrador es obligatorio.");
        }

        String nombre = (dto != null && dto.getNombreCompleto() != null && !dto.getNombreCompleto().isBlank())
                ? dto.getNombreCompleto().trim()
                : "";

        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre completo del administrador es obligatorio.");
        }

        String emailInput = (dto != null && dto.getEmail() != null) ? dto.getEmail().trim() : "";
        if (emailInput.isBlank()) {
            throw new IllegalArgumentException("El correo electrónico del administrador es obligatorio.");
        }

        if (!emailInput.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("El formato del correo electrónico ingresado es inválido (ej. usuario@ejemplo.com).");
        }

        String tel = (dto != null && dto.getTelefono() != null && !dto.getTelefono().isBlank())
                ? dto.getTelefono().trim()
                : "";

        if (tel.isBlank()) {
            throw new IllegalArgumentException("El teléfono del administrador es obligatorio.");
        }

        Copropiedad copropiedad = copropiedadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Copropiedad no encontrada con ID: " + id));

        // 1. Desvincular de esta copropiedad cualquier administrador anterior con documento diferente
        List<Usuario> todosLosUsuarios = usuarioRepository.findAll();
        for (Usuario u : todosLosUsuarios) {
            String uDoc = u.getDocumento();
            if (uDoc != null && !uDoc.equals(doc)
                    && u.getCopropiedadesAsignadas().stream().anyMatch(c -> c.getId().equals(id))) {
                u.getCopropiedadesAsignadas().removeIf(c -> c.getId().equals(id));
                usuarioRepository.save(u);
            }
        }

        // 2. Buscar o crear Persona por cédula (doc)
        Persona persona = personaRepository.findByCedula(doc).orElse(null);

        if (persona == null) {
            persona = personaRepository.save(Persona.builder()
                    .cedula(doc)
                    .nombreCompleto(nombre)
                    .email(emailInput)
                    .telefono(tel)
                    .build());
        } else {
            persona.setNombreCompleto(nombre);
            persona.setEmail(emailInput);
            persona.setTelefono(tel);
            persona = personaRepository.save(persona);
        }

        // 3. Buscar Usuario ÚNICAMENTE por el documento (cédula)
        Usuario admin = usuarioRepository.findByDocumento(doc).orElse(null);
        boolean esNuevoAdmin = (admin == null);
        String rawPass = passwordGeneratorService.generarPasswordSegura();

        if (admin == null) {
            String usernameGenerated = usernameService.generarUsernameUnico(nombre);

            admin = Usuario.builder()
                    .persona(persona)
                    .username(usernameGenerated)
                    .password(passwordEncoder.encode(rawPass))
                    .roles(new HashSet<>(List.of(Rol.ROLE_ADMIN)))
                    .copropiedadesAsignadas(new HashSet<>(List.of(copropiedad)))
                    .debeCambiarPassword(true)
                    .activo(true)
                    .build();
        } else {
            // Si el usuario ya existía para esta cédula, asegurar que apunte a la persona correcta
            admin.setPersona(persona);
            admin.setActivo(true);
            if (!admin.getRoles().contains(Rol.ROLE_ADMIN)) {
                admin.getRoles().add(Rol.ROLE_ADMIN);
            }
            if (admin.getCopropiedadesAsignadas() == null) {
                admin.setCopropiedadesAsignadas(new HashSet<>());
            }
            admin.getCopropiedadesAsignadas().add(copropiedad);
        }

        Usuario guardado = usuarioRepository.save(admin);

        // REPARACIÓN AUTOMÁTICA: Si el usuario guardado tiene username 'jahir.linares' pero el documento es diferente
        try {
            if ("jahir.linares".equalsIgnoreCase(guardado.getUsername()) && !"00000000".equals(doc)) {
                String nuevoUsername = usernameService.generarUsernameUnico(nombre);
                guardado.setUsername(nuevoUsername);
                guardado = usuarioRepository.save(guardado);
            }
        } catch (Exception e) {
            log.warn("Observación en reparación automática de username: {}", e.getMessage());
        }

        // 4. Envío en línea de credenciales/notificación y registro obligatorio en notificaciones_email
        String passTemporal = esNuevoAdmin ? rawPass : "[Tu contraseña habitual]";
        String asunto = "🎉 Tus Credenciales de Acceso a proyectoPH";
        String htmlBody = "Credenciales y asignación de acceso como administrador en " + copropiedad.getNombre() + " (Usuario: " + guardado.getUsername() + ")";

        emailService.enviarCredencialesIniciales(
                emailInput,
                guardado.getNombreCompleto(),
                guardado.getUsername(),
                passTemporal,
                copropiedad.getNombre()
        );

        notificacionService.registrarHistoricoNotificacion(
                copropiedad.getId(),
                persona != null ? persona.getId() : null,
                emailInput,
                guardado.getNombreCompleto(),
                asunto,
                htmlBody,
                com.ph.backend.model.TipoNotificacion.CREDENCIALES_INICIALES,
                true
        );

        return ResponseEntity.ok(guardado);
    }
}
