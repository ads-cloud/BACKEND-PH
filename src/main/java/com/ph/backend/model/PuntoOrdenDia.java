package com.ph.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "puntos_orden_dia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PuntoOrdenDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asamblea_id", nullable = false)
    private Asamblea asamblea;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean completado = false;

    private LocalDateTime fechaCompletado;
}
