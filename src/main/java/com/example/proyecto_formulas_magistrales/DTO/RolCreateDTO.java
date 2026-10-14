package com.example.proyecto_formulas_magistrales.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolCreateDTO {

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(min = 2, max = 30, message = "El nombre del rol debe tener entre 2 y 30 caracteres")
    private String nombre;
}
