package com.ph.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "votos_emitidos", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"pregunta_id", "unidad_privada_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotoEmitido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcion_voto_id", nullable = false)
    private OpcionVoto opcionVoto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_privada_id", nullable = false)
    private UnidadPrivada unidadPrivada;

    @Column(nullable = false)
    private Double coeficienteAplicado;

    @Column(nullable = false)
    private LocalDateTime fechaHoraVoto;
}
