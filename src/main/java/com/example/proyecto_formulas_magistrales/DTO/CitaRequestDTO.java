package com.example.proyecto_formulas_magistrales.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaRequestDTO {

    @NotNull(message = "El paciente es obligatorio")
    private Long idPaciente;

    // Opcional para médicos (se deriva de su usuario autenticado); obligatorio para ADMIN
    private Long idMedico;

    @NotNull(message = "La fecha y hora de la cita son obligatorias")
    private LocalDateTime fechaHora;

    @NotBlank(message = "El motivo de la cita es obligatorio")
    @Size(min = 3, max = 255, message = "El motivo debe tener entre 3 y 255 caracteres")
    private String motivo;

    @Builder.Default
    private String estado = "PROGRAMADA";

    private String observaciones;
}
