package com.example.proyecto_formulas_magistrales.Controller;

import com.example.proyecto_formulas_magistrales.DTO.RolCreateDTO;
import com.example.proyecto_formulas_magistrales.DTO.RolResponseDTO;
import com.example.proyecto_formulas_magistrales.Service.RolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    /**
     * Listar todos los roles del sistema (accesible para usuarios autenticados).
     */
    @GetMapping
    public ResponseEntity<List<RolResponseDTO>> listarRoles() {
        return ResponseEntity.ok(rolService.listarTodos());
    }

    /**
     * Obtener rol por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RolResponseDTO> obtenerRol(@PathVariable Long id) {
        return ResponseEntity.ok(rolService.obtenerRol(id));
    }

    /**
     * Registrar un nuevo rol en el catálogo (solo ADMIN).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RolResponseDTO> registrarRol(@Valid @RequestBody RolCreateDTO dto) {
        RolResponseDTO creado = rolService.registrarRol(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Buscar rol por nombre.
     */
    @GetMapping("/buscar/nombre")
    public ResponseEntity<RolResponseDTO> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(rolService.buscarPorNombre(nombre));
    }

    /**
     * Estadísticas: total de roles.
     */
    @GetMapping("/stats/total")
    public ResponseEntity<Map<String, Long>> totalRoles() {
        return ResponseEntity.ok(Collections.singletonMap("total", rolService.totalRoles()));
    }
}
