package com.example.proyecto_formulas_magistrales.Controller;

import com.example.proyecto_formulas_magistrales.DTO.PasswordResetDTO;
import com.example.proyecto_formulas_magistrales.DTO.UsuarioCreateDTO;
import com.example.proyecto_formulas_magistrales.DTO.UsuarioResponseDTO;
import com.example.proyecto_formulas_magistrales.DTO.UsuarioUpdateDTO;
import com.example.proyecto_formulas_magistrales.Service.UsuarioService;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Listar o filtrar usuarios con parámetros opcionales.
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(usuarioService.listarTodos(search, rol, activo));
    }

    /**
     * Obtener usuario por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuario(id));
    }

    /**
     * Buscar usuario por nombre de usuario.
     */
    @GetMapping("/buscar/username")
    public ResponseEntity<UsuarioResponseDTO> buscarPorUsername(@RequestParam String username) {
        return ResponseEntity.ok(usuarioService.buscarPorUsername(username));
    }

    /**
     * Registrar un nuevo usuario con credenciales y roles validados.
     */
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> registrarUsuario(@Valid @RequestBody UsuarioCreateDTO dto) {
        UsuarioResponseDTO creado = usuarioService.registrarUsuario(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Actualizar datos generales y roles de un usuario existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateDTO dto,
            org.springframework.security.core.Authentication auth) {
        UsuarioResponseDTO actualizado = usuarioService.actualizarUsuario(id, dto, auth);
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Activar la cuenta de un usuario.
     */
    @PatchMapping("/{id}/activar")
    public ResponseEntity<Map<String, String>> activarUsuario(
            @PathVariable Long id,
            org.springframework.security.core.Authentication auth) {
        usuarioService.cambiarEstado(id, true, auth);
        return ResponseEntity.ok(Collections.singletonMap("message", "Usuario activado con éxito"));
    }

    /**
     * Desactivar la cuenta de un usuario con validación de autoprotección.
     */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Map<String, String>> desactivarUsuario(
            @PathVariable Long id,
            org.springframework.security.core.Authentication auth) {
        usuarioService.cambiarEstado(id, false, auth);
        return ResponseEntity.ok(Collections.singletonMap("message", "Usuario desactivado con éxito"));
    }

    /**
     * Restablecer contraseña mediante flujo independiente.
     */
    @PatchMapping("/{id}/password")
    public ResponseEntity<Map<String, String>> restablecerPassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordResetDTO dto,
            org.springframework.security.core.Authentication auth) {
        usuarioService.restablecerPassword(id, dto, auth);
        return ResponseEntity.ok(Collections.singletonMap("message", "Contraseña actualizada exitosamente"));
    }


    /**
     * Total de usuarios registrados.
     */
    @GetMapping("/stats/total")
    public ResponseEntity<Map<String, Long>> totalUsuarios() {
        return ResponseEntity.ok(Collections.singletonMap("total", usuarioService.totalUsuarios()));
    }

    /**
     * Total de usuarios activos.
     */
    @GetMapping("/stats/activos")
    public ResponseEntity<Map<String, Long>> totalUsuariosActivos() {
        return ResponseEntity.ok(Collections.singletonMap("totalActivos", usuarioService.totalUsuariosActivos()));
    }
}
