package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaResponseDTO {

    private Long idCita;
    private LocalDateTime fechaHora;
    private String motivo;
    private String estado;
    private String observaciones;

    // Paciente
    private Long idPaciente;
    private String nombrePaciente;
    private String dniPaciente;
    private String telefonoPaciente;

    // Médico
    private Long idMedico;
    private String nombreMedico;
    private String especialidadMedico;
    private String colegiaturaMedico;

    // Consulta vinculada si ya fue atendida
    private Long idConsulta;
}
