package com.ph.backend.service;

import com.ph.backend.model.Copropiedad;
import com.ph.backend.model.Modulo;
import com.ph.backend.model.PlantillaEmail;
import com.ph.backend.model.Rol;
import com.ph.backend.repository.CopropiedadRepository;
import com.ph.backend.repository.ModuloRepository;
import com.ph.backend.repository.PlantillaEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializerService implements CommandLineRunner {

    private final CopropiedadRepository copropiedadRepository;
    private final PlantillaEmailRepository plantillaEmailRepository;
    private final ModuloRepository moduloRepository;

    @Override
    public void run(String... args) throws Exception {
        sanitizarNitsExistentes();
        inicializarPlantillasEmailDefault();
        inicializarModulosSistemaDefault();
    }

    private void sanitizarNitsExistentes() {
        try {
            List<Copropiedad> copropiedades = copropiedadRepository.findAll();
            for (Copropiedad cop : copropiedades) {
                if (cop.getNit() != null) {
                    String nitLimpio = cop.getNit().replaceAll("\\D+", "");
                    if (!nitLimpio.isBlank() && !nitLimpio.equals(cop.getNit())) {
                        cop.setNit(nitLimpio);
                        copropiedadRepository.save(cop);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Observación al sanitizar NITs en inicio: {}", e.getMessage());
        }
    }

    private void inicializarPlantillasEmailDefault() {
        try {
            crearSiNoExiste(
                    "CREDENCIALES_INICIALES",
                    "Credenciales de Acceso Inicial",
                    "Bienvenido a ProyectoPH - Credenciales de Acceso",
                    """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #0f172a; color: #f8fafc;">
                        <div style="text-align: center; padding-bottom: 20px; border-bottom: 1px solid #1e293b;">
                            <h2 style="color: #6366f1; margin: 0;">proyectoPH — Credenciales de Acceso</h2>
                            <p style="color: #94a3b8; font-size: 14px; margin-top: 5px;">{{contexto}}</p>
                        </div>
                        <div style="padding: 20px 0;">
                            <p style="font-size: 16px; color: #cbd5e1;">Hola <strong>{{nombre}}</strong>,</p>
                            <p style="font-size: 14px; color: #94a3b8;">Se ha registrado tu cuenta en la plataforma <strong>ProyectoPH</strong>. Tus credenciales de acceso son:</p>
                            <div style="background-color: #1e293b; padding: 16px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #6366f1;">
                                <p style="margin: 5px 0; color: #e2e8f0;"><strong>Usuario:</strong> <span style="color: #818cf8;">{{username}}</span></p>
                                <p style="margin: 5px 0; color: #e2e8f0;"><strong>Contraseña inicial:</strong> <span style="color: #34d399;">{{password}}</span></p>
                            </div>
                            <p style="font-size: 12px; color: #94a3b8;">Por motivos de seguridad, te sugerimos cambiar tu contraseña al ingresar por primera vez.</p>
                        </div>
                        <div style="text-align: center; padding-top: 20px; border-top: 1px solid #1e293b; font-size: 12px; color: #64748b;">
                            <p>proyectoPH &copy; 2026 — Plataforma de Gestión de Propiedad Horizontal</p>
                        </div>
                    </div>
                    """,
                    "{{nombre}}, {{username}}, {{password}}, {{contexto}}"
            );

            crearSiNoExiste(
                    "OTP_ASAMBLEA",
                    "Código Clave de Acceso a Asamblea",
                    "🔑 Tu Clave de Acceso a la Asamblea — proyectoPH",
                    """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #0f172a; color: #f8fafc;">
                        <div style="text-align: center; padding-bottom: 20px; border-bottom: 1px solid #1e293b;">
                            <h2 style="color: #6366f1; margin: 0;">proyectoPH — Asamblea General</h2>
                            <p style="color: #94a3b8; font-size: 14px; margin-top: 5px;">Plataforma de Votaciones Ley 675 de 2001</p>
                        </div>
                        <div style="padding: 20px 0;">
                            <p style="font-size: 16px; color: #cbd5e1;">Hola <strong>{{nombre}}</strong>,</p>
                            <p style="font-size: 14px; color: #94a3b8;">Tu solicitud de ingreso presencial a la asamblea ha sido procesada. Utiliza el siguiente código de verificación temporal de 6 dígitos para acceder a votar:</p>
                            <div style="text-align: center; margin: 30px 0;">
                                <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #10b981; background-color: #022c22; padding: 12px 24px; border-radius: 8px; border: 1px solid #059669;">
                                    {{pinAsamblea}}
                                </span>
                            </div>
                            <p style="font-size: 12px; color: #64748b; text-align: center;">Este código es de uso personal e intransferible. Válido durante la sesión de la asamblea.</p>
                        </div>
                        <div style="text-align: center; padding-top: 20px; border-top: 1px solid #1e293b; font-size: 12px; color: #64748b;">
                            <p>proyectoPH &copy; 2026 — Gestión de Propiedad Horizontal</p>
                        </div>
                    </div>
                    """,
                    "{{nombre}}, {{pinAsamblea}}"
            );

            crearSiNoExiste(
                    "RESET_PASSWORD",
                    "Restablecimiento de Contraseña (PIN)",
                    "🔑 Código de Verificación para Restablecer Contraseña - proyectoPH",
                    """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #0f172a; color: #f8fafc;">
                        <div style="text-align: center; padding-bottom: 20px; border-bottom: 1px solid #1e293b;">
                            <h2 style="color: #6366f1; margin: 0;">proyectoPH — Restablecimiento de Contraseña</h2>
                        </div>
                        <div style="padding: 20px 0;">
                            <p style="font-size: 16px; color: #cbd5e1;">Hola <strong>{{nombre}}</strong>,</p>
                            <p style="font-size: 14px; color: #94a3b8;">Has solicitado restablecer tu contraseña en la plataforma. Tu PIN de verificación de 6 dígitos es:</p>
                            <div style="text-align: center; margin: 30px 0;">
                                <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #6366f1; background-color: #1e1b4b; padding: 12px 24px; border-radius: 8px; border: 1px solid #4f46e5;">
                                    {{pin}}
                                </span>
                            </div>
                            <p style="font-size: 12px; color: #94a3b8; text-align: center;">Este código expira en 15 minutos. Si no solicitaste este cambio, puedes ignorar este mensaje.</p>
                        </div>
                        <div style="text-align: center; padding-top: 20px; border-top: 1px solid #1e293b; font-size: 12px; color: #64748b;">
                            <p>proyectoPH &copy; 2026 — Gestión de Propiedad Horizontal</p>
                        </div>
                    </div>
                    """,
                    "{{nombre}}, {{pin}}"
            );
        } catch (Exception e) {
            log.warn("Observación al inicializar plantillas HTML de correo por defecto: {}", e.getMessage());
        }
    }

    private void crearSiNoExiste(String codigo, String nombre, String asunto, String cuerpoHtml, String variablesDisponibles) {
        if (plantillaEmailRepository.findByCodigoAndCopropiedadIdIsNull(codigo).isEmpty()) {
            PlantillaEmail p = PlantillaEmail.builder()
                    .codigo(codigo)
                    .nombre(nombre)
                    .asunto(asunto)
                    .cuerpoHtml(cuerpoHtml.trim())
                    .variablesDisponibles(variablesDisponibles)
                    .activo(true)
                    .build();
            plantillaEmailRepository.save(p);
        }
    }

    private void inicializarModulosSistemaDefault() {
        try {
            crearModuloSiNoExiste(
                    "CONFIGURACION_PH",
                    "Configuración del Conjunto",
                    "Personalización de plantillas HTML de correo, usuarios gestores e información del conjunto.",
                    "Settings",
                    "configuracion_ph",
                    "from-indigo-600 via-purple-600 to-pink-600",
                    "border-purple-500/40 hover:border-purple-400",
                    "shadow-purple-500/10 hover:shadow-purple-500/25",
                    "Activo",
                    "bg-indigo-500/20 text-indigo-300 border-indigo-500/40",
                    5,
                    Set.of(Rol.ROLE_ADMIN, Rol.ROLE_SUPER_ADMIN)
            );
        } catch (Exception e) {
            log.warn("Observación al inicializar módulos por defecto: {}", e.getMessage());
        }
    }

    private void crearModuloSiNoExiste(String codigo, String titulo, String descripcion, String icono,
                                      String ruta, String colorGradient, String borderColor, String shadowColor,
                                      String badge, String badgeColor, int orden, Set<Rol> roles) {
        if (moduloRepository.findByCodigo(codigo).isEmpty()) {
            Modulo modulo = Modulo.builder()
                    .codigo(codigo)
                    .titulo(titulo)
                    .descripcion(descripcion)
                    .icono(icono)
                    .ruta(ruta)
                    .colorGradient(colorGradient)
                    .borderColor(borderColor)
                    .shadowColor(shadowColor)
                    .badge(badge)
                    .badgeColor(badgeColor)
                    .activo(true)
                    .orden(orden)
                    .rolesPermitidos(new HashSet<>(roles))
                    .build();
            moduloRepository.save(modulo);
            log.info("✅ Módulo '{}' creado exitosamente en el catálogo.", titulo);
        }
    }
}

