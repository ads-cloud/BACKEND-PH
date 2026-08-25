package com.ph.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "preguntas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asamblea_id", nullable = false)
    private Asamblea asamblea;

    @Column(nullable = false, length = 1000)
    private String enunciado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreguntaType tipoMayoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreguntaStatus estado;

    @Column(name = "duracion_minutos")
    @Builder.Default
    private Integer duracionMinutos = 5;

    @Column(name = "fecha_hora_apertura")
    private java.time.LocalDateTime fechaHoraApertura;

    @Column(name = "fecha_hora_cierre")
    private java.time.LocalDateTime fechaHoraCierre;

    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<OpcionVoto> opciones = new ArrayList<>();
}
