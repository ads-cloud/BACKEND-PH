package com.ph.backend.controller;

import com.ph.backend.config.tenant.TenantContext;
import com.ph.backend.dto.RenderedEmailDto;
import com.ph.backend.model.PlantillaEmail;
import com.ph.backend.service.PlantillaEmailService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/plantillas-email")
@RequiredArgsConstructor
public class PlantillaEmailController {

    private final PlantillaEmailService plantillaEmailService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<List<PlantillaEmail>> listarPlantillas(
            @RequestParam(value = "copropiedadId", required = false) Long copropiedadId) {
        Long tenantId = copropiedadId != null ? copropiedadId : TenantContext.getCurrentTenant();
        return ResponseEntity.ok(plantillaEmailService.listarPlantillas(tenantId));
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<PlantillaEmail> guardarOActualizarPlantilla(
            @PathVariable String codigo,
            @RequestBody GuardarPlantillaRequest req,
            @RequestParam(value = "copropiedadId", required = false) Long copropiedadId) {
        Long tenantId = copropiedadId != null ? copropiedadId : TenantContext.getCurrentTenant();
        PlantillaEmail guardada = plantillaEmailService.guardarOActualizarPlantilla(
                tenantId,
                codigo.toUpperCase(),
                req.getNombre(),
                req.getAsunto(),
                req.getCuerpoHtml(),
                req.getVariablesDisponibles()
        );
        return ResponseEntity.ok(guardada);
    }

    @DeleteMapping("/{codigo}/restaurar")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<?> restaurarPlantillaDefault(
            @PathVariable String codigo,
            @RequestParam(value = "copropiedadId", required = false) Long copropiedadId) {
        Long tenantId = copropiedadId != null ? copropiedadId : TenantContext.getCurrentTenant();
        plantillaEmailService.restaurarPlantillaDefault(tenantId, codigo.toUpperCase());
        return ResponseEntity.ok(Map.of("message", "Plantilla restaurada a su valor global por defecto exitosamente."));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<RenderedEmailDto> previsualizarPlantilla(@RequestBody PreviewPlantillaRequest req) {
        Map<String, String> vars = req.getVariables() != null ? req.getVariables() : Map.of(
                "nombre", "Juan Pérez",
                "username", "jperez",
                "password", "pass1234",
                "pin", "123456",
                "pinAsamblea", "654321",
                "contexto", "Acceso a la Plataforma"
        );

        String asuntoProcesado = req.getAsunto() != null ? reemplazarVars(req.getAsunto(), vars) : "";
        String htmlProcesado = req.getCuerpoHtml() != null ? reemplazarVars(req.getCuerpoHtml(), vars) : "";

        return ResponseEntity.ok(RenderedEmailDto.builder()
                .asunto(asuntoProcesado)
                .cuerpoHtml(htmlProcesado)
                .build());
    }

    private String reemplazarVars(String texto, Map<String, String> vars) {
        if (texto == null) return "";
        String res = texto;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            res = res.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return res;
    }

    @Data
    public static class GuardarPlantillaRequest {
        private String nombre;
        private String asunto;
        private String cuerpoHtml;
        private String variablesDisponibles;
    }

    @Data
    public static class PreviewPlantillaRequest {
        private String asunto;
        private String cuerpoHtml;
        private Map<String, String> variables;
    }
}
