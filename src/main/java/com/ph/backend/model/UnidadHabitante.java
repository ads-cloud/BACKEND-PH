package com.ph.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unidad_habitantes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"unidad_id", "persona_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UnidadHabitante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_id", nullable = false)
    private UnidadPrivada unidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Builder.Default
    @Column(name = "es_propietario_principal", nullable = false)
    private Boolean esPropietarioPrincipal = false;

    @Builder.Default
    @Column(name = "es_arrendatario", nullable = false)
    private Boolean esArrendatario = false;

    @Builder.Default
    @Column(name = "es_apoderado_designado", nullable = false)
    private Boolean esApoderadoDesignado = false;

    @Column(name = "parentesco_o_relacion")
    private String parentesco; // PROPIETARIO, CONYUGE, HIJO, ARRENDATARIO, APODERADO_EXTERNO
}
