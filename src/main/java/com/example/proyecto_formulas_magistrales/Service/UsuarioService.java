package com.example.proyecto_formulas_magistrales.Service;

import com.example.proyecto_formulas_magistrales.DTO.CambioPasswordPropioDTO;

import com.example.proyecto_formulas_magistrales.DTO.PasswordResetDTO;
import com.example.proyecto_formulas_magistrales.DTO.PerfilResponseDTO;
import com.example.proyecto_formulas_magistrales.DTO.UsuarioCreateDTO;
import com.example.proyecto_formulas_magistrales.DTO.UsuarioResponseDTO;
import com.example.proyecto_formulas_magistrales.DTO.UsuarioUpdateDTO;
import com.example.proyecto_formulas_magistrales.Model.Cita;
import com.example.proyecto_formulas_magistrales.Model.Medico;
import com.example.proyecto_formulas_magistrales.Model.Rol;
import com.example.proyecto_formulas_magistrales.Model.Usuario;
import com.example.proyecto_formulas_magistrales.Repository.CitaRepository;
import com.example.proyecto_formulas_magistrales.Repository.RolRepository;
import com.example.proyecto_formulas_magistrales.Repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final CitaRepository citaRepository;
    private final PasswordEncoder passwordEncoder;


    // Política de contraseñas: mínimo 8 caracteres, al menos una letra y un dígito
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos(String search, String rol, Boolean activo) {
        List<Usuario> usuarios = usuarioRepository.findAllWithRoles();

        return usuarios.stream()
                .filter(u -> {
                    if (search == null || search.isBlank()) return true;
                    String q = search.trim().toLowerCase();
                    boolean matchUsername = u.getUsername() != null && u.getUsername().toLowerCase().contains(q);
                    boolean matchNombre = u.getNombreCompleto() != null && u.getNombreCompleto().toLowerCase().contains(q);
                    boolean matchCorreo = u.getCorreo() != null && u.getCorreo().toLowerCase().contains(q);
                    return matchUsername || matchNombre || matchCorreo;
                })
                .filter(u -> {
                    if (rol == null || rol.isBlank() || "TODOS".equalsIgnoreCase(rol)) return true;
                    String targetRole = rol.trim().toUpperCase().replace("ROLE_", "");
                    return u.getRoles().stream()
                            .anyMatch(r -> r.getNombre().equalsIgnoreCase(targetRole));
                })
                .filter(u -> {
                    if (activo == null) return true;
                    return activo.equals(u.getActivo());
                })
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario con ID " + id + " no encontrado"));
        return mapToResponseDTO(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario con username '" + username + "' no encontrado"));
        return mapToResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO registrarUsuario(UsuarioCreateDTO dto) {
        String cleanUsername = dto.getUsername().trim();
        String cleanCorreo = dto.getCorreo().trim().toLowerCase();

        if (usuarioRepository.existsByUsername(cleanUsername)) {
            throw new IllegalStateException("El nombre de usuario '" + cleanUsername + "' ya está registrado");
        }
        if (usuarioRepository.existsByCorreo(cleanCorreo)) {
            throw new IllegalStateException("El correo electrónico '" + cleanCorreo + "' ya está registrado");
        }

        validarPoliticaPassword(dto.getPassword());

        Set<Rol> roles = resolverRoles(dto.getRoles());

        Usuario nuevo = new Usuario();
        nuevo.setUsername(cleanUsername);
        nuevo.setNombreCompleto(dto.getNombreCompleto() != null ? dto.getNombreCompleto().trim() : null);
        nuevo.setCorreo(cleanCorreo);
        nuevo.setPassword(passwordEncoder.encode(dto.getPassword()));
        nuevo.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        nuevo.setRoles(roles);

        Usuario guardado = usuarioRepository.save(nuevo);
        return mapToResponseDTO(guardado);
    }

    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Long id, UsuarioUpdateDTO dto) {
        return actualizarUsuario(id, dto, null);
    }

    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Long id, UsuarioUpdateDTO dto, Authentication auth) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario con ID " + id + " no encontrado"));

        String currentUsername = auth != null ? auth.getName() : null;
        boolean esMismoUsuario = currentUsername != null && existente.getUsername().equalsIgnoreCase(currentUsername);
        boolean existenteEsAdmin = existente.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getNombre()));

        // 1. Un administrador no puede desactivar su propia cuenta en sesión
        if (esMismoUsuario && Boolean.FALSE.equals(dto.getActivo())) {
            throw new IllegalStateException("Por motivos de seguridad, no está permitido desactivar tu propia cuenta de usuario en sesión");
        }

        // 2. Un administrador no puede editar los datos ni privilegios de OTRO administrador
        if (existenteEsAdmin && !esMismoUsuario && auth != null) {
            throw new IllegalStateException("Por políticas de seguridad, no está permitido modificar la cuenta ni los privilegios de otro Administrador");
        }

        String cleanUsername = dto.getUsername() != null ? dto.getUsername().trim() : "";
        String cleanCorreo = (dto.getCorreo() != null && !dto.getCorreo().trim().isEmpty())
                ? dto.getCorreo().trim().toLowerCase()
                : null;

        if (cleanUsername.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }

        // Validar unicidad de username solo si ha cambiado
        if (existente.getUsername() == null || !existente.getUsername().equalsIgnoreCase(cleanUsername)) {
            if (usuarioRepository.existsByUsername(cleanUsername)) {
                throw new IllegalStateException("El nombre de usuario '" + cleanUsername + "' ya está en uso");
            }
        }

        // Validar unicidad de correo solo si se proporcionó y ha cambiado
        if (cleanCorreo != null) {
            if (existente.getCorreo() == null || !existente.getCorreo().equalsIgnoreCase(cleanCorreo)) {
                if (usuarioRepository.existsByCorreo(cleanCorreo)) {
                    throw new IllegalStateException("El correo electrónico '" + cleanCorreo + "' ya está en uso");
                }
            }
        }

        Set<Rol> nuevosRoles = resolverRoles(dto.getRoles());

        // 3. Un administrador no puede retirarse a sí mismo el rol de ADMIN
        if (esMismoUsuario && existenteEsAdmin) {
            boolean sigueSiendoAdmin = nuevosRoles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getNombre()));
            if (!sigueSiendoAdmin) {
                throw new IllegalStateException("No está permitido retirar el rol de Administrador a tu propia cuenta en sesión");
            }
        }

        // 4. Regla de autoprotección del último administrador activo
        boolean eraAdmin = existente.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getNombre()));
        boolean seguiraSiendoAdmin = nuevosRoles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getNombre()));
        boolean seguiraActivo = Boolean.TRUE.equals(dto.getActivo());

        if (eraAdmin && Boolean.TRUE.equals(existente.getActivo()) && (!seguiraSiendoAdmin || !seguiraActivo)) {
            long adminsActivos = usuarioRepository.countByRoles_NombreIgnoreCaseAndActivoTrue("ADMIN");
            if (adminsActivos <= 1) {
                throw new IllegalStateException("No es posible desactivar ni retirar el rol de administrador al único administrador activo del sistema");
            }
        }

        // 5. Si se desactiva un usuario vinculado a un médico, verificar que no tenga citas pendientes
        if (Boolean.TRUE.equals(existente.getActivo()) && Boolean.FALSE.equals(dto.getActivo()) && existente.getMedico() != null) {
            validarCitasPendientesDeMedico(existente.getMedico().getIdMedico());
        }

        existente.setUsername(cleanUsername);
        existente.setNombreCompleto(dto.getNombreCompleto() != null ? dto.getNombreCompleto().trim() : null);
        existente.setCorreo(cleanCorreo);
        existente.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        existente.setRoles(nuevosRoles);

        Usuario actualizado = usuarioRepository.save(existente);
        return mapToResponseDTO(actualizado);
    }

    @Transactional
    public void cambiarEstado(Long id, boolean nuevoEstado) {
        cambiarEstado(id, nuevoEstado, null);
    }

    @Transactional
    public void cambiarEstado(Long id, boolean nuevoEstado, Authentication auth) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario con ID " + id + " no encontrado"));

        String currentUsername = auth != null ? auth.getName() : null;
        boolean esMismoUsuario = currentUsername != null && usuario.getUsername().equalsIgnoreCase(currentUsername);
        boolean esAdmin = usuario.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getNombre()));

        if (!nuevoEstado) {
            // No permitir desactivar la propia cuenta en sesión
            if (esMismoUsuario) {
                throw new IllegalStateException("Por motivos de seguridad, no está permitido desactivar tu propia cuenta de usuario en sesión");
            }

            // No permitir que un admin desactive a otro admin
            if (esAdmin && !esMismoUsuario && auth != null) {
                throw new IllegalStateException("Por políticas de seguridad, no está permitido desactivar la cuenta de otro Administrador");
            }

            // Protección al último admin activo
            if (esAdmin && Boolean.TRUE.equals(usuario.getActivo())) {
                long adminsActivos = usuarioRepository.countByRoles_NombreIgnoreCaseAndActivoTrue("ADMIN");
                if (adminsActivos <= 1) {
                    throw new IllegalStateException("No es posible desactivar al único administrador activo del sistema");
                }
            }

            // Validar si el usuario está asociado a un médico con citas pendientes
            if (usuario.getMedico() != null) {
                validarCitasPendientesDeMedico(usuario.getMedico().getIdMedico());
            }
        }

        usuario.setActivo(nuevoEstado);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void restablecerPassword(Long id, PasswordResetDTO dto) {
        restablecerPassword(id, dto, null);
    }

    @Transactional
    public void restablecerPassword(Long id, PasswordResetDTO dto, Authentication auth) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario con ID " + id + " no encontrado"));

        String currentUsername = auth != null ? auth.getName() : null;
        boolean esAdmin = usuario.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getNombre()));
        boolean esMismoUsuario = currentUsername != null && usuario.getUsername().equalsIgnoreCase(currentUsername);

        // Un admin no puede restablecer la contraseña de otro admin
        if (esAdmin && !esMismoUsuario && auth != null) {
            throw new IllegalStateException("Por motivos de seguridad, no está permitido restablecer la contraseña de otro Administrador");
        }

        validarPoliticaPassword(dto.getPassword());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public PerfilResponseDTO obtenerMiPerfil(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("No se encontró información de autenticación válida");
        }

        Usuario usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado: " + auth.getName()));

        Set<String> rolesNames = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toSet());

        Long idMedico = null;
        String nombreMedico = null;
        String dniMedico = null;
        String especialidad = null;
        String colegiatura = null;
        String telefonoMedico = null;
        String correoMedico = null;
        Boolean estadoMedico = null;

        if (usuario.getMedico() != null) {
            Medico m = usuario.getMedico();
            idMedico = m.getIdMedico();
            nombreMedico = (m.getNombres() + " " + m.getApellidos()).trim();
            dniMedico = m.getDni();
            especialidad = m.getEspecialidad();
            colegiatura = m.getColegiatura();
            telefonoMedico = m.getTelefono();
            correoMedico = m.getCorreo();
            estadoMedico = m.getEstado();
        }

        return PerfilResponseDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .correo(usuario.getCorreo())
                .activo(usuario.getActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .roles(rolesNames)
                .idMedico(idMedico)
                .nombreMedico(nombreMedico)
                .dniMedico(dniMedico)
                .especialidad(especialidad)
                .colegiatura(colegiatura)
                .telefonoMedico(telefonoMedico)
                .correoMedico(correoMedico)
                .estadoMedico(estadoMedico)
                .build();
    }

    @Transactional
    public void cambiarPasswordPropio(CambioPasswordPropioDTO dto, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("No se encontró información de autenticación válida");
        }

        Usuario usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado: " + auth.getName()));

        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual ingresada es incorrecta");
        }

        validarPoliticaPassword(dto.getPasswordNuevo());
        usuario.setPassword(passwordEncoder.encode(dto.getPasswordNuevo()));
        usuarioRepository.save(usuario);
    }

    private void validarCitasPendientesDeMedico(Long idMedico) {
        long citasPendientes = citaRepository.countByMedico_IdMedicoAndEstadoIn(
                idMedico,
                List.of(Cita.Estado.PROGRAMADA, Cita.Estado.CONFIRMADA)
        );

        if (citasPendientes > 0) {
            throw new IllegalStateException("No es posible desactivar este usuario porque el médico asociado tiene " +
                    citasPendientes + " cita(s) programada(s) o confirmada(s) pendiente(s). Reasigne o cancele las citas previamente.");
        }
    }


    @Transactional(readOnly = true)
    public long totalUsuarios() {
        return usuarioRepository.count();
    }

    @Transactional(readOnly = true)
    public long totalUsuariosActivos() {
        return usuarioRepository.countByRoles_NombreIgnoreCaseAndActivoTrue("ADMIN")
                + usuarioRepository.countByRoles_NombreIgnoreCaseAndActivoTrue("MEDICO");
    }

    private void validarPoliticaPassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres y contener al menos una letra y un número");
        }
    }

    private Set<Rol> resolverRoles(Set<String> nombresRoles) {
        if (nombresRoles == null || nombresRoles.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un rol válido");
        }

        Set<Rol> roles = new HashSet<>();
        for (String nombre : nombresRoles) {
            String clean = nombre.trim().toUpperCase().replace("ROLE_", "");
            Rol rol = rolRepository.findByNombre(clean)
                    .orElseThrow(() -> new IllegalArgumentException("El rol '" + clean + "' no existe en el catálogo"));
            roles.add(rol);
        }
        return roles;
    }

    public UsuarioResponseDTO mapToResponseDTO(Usuario usuario) {
        Set<String> rolesNames = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toSet());

        Long idMedico = null;
        String nombreMedico = null;
        if (usuario.getMedico() != null) {
            idMedico = usuario.getMedico().getIdMedico();
            nombreMedico = usuario.getMedico().getNombres() + " " + usuario.getMedico().getApellidos();
        }

        return UsuarioResponseDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombreCompleto())
                .correo(usuario.getCorreo())
                .activo(usuario.getActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .roles(rolesNames)
                .idMedico(idMedico)
                .nombreMedico(nombreMedico)
                .build();
    }
}
