package com.ph.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "asistencias_asamblea", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"asamblea_id", "unidad_privada_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AsistenciaAsamblea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asamblea_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Asamblea asamblea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_privada_id", nullable = false)
    private UnidadPrivada unidadPrivada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Column(nullable = false)
    private LocalDateTime fechaHoraIngreso;

    @Builder.Default
    @Column(nullable = false)
    private Boolean esApoderado = false;

    @Column(name = "pin_asamblea")
    private String pinAsamblea;

    @Builder.Default
    @Column(name = "asistencia_confirmada", nullable = false)
    private Boolean asistenciaConfirmada = false;
}
