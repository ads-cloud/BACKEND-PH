package com.ph.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "opciones_voto")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcionVoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Pregunta pregunta;

    @Column(nullable = false)
    private String texto;

    public String getTextoOpcion() {
        return texto;
    }

    @Column(nullable = false)
    private Integer orden;
}
