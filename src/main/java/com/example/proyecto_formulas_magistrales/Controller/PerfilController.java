package com.example.proyecto_formulas_magistrales.Controller;

import com.example.proyecto_formulas_magistrales.DTO.CambioPasswordPropioDTO;
import com.example.proyecto_formulas_magistrales.DTO.PerfilResponseDTO;
import com.example.proyecto_formulas_magistrales.Service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PerfilController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<PerfilResponseDTO> obtenerMiPerfil(Authentication auth) {
        return ResponseEntity.ok(usuarioService.obtenerMiPerfil(auth));
    }

    @PutMapping("/cambiar-password")
    public ResponseEntity<Map<String, String>> cambiarPassword(
            @Valid @RequestBody CambioPasswordPropioDTO dto,
            Authentication auth) {
        usuarioService.cambiarPasswordPropio(dto, auth);
        return ResponseEntity.ok(Collections.singletonMap("message", "Contraseña actualizada exitosamente"));
    }
}
