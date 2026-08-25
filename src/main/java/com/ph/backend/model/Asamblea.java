package com.ph.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "asambleas", indexes = {
    @Index(name = "idx_asamblea_cop_estado", columnList = "copropiedad_id, estado"),
    @Index(name = "idx_asamblea_fecha", columnList = "fecha")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Asamblea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copropiedad_id", nullable = false)
    private Copropiedad copropiedad;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AsambleaType tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AsambleaStatus estado;

    @Builder.Default
    @Column(nullable = false)
    private Boolean registroCerrado = false;

    @Builder.Default
    @Column(name = "orden_dia_definitivo")
    private Boolean ordenDiaDefinitivo = false;
}
