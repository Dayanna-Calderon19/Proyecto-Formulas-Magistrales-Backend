package com.example.proyecto_formulas_magistrales.Repository;

import com.example.proyecto_formulas_magistrales.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar usuario por nombre de usuario
    Optional<Usuario> findByUsername(String username);

    // Verificar existencia de nombre de usuario
    boolean existsByUsername(String username);
    // Buscar por correo electrónico
    Optional<Usuario> findByCorreo(String correo);
    // Verificar si el correo electrónico ya existe
    boolean existsByCorreo(String correo);
    // Contar usuarios activos con un determinado rol (ej. ADMIN)
    long countByRoles_NombreIgnoreCaseAndActivoTrue(String nombreRol);

    long countByActivoTrue();


    // Búsqueda por coincidencia en username o correo
    List<Usuario> findByUsernameContainingIgnoreCaseOrCorreoContainingIgnoreCase(String username, String correo);

    // Consulta optimizada para listar usuarios junto con sus roles
    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.roles")
    List<Usuario> findAllWithRoles();

    // Buscar usuarios con rol MEDICO que aún no estén vinculados a un médico
    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.roles r WHERE UPPER(r.nombre) = 'MEDICO' AND u.medico IS NULL AND u.activo = true ORDER BY u.username ASC")
    List<Usuario> findMedicosSinAsignar();
}

