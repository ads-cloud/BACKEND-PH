package com.ph.backend.controller;

import com.ph.backend.dto.CargueMasivoResponseDto;
import com.ph.backend.dto.CrearUnidadDto;
import com.ph.backend.model.UnidadPrivada;
import com.ph.backend.model.Usuario;
import com.ph.backend.repository.UnidadPrivadaRepository;
import com.ph.backend.repository.UsuarioRepository;
import com.ph.backend.service.UnidadPrivadaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/unidades")
@RequiredArgsConstructor
@Slf4j
public class UnidadPrivadaController {

    private final UnidadPrivadaRepository unidadPrivadaRepository;
    private final UnidadPrivadaService unidadPrivadaService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<UnidadPrivada>> listarUnidades() {
        Long tenantId = com.ph.backend.config.tenant.TenantContext.getCurrentTenant();
        return ResponseEntity.ok(unidadPrivadaRepository.findByCopropiedadId(tenantId));
    }

    @GetMapping("/mis-inmuebles")
    public ResponseEntity<List<UnidadPrivada>> obtenerMisInmuebles(@RequestParam(required = false) String documento) {
        String doc = documento;
        if (doc == null || doc.isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String username = auth.getName();
                Usuario usuario = usuarioRepository.findByUsername(username)
                        .orElseGet(() -> usuarioRepository.findByDocumento(username).orElse(null));
                if (usuario != null) {
                    doc = usuario.getDocumento();
                }
            }
        }

        if (doc == null || doc.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(unidadPrivadaService.obtenerMisInmuebles(doc));
    }

    @GetMapping("/plantilla-excel")
    public ResponseEntity<byte[]> descargarPlantillaExcel() {
        byte[] excelBytes = unidadPrivadaService.generarPlantillaExcel();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "plantilla_cargue_unidades.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }

    @PostMapping
    public ResponseEntity<UnidadPrivada> crearUnidad(@RequestBody CrearUnidadDto dto) {
        return ResponseEntity.ok(unidadPrivadaService.crearUnidad(dto));
    }

    @PostMapping(value = "/cargue-masivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CargueMasivoResponseDto> cargueMasivoUnidades(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "copropiedadId", required = false) Long copropiedadId) {
        Long tenantId = copropiedadId != null ? copropiedadId : com.ph.backend.config.tenant.TenantContext.getCurrentTenant();
        return ResponseEntity.ok(unidadPrivadaService.procesarCargueMasivo(file, tenantId));
    }

    @PatchMapping("/{id}/aprobar-poder")
    public ResponseEntity<UnidadPrivada> aprobarPoder(@PathVariable Long id, @RequestParam Boolean aprobado) {
        UnidadPrivada unidad = unidadPrivadaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidad inmobiliaria no encontrada con ID: " + id));

        unidad.setPoderAprobado(aprobado);
        UnidadPrivada actualizada = unidadPrivadaRepository.save(unidad);
        return ResponseEntity.ok(actualizada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnidadPrivada> actualizarUnidad(
            @PathVariable Long id,
            @RequestBody com.ph.backend.dto.ActualizarUnidadDto dto) {
        return ResponseEntity.ok(unidadPrivadaService.actualizarUnidad(id, dto));
    }
}
