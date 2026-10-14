package com.example.proyecto_formulas_magistrales.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pacientes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paciente")
    private Long idPaciente;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombres", length = 100, nullable = false)
    private String nombres;

    @NotBlank
    @Size(max = 100)
    @Column(name = "apellidos", length = 100, nullable = false)
    private String apellidos;

    @NotBlank
    @Size(max = 15)
    @Column(name = "dni", length = 15, nullable = false, unique = true)
    private String dni;

    @NotNull
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Size(max = 20)
    @Column(name = "sexo", length = 20)
    private String sexo;

    @Size(max = 20)
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Size(max = 150)
    @Email
    @Column(name = "correo", length = 150)
    private String correo;

    @Size(max = 255)
    @Column(name = "direccion", length = 255)
    private String direccion;

    @NotNull
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.PERSIST, orphanRemoval = false)
    private List<Cita> citas = new ArrayList<>();

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.PERSIST, orphanRemoval = false)
    private List<Consulta> consultas = new ArrayList<>();
}
    