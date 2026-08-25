package com.ph.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unidades_privadas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"copropiedad_id", "torre", "numeroUnidad"})
}, indexes = {
    @Index(name = "idx_up_copropiedad_id", columnList = "copropiedad_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UnidadPrivada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copropiedad_id", nullable = false)
    private Copropiedad copropiedad;

    @Column(nullable = false)
    private String torre;

    @Column(nullable = false)
    private String numeroUnidad;

    @Column(nullable = false)
    private Double coeficiente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id")
    private Persona propietario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apoderado_usuario_id")
    private Usuario apoderado;

    @Builder.Default
    @Column(name = "poder_aprobado", nullable = false)
    private Boolean poderAprobado = false;
}
