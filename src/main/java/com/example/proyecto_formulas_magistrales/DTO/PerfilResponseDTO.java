package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilResponseDTO {

    private Long idUsuario;
    private String username;
    private String nombreCompleto;
    private String correo;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private Set<String> roles;

    // Información médica clínica si está vinculado a un médico
    private Long idMedico;
    private String nombreMedico;
    private String dniMedico;
    private String especialidad;
    private String colegiatura;
    private String telefonoMedico;
    private String correoMedico;
    private Boolean estadoMedico;
}
