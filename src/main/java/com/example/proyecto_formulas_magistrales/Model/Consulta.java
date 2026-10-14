package com.example.proyecto_formulas_magistrales.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consultas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consulta")
    private Long idConsulta;

    @NotNull
    @Column(name = "fecha_consulta", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime fechaConsulta = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (fechaConsulta == null) {
            fechaConsulta = LocalDateTime.now();
        }
    }

    @NotBlank
    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @NotBlank
    @Column(name = "diagnostico", nullable = false, columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "tratamiento", columnDefinition = "TEXT")
    private String tratamiento;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @OneToOne
    @JoinColumn(name = "id_cita", unique = true)
    private Cita cita;

    @ManyToOne
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "id_medico", nullable = false)
    private Medico medico;

    @OneToMany(mappedBy = "consulta", cascade = CascadeType.PERSIST, orphanRemoval = false)
    private List<FormulaMagistral> formulas = new ArrayList<>();

}
