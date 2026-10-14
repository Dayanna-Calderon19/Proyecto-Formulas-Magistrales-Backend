package com.example.proyecto_formulas_magistrales.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaReprogramarDTO {

    @NotNull(message = "La nueva fecha y hora son obligatorias")
    private LocalDateTime nuevaFechaHora;

    private String motivoReprogramacion;
}
