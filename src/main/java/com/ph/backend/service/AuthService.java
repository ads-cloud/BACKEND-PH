package com.ph.backend.service;

import com.ph.backend.config.security.JwtTokenProvider;
import com.ph.backend.dto.*;
import com.ph.backend.model.Asamblea;
import com.ph.backend.model.AsambleaStatus;
import com.ph.backend.model.UnidadPrivada;
import com.ph.backend.model.Usuario;
import com.ph.backend.model.AsistenciaAsamblea;
import com.ph.backend.repository.AsistenciaAsambleaRepository;
import com.ph.backend.repository.AsambleaRepository;
import com.ph.backend.repository.UnidadPrivadaRepository;
import com.ph.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.ph.backend.model.Persona;
import com.ph.backend.repository.PersonaRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final UnidadPrivadaRepository unidadPrivadaRepository;
    private final AsambleaRepository asambleaRepository;
    private final AsistenciaAsambleaRepository asistenciaRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionService notificacionService;

    @Transactional
    public String solicitarPinResetPassword(SolicitarResetPasswordDto dto) {
        Usuario usuario = buscarUsuarioPorIdentificador(dto.getDocumento(), dto.getEmail());

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("El usuario no tiene un correo electrónico registrado en el sistema.");
        }

        String pin = String.format("%06d", new Random().nextInt(999999));
        usuario.setClaveOtp(pin);
        usuario.setFechaExpiracionOtp(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);

        // Enviar correo vía Resend asíncrono
        emailService.enviarClaveAcceso(usuario.getEmail(), usuario.getNombreCompleto(), pin);

        // Registrar copia histórica de auditoría en la tabla notificaciones_email
        Long copId = (usuario.getCopropiedadesAsignadas() != null && !usuario.getCopropiedadesAsignadas().isEmpty())
                ? usuario.getCopropiedadesAsignadas().iterator().next().getId()
                : 1L;
        Long personaId = usuario.getPersona() != null ? usuario.getPersona().getId() : null;

        notificacionService.registrarHistoricoNotificacion(
                copId,
                personaId,
                usuario.getEmail(),
                usuario.getNombreCompleto(),
                "🔑 Restablecimiento de Contraseña — proyectoPH",
                "PIN de 6 dígitos generado y enviado para restablecimiento de contraseña (PIN: " + enmascararPin(pin) + ")",
                com.ph.backend.model.TipoNotificacion.RESET_PASSWORD,
                true);

        return "Se ha enviado un PIN de 6 dígitos al correo electrónico registrado";
    }

    @Transactional(readOnly = true)
    public String verificarPinResetPassword(VerificarPinDto dto) {
        String pin = dto.getPin() != null ? dto.getPin().trim() : "";
        Usuario usuario = buscarUsuarioPorIdentificador(dto.getDocumento(), dto.getEmail());

        boolean esPinValido = usuario.getClaveOtp() != null && usuario.getClaveOtp().equals(pin);

        if (!esPinValido) {
            throw new IllegalArgumentException("El PIN ingresado es incorrecto.");
        }

        if (usuario.getFechaExpiracionOtp() != null
                && LocalDateTime.now().isAfter(usuario.getFechaExpiracionOtp())) {
            throw new IllegalArgumentException("El PIN de seguridad ha expirado. Por favor solicita uno nuevo.");
        }

        return "PIN verificado correctamente. Puede proceder a ingresar su nueva contraseña.";
    }

    @Transactional
    public String confirmarResetPassword(RestablecerPasswordDto dto) {
        String pin = dto.getPin() != null ? dto.getPin().trim() : "";
        String nuevaPassword = dto.getNuevaPassword() != null ? dto.getNuevaPassword().trim() : "";

        if (nuevaPassword.length() < 6) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        Usuario usuario = buscarUsuarioPorIdentificador(dto.getDocumento(), dto.getEmail());

        boolean esPinValido = usuario.getClaveOtp() != null && usuario.getClaveOtp().equals(pin);

        if (!esPinValido) {
            throw new IllegalArgumentException("El PIN ingresado es incorrecto.");
        }

        if (usuario.getFechaExpiracionOtp() != null
                && LocalDateTime.now().isAfter(usuario.getFechaExpiracionOtp())) {
            throw new IllegalArgumentException("El PIN de seguridad ha expirado. Por favor solicita uno nuevo.");
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setClaveOtp(null);
        usuario.setFechaExpiracionOtp(null);
        usuarioRepository.save(usuario);

        return "¡Contraseña actualizada exitosamente! Ya puedes iniciar sesión con tu nueva contraseña.";
    }

    private Usuario buscarUsuarioPorIdentificador(String documento, String email) {
        String doc = documento != null ? documento.trim() : null;
        String em = email != null ? email.trim().toLowerCase() : null;

        if (doc != null && !doc.isBlank()) {
            return usuarioRepository.findByDocumento(doc)
                    .orElseGet(() -> usuarioRepository.findByUsername(doc)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "No se encontró ningún usuario registrado con la cédula/documento: " + doc)));
        }

        if (em != null && !em.isBlank()) {
            return usuarioRepository.findByEmail(em)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se encontró ninguna cuenta registrada con el correo: " + em));
        }

        throw new IllegalArgumentException("Debe ingresar el número de cédula o documento de identidad.");
    }

    @Transactional
    public String solicitarClaveOtp(SolicitarClaveRequestDto dto) {
        String doc = dto.getDocumento().trim();

        Usuario usuario = usuarioRepository.findByDocumento(doc)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún usuario registrado con el documento: " + doc));

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("El usuario no tiene un correo electrónico registrado.");
        }

        // Generar OTP de 6 dígitos
        String otp = String.format("%06d", new Random().nextInt(999999));
        usuario.setClaveOtp(otp);
        usuario.setFechaExpiracionOtp(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);

        // Enviar correo vía Resend
        emailService.enviarClaveAcceso(usuario.getEmail(), usuario.getNombreCompleto(), otp);

        // Registrar copia histórica de auditoría en la tabla notificaciones_email
        Long copId = (usuario.getCopropiedadesAsignadas() != null && !usuario.getCopropiedadesAsignadas().isEmpty())
                ? usuario.getCopropiedadesAsignadas().iterator().next().getId()
                : 1L;
        Long personaId = usuario.getPersona() != null ? usuario.getPersona().getId() : null;

        notificacionService.registrarHistoricoNotificacion(
                copId,
                personaId,
                usuario.getEmail(),
                usuario.getNombreCompleto(),
                "🔑 Tu Clave de Acceso Temporal — proyectoPH",
                "Clave temporal OTP enviada: " + enmascararPin(otp),
                com.ph.backend.model.TipoNotificacion.OTP,
                true);

        return "Se ha enviado una clave de acceso temporal de 6 dígitos al correo registrado ("
                + enmascararEmail(usuario.getEmail()) + ").";
    }

    @Transactional
    public JwtAuthResponseDto loginConOtp(LoginOtpRequestDto dto) {
        String doc = dto.getDocumento().trim();
        String otpIngresado = dto.getClaveOtp().trim();

        Usuario usuario = usuarioRepository.findByDocumento(doc)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con documento: " + doc));

        boolean esOtpValido = usuario.getClaveOtp() != null && usuario.getClaveOtp().equals(otpIngresado);

        if (!esOtpValido) {
            throw new IllegalArgumentException("La clave ingresada es inválida o ha expirado.");
        }

        if (usuario.getFechaExpiracionOtp() != null
                && LocalDateTime.now().isAfter(usuario.getFechaExpiracionOtp())) {
            throw new IllegalArgumentException(
                    "La clave temporal ingresada ya ha expirado. Por favor solicita una nueva.");
        }

        // Limpiar OTP utilizado
        usuario.setClaveOtp(null);
        usuario.setFechaExpiracionOtp(null);
        usuarioRepository.save(usuario);

        org.springframework.security.core.Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                usuario.getUsername(), null,
                usuario.getRoles().stream().map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(r.name())).toList());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);
        return construirResponseDtoParaUsuario(usuario, token);
    }

    @Transactional
    public JwtAuthResponseDto login(LoginRequestDto loginDto) {
        String usernameInput = loginDto.getUsername().trim();

        Usuario usuario = usuarioRepository.findByUsername(usernameInput)
                .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña incorrectos"));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usuario.getUsername(), loginDto.getPassword().trim()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        return construirResponseDtoParaUsuario(usuario, token);
    }

    @Transactional(readOnly = true)
    public JwtAuthResponseDto obtenerPerfilActual(org.springframework.security.core.Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseGet(() -> usuarioRepository.findByDocumento(username)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username)));
        return construirResponseDtoParaUsuario(usuario, null);
    }

    @Transactional(readOnly = true)
    public JwtAuthResponseDto obtenerPerfil(String documento) {
        String doc = documento.trim();
        Usuario usuario = usuarioRepository.findByDocumento(doc)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con documento: " + doc));
        return construirResponseDtoParaUsuario(usuario, null);
    }

    private JwtAuthResponseDto construirResponseDtoParaUsuario(Usuario usuario, String token) {
        // Buscar unidades asociadas como propietario o apoderado
        List<UnidadPrivada> unidades = unidadPrivadaRepository
                .findByPropietarioCedulaOrApoderadoDocumento(usuario.getDocumento());
        List<UnidadRepresentadaDto> representadas = new ArrayList<>();

        // Buscar asamblea activa
        Long asambleaActivaId = null;
        if (!unidades.isEmpty()) {
            Long copropiedadId = unidades.get(0).getCopropiedad().getId();
            Asamblea asamblea = asambleaRepository
                    .findFirstByCopropiedadIdAndEstadoOrderByFechaDesc(copropiedadId, AsambleaStatus.EN_CURSO)
                    .orElseGet(() -> asambleaRepository
                            .findFirstByCopropiedadIdAndEstadoOrderByFechaDesc(copropiedadId,
                                    AsambleaStatus.EN_REGISTRO)
                            .orElseGet(() -> asambleaRepository
                                    .findFirstByCopropiedadIdAndEstadoOrderByFechaDesc(copropiedadId,
                                            AsambleaStatus.PROGRAMADA)
                                    .orElse(null)));

            if (asamblea != null) {
                asambleaActivaId = asamblea.getId();

                // Verificar únicamente asistencia registrada presencialmente por el
                // administrador
                for (UnidadPrivada u : unidades) {
                    boolean esPorPoder = (u.getApoderado() != null
                            && u.getApoderado().getDocumento().equals(usuario.getDocumento()));

                    Optional<AsistenciaAsamblea> optAsistencia = asistenciaRepository
                            .findByAsambleaIdAndUnidadPrivadaId(asamblea.getId(), u.getId());
                    boolean registrada = optAsistencia.isPresent();
                    boolean confirmada = optAsistencia.map(AsistenciaAsamblea::getAsistenciaConfirmada).orElse(false);

                    representadas.add(UnidadRepresentadaDto.builder()
                            .unidadId(u.getId())
                            .torre(u.getTorre())
                            .numeroUnidad(u.getNumeroUnidad())
                            .coeficiente(u.getCoeficiente())
                            .esPorPoder(esPorPoder)
                            .poderAprobado(u.getPoderAprobado())
                            .asistenciaRegistrada(registrada)
                            .asistenciaConfirmada(confirmada)
                            .build());
                }
            }
        }

        boolean isSuperAdmin = usuario.getRoles().contains(com.ph.backend.model.Rol.ROLE_SUPER_ADMIN);
        List<com.ph.backend.model.Copropiedad> copropiedadesAsignadas = new ArrayList<>(
                usuario.getCopropiedadesAsignadas());
        Long primaryCopropiedadId = !copropiedadesAsignadas.isEmpty() ? copropiedadesAsignadas.get(0).getId() : 1L;
        String primaryCopropiedadNombre = !copropiedadesAsignadas.isEmpty() ? copropiedadesAsignadas.get(0).getNombre()
                : "Copropiedad Principal";

        boolean debeCambiar = Boolean.TRUE.equals(usuario.getDebeCambiarPassword());

        return JwtAuthResponseDto.builder()
                .accessToken(token)
                .tokenType(token != null ? "Bearer" : null)
                .usuarioId(usuario.getId())
                .copropiedadId(primaryCopropiedadId)
                .copropiedadNombre(primaryCopropiedadNombre)
                .documento(usuario.getDocumento())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .roles(usuario.getRoles())
                .isSuperAdmin(isSuperAdmin)
                .debeCambiarPassword(debeCambiar)
                .copropiedadesAsignadas(copropiedadesAsignadas)
                .asambleaActivaId(asambleaActivaId)
                .unidadesRepresentadas(representadas)
                .build();
    }

    @Transactional
    public JwtAuthResponseDto actualizarPerfil(ActualizarPerfilDto dto) {
        String doc = dto.getDocumento().trim();

        Optional<Persona> optPersona = personaRepository.findByCedula(doc);
        if (optPersona.isPresent()) {
            Persona p = optPersona.get();
            if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
                p.setEmail(dto.getEmail().trim().toLowerCase());
            }
            if (dto.getTelefono() != null) {
                p.setTelefono(dto.getTelefono().trim());
            }
            personaRepository.save(p);
        }

        if (dto.getNuevaPassword() != null && !dto.getNuevaPassword().trim().isEmpty()) {
            String nuevaPwd = dto.getNuevaPassword().trim();
            if (nuevaPwd.length() < 6) {
                throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres.");
            }
            Optional<Usuario> optUsuario = usuarioRepository.findByDocumento(doc);
            if (optUsuario.isPresent()) {
                Usuario u = optUsuario.get();
                u.setPassword(passwordEncoder.encode(nuevaPwd));
                u.setDebeCambiarPassword(false);
                usuarioRepository.save(u);
            }
        }

        return obtenerPerfil(doc);
    }

    @Transactional
    public JwtAuthResponseDto cambiarPasswordInicial(CambiarPasswordInicialDto dto) {
        String username = dto.getUsername().trim();
        String pwdActual = dto.getPasswordActual().trim();
        String nuevaPwd = dto.getNuevaPassword().trim();

        if (nuevaPwd.length() < 6) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con username: " + username));

        boolean esValida = passwordEncoder.matches(pwdActual, usuario.getPassword());
        if (!esValida) {
            throw new IllegalArgumentException("La contraseña temporal ingresada es incorrecta.");
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPwd));
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);

        return login(LoginRequestDto.builder().username(username).password(nuevaPwd).build());
    }

    private String enmascararEmail(String email) {
        if (email == null || !email.contains("@"))
            return email;
        String[] partes = email.split("@");
        String usuarioStr = partes[0];
        if (usuarioStr.length() <= 2)
            return "*@" + partes[1];
        return usuarioStr.substring(0, 2) + "***@" + partes[1];
    }

    private String enmascararPin(String pin) {
        if (pin == null || pin.length() < 2)
            return "****";
        return "****" + pin.substring(pin.length() - 2);
    }
}
