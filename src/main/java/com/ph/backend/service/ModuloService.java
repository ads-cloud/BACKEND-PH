package com.ph.backend.service;

import com.ph.backend.model.CopropiedadModulo;
import com.ph.backend.model.Modulo;
import com.ph.backend.model.Rol;
import com.ph.backend.model.Usuario;
import com.ph.backend.repository.CopropiedadModuloRepository;
import com.ph.backend.repository.CopropiedadRepository;
import com.ph.backend.repository.ModuloRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModuloService {

    private final ModuloRepository moduloRepository;
    private final CopropiedadModuloRepository copropiedadModuloRepository;
    private final CopropiedadRepository copropiedadRepository;

    @Transactional(readOnly = true)
    public List<Modulo> obtenerModulosAutorizados(Usuario usuario, Long copropiedadId) {
        if (usuario == null) {
            return Collections.emptyList();
        }

        Set<Rol> rolesUsuario = usuario.getRoles();
        if (rolesUsuario == null || rolesUsuario.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("🔍 Módulos autorizados para usuario: {}, copropiedadId: {}", usuario.getUsername(), copropiedadId);

        List<Modulo> baseModulos;
        try {
            baseModulos = new ArrayList<>(moduloRepository.findByRolesPermitidosInAndActivoTrueOrderByOrdenAsc(rolesUsuario));
        } catch (Exception e) {
            log.warn("Observación al consultar módulos por rol: {}. Obteniendo catálogo activo...", e.getMessage());
            baseModulos = new ArrayList<>(moduloRepository.findByActivoTrueOrderByOrdenAsc());
        }

        // Si es SuperAdmin, retornar módulos autorizados asegurando la inclusión de ADMIN_MODULOS
        if (rolesUsuario.contains(Rol.ROLE_SUPER_ADMIN)) {
            boolean existeAdminModulos = baseModulos.stream().anyMatch(m -> "ADMIN_MODULOS".equals(m.getCodigo()) || "GESTION_CATALOGO_MODULOS".equals(m.getCodigo()));
            if (!existeAdminModulos) {
                Modulo adminMod = Modulo.builder()
                        .id(999L)
                        .codigo("ADMIN_MODULOS")
                        .titulo("Administración de Módulos")
                        .descripcion("Gestión global del catálogo de módulos del sistema, parametrización de rutas, íconos y roles permitidos.")
                        .icono("Settings")
                        .ruta("admin_modulos")
                        .colorGradient("from-indigo-600 via-purple-600 to-pink-600")
                        .borderColor("border-purple-500/40 hover:border-purple-400")
                        .shadowColor("shadow-purple-500/10 hover:shadow-purple-500/25")
                        .badge("Disponible")
                        .badgeColor("bg-emerald-500/20 text-emerald-300 border-emerald-500/40")
                        .activo(true)
                        .orden(10)
                        .rolesPermitidos(new HashSet<>(Set.of(Rol.ROLE_SUPER_ADMIN)))
                        .build();
                baseModulos.add(adminMod);
            }
            return baseModulos;
        }

        Long copIdTarget = copropiedadId;
        if (copIdTarget == null && usuario.getCopropiedadesAsignadas() != null && !usuario.getCopropiedadesAsignadas().isEmpty()) {
            copIdTarget = usuario.getCopropiedadesAsignadas().iterator().next().getId();
        }

        if (copIdTarget == null) {
            return baseModulos;
        }

        try {
            List<CopropiedadModulo> copModulosInactivos = copropiedadModuloRepository.findByCopropiedadIdAndActivoFalse(copIdTarget);
            if (!copModulosInactivos.isEmpty()) {
                Set<Long> modulosDeshabilitados = copModulosInactivos.stream()
                        .map(cm -> cm.getModulo().getId())
                        .collect(Collectors.toSet());

                return baseModulos.stream()
                        .filter(m -> !modulosDeshabilitados.contains(m.getId()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Observación al verificar módulos deshabilitados por copropiedad: {}", e.getMessage());
        }

        return baseModulos;
    }

    @Transactional(readOnly = true)
    public List<CopropiedadModulo> obtenerModulosCopropiedad(Long copropiedadId) {
        return copropiedadModuloRepository.findByCopropiedadId(copropiedadId);
    }

    @Transactional
    public void guardarModulosCopropiedad(Long copropiedadId, List<Long> modulosActivosIds) {
        var copropiedad = copropiedadRepository.findById(copropiedadId)
                .orElseThrow(() -> new IllegalArgumentException("Copropiedad no encontrada: " + copropiedadId));

        List<Modulo> todosLosModulos = moduloRepository.findAll();
        for (Modulo mod : todosLosModulos) {
            boolean activo = modulosActivosIds.contains(mod.getId());
            var opt = copropiedadModuloRepository.findByCopropiedadIdAndModuloId(copropiedadId, mod.getId());
            if (opt.isPresent()) {
                CopropiedadModulo cm = opt.get();
                cm.setActivo(activo);
                copropiedadModuloRepository.save(cm);
            } else {
                CopropiedadModulo cm = CopropiedadModulo.builder()
                        .copropiedad(copropiedad)
                        .modulo(mod)
                        .activo(activo)
                        .build();
                copropiedadModuloRepository.save(cm);
            }
        }
        log.info("💾 Módulos de la copropiedad ID {} actualizados por SuperAdmin", copropiedadId);
    }

    @Transactional(readOnly = true)
    public List<Modulo> obtenerTodosLosModulos() {
        return moduloRepository.findAllByOrderByOrdenAsc();
    }

    @Transactional(readOnly = true)
    public List<Modulo> obtenerCatalogoGlobalModulos() {
        return moduloRepository.findAllByOrderByOrdenAsc();
    }

    @Transactional(readOnly = true)
    public List<Modulo> obtenerModulosParaAdministracion(Usuario usuario, Long copropiedadId) {
        List<Modulo> todos = moduloRepository.findAllByOrderByOrdenAsc();
        if (usuario == null) {
            return Collections.emptyList();
        }

        Set<Rol> rolesUsuario = usuario.getRoles();
        boolean isSuperAdmin = rolesUsuario != null && rolesUsuario.contains(Rol.ROLE_SUPER_ADMIN);

        if (isSuperAdmin && copropiedadId == null) {
            return todos;
        }

        List<Modulo> paraAdmin = todos.stream()
                .filter(m -> !"MONITOREO".equals(m.getCodigo()) && !"ADMIN_PH".equals(m.getCodigo()))
                .collect(Collectors.toList());

        if (isSuperAdmin) {
            return paraAdmin;
        }

        Long copIdTarget = copropiedadId;
        if (copIdTarget == null && usuario.getCopropiedadesAsignadas() != null && !usuario.getCopropiedadesAsignadas().isEmpty()) {
            copIdTarget = usuario.getCopropiedadesAsignadas().iterator().next().getId();
        }

        if (copIdTarget == null) {
            return paraAdmin;
        }

        List<CopropiedadModulo> copModulosInactivos = copropiedadModuloRepository.findByCopropiedadIdAndActivoFalse(copIdTarget);
        if (copModulosInactivos.isEmpty()) {
            return paraAdmin;
        }

        Set<Long> modulosDeshabilitados = copModulosInactivos.stream()
                .map(cm -> cm.getModulo().getId())
                .collect(Collectors.toSet());

        return paraAdmin.stream()
                .filter(m -> !modulosDeshabilitados.contains(m.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Modulo guardarOActualizarModulo(Modulo modulo) {
        log.info("💾 Guardando/Actualizando módulo: {}", modulo.getCodigo());
        return moduloRepository.save(modulo);
    }

    @Transactional
    public void cambiarEstadoModulo(Long id, boolean activo) {
        moduloRepository.findById(id).ifPresent(m -> {
            m.setActivo(activo);
            moduloRepository.save(m);
            log.info("🔄 Estado del módulo {} cambiado a {}", m.getCodigo(), activo);
        });
    }
}
