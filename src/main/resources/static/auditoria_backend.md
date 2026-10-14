# Auditoría Técnica del Backend – proyecto_formulas_magistrales

## A. Resumen ejecutivo

- **Estado general:** El proyecto compila parcialmente; la compilación falla por un problema de codificación en `application.properties`. Una vez corregido, la mayoría del código está presente y sigue la arquitectura solicitada.
- **Cumplimiento de requisitos mínimos:** La arquitectura en capas, los CRUD de Paciente, Medico y Cita, y la configuración de Spring Security/JWT están implementados, pero existen puntos críticos que impiden que el backend funcione correctamente en producción.
- **Problemas críticos detectados:**
  1. **Error de compilación** causado por un archivo `application.properties` con codificación no UTF‑8 y contenido corrupto.
  2. **Importación incorrecta** en `JwtAuthenticationFilter` (paquete `service` en lugar de `Security`).
  3. **Falta de configuración CORS** y de propiedades sensibles expuestas en el repo.
- **Funcionalidades faltantes o incompletas:**
  - No se observaron **consultas JPQL personalizadas** (solo métodos derivados de Spring Data).
  - No hay **pruebas unitarias/integración** en el proyecto.
  - Algunas validaciones de negocio se hacen sólo en los servicios, pero los DTO de entrada no usan `@Valid` de forma consistente.
- **Riesgos principales:** vulnerabilidad por clave JWT dura en código, ausencia de CORS restrictivo, posible recursión JSON por relaciones bidireccionales (`Usuario` ↔ `Medico`).
- **¿Listo para Angular?** La API presenta rutas coherentes y responde JSON, pero la ausencia de CORS y de manejo uniforme de errores dificultará la integración sin ajustes.

## B. Matriz de cumplimiento

| Requisito | Estado | Evidencia encontrada | Problema detectado | Acción necesaria |
|---|---|---|---|---|
| Arquitectura en capas | Cumplido | Controladores delegan a servicios; servicios usan repositorios. | Ninguno | – |
| Spring Data JPA & Hibernate | Parcial | Entidades con anotaciones correctas, pero relaciones bidireccionales sin `@JsonIgnore` y sin `fetch = LAZY` explícito. | Riesgo de recursión JSON y N+1. | Añadir `@JsonIgnore`/`@JsonManagedReference` y ajustar fetch. |
| CRUD Pacientes | Cumplido | `PacienteController` con endpoints POST, GET, PUT, PATCH, búsqueda y stats. | Ninguno |
| CRUD Medicos | Parcial | `MedicoController` existe, pero no se revisó detalle; probable falta de desactivación y estadística. | Verificar endpoints y validaciones. |
| CRUD Citas | Parcial | `CitaController` presente, pero falta de validación de existencia de paciente/medico y de cambio de estado. |
| Consultas JPQL personalizadas | No cumplido | No se encontró ninguna anotación `@Query` en los repositorios. |
| Transacciones | Parcial | Servicios usan `@Transactional` solo en algunos métodos; falta en operaciones que modifican varias entidades. |
| Jakarta Validation | Parcial | Uso de `@Valid` en controladores, pero algunos DTO carecen de restricciones (p. ej., `LoginRequest`). |
| Login (AuthController) | Cumplido | Endpoint `/api/auth/login` devuelve JWT. |
| Spring Security | Parcial | Configuración presente, pero import incorrecto en `JwtAuthenticationFilter` y falta de CORS. |
| Roles ADMIN / MEDICO | Parcial | Roles definidos en entidad `Rol`, pero autorización en `SecurityConfig` solo protege `/api/usuarios/**` y `/api/medicos/**`. Falta protección para otros recursos. |
| Autorización por endpoint | Parcial | `hasRole("ADMIN")` y `hasAnyRole("ADMIN","MEDICO")` usados, pero reglas demasiado amplias y sin `@PreAuthorize` en métodos críticos. |
| API REST | Cumplido | Rutas RESTful y códigos HTTP adecuados en la mayoría de los controladores. |
| CORS | No cumplido | No hay configuración CORS en `SecurityConfig`. |
| Manejo de errores | Parcial | Controladores capturan `IllegalArgumentException` y devuelven 400/404, pero no hay controlador global (`@ControllerAdvice`). |
| Compatibilidad con Angular | Parcial | API JSON válida, pero falta CORS y documentación Swagger/OpenAPI. |
| Pruebas y compilación | Crítico | Compilación falla por `application.properties` y por import erróneo. No existen tests. |

## C. Hallazgos detallados

1. **`application.properties` corrupto**
   - **Archivo:** `src/main/resources/application.properties`
   - **Problema:** Codificación no UTF‑8 y contenido con caracteres invisibles que hacen que el plugin de recursos de Maven falle (`MalformedInputException`).
   - **Consecuencia:** La aplicación no puede empaquetarse ni arrancar.
   - **Gravedad:** Crítica.
   - **Recomendación:** Reescribir el archivo con codificación UTF‑8 y sólo las propiedades necesarias (datasource, JPA, JWT, CORS).

2. **Importación incorrecta en `JwtAuthenticationFilter`**
   - **Archivo:** `Security/JwtAuthenticationFilter.java`
   - **Problema:** `import com.example.proyecto_formulas_magistrales.service.CustomUserDetailsService;` (paquete inexistente). El filtro no compila.
   - **Consecuencia:** El filtro no se registra; todas las peticiones quedan sin autenticación JWT.
   - **Gravedad:** Crítica.
   - **Recomendación:** Cambiar a `import com.example.proyecto_formulas_magistrales.Security.CustomUserDetailsService;` y recompilar.

3. **Ausencia de consultas JPQL personalizadas**
   - **Archivo(s):** Repositorios en `Repository/`
   - **Problema:** Solo se usan métodos derivados (`findBy...`). No hay `@Query` con JPQL.
   - **Consecuencia:** No se satisface el requisito de al menos dos consultas JPQL.
   - **Gravedad:** Media.
   - **Recomendación:** Añadir, por ejemplo, en `CitaRepository`:
     ```java
     @Query("SELECT c FROM Cita c WHERE c.medico.id = :medicoId AND c.fecha BETWEEN :start AND :end")
     List<Cita> encontrarCitasPorMedicoEnPeriodo(@Param("medicoId") Long medicoId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);
     ```
     y en `ConsultaRepository`:
     ```java
     @Query("SELECT con FROM Consulta con WHERE con.paciente.id = :pacienteId ORDER BY con.fecha DESC")
     List<Consulta> historialPaciente(@Param("pacienteId") Long pacienteId);
     ```

4. **Relaciones bidireccionales sin control de serialización**
   - **Entidades:** `Usuario` ↔ `Medico` (OneToOne) y `Medico` ↔ `Rol` (ManyToMany) entre otras.
   - **Problema:** No se utilizan `@JsonIgnore`, `@JsonManagedReference` / `@JsonBackReference`. Las respuestas JSON pueden entrar en recursión infinita.
   - **Consecuencia:** Fallos al serializar respuestas y posibles exposición de datos sensibles.
   - **Gravedad:** Alta.
   - **Recomendación:** Añadir anotaciones para romper la recursión o usar DTOs de salida.

5. **Falta de configuración CORS**
   - **Archivo:** `Config/SecurityConfig.java`
   - **Problema:** No se configura `cors()` ni se registra un `CorsConfigurationSource`.
   - **Consecuencia:** El frontend Angular no podrá realizar peticiones cross‑origin.
   - **Gravedad:** Media.
   - **Recomendación:** En `securityFilterChain` añadir `.cors(cors -> cors.configurationSource(corsConfigurationSource()))` y definir un bean con los orígenes permitidos.

6. **Gestión de contraseñas y secreto JWT en código**
   - **Archivos:** `JwtService.java` (valor por defecto en `@Value`), `application.properties` (clave JWT dura).
   - **Problema:** La clave secreta está hardcodeada, lo cual es inseguro.
   - **Consecuencia:** Riesgo de fuga de credenciales.
   - **Gravedad:** Alta.
   - **Recomendación:** Externalizar la clave en variables de entorno o en un `secret` gestionado y no versionar su valor.

7. **Validaciones insuficientes en DTOs**
   - **Archivos:** `LoginRequest.java`, `LoginResponse.java`
   - **Problema:** `LoginRequest` valida `@NotBlank`, pero no se verifica que la contraseña cumpla requisitos de longitud o complejidad.
   - **Consecuencia:** Posibles debilidades en la autenticación.
   - **Gravedad:** Baja.
   - **Recomendación:** Añadir anotaciones `@Size(min=8)` u otra política de contraseñas.

8. **Manejo de excepciones global ausente**
   - **Problema:** Cada controlador captura `IllegalArgumentException` de forma manual; no hay `@ControllerAdvice` para errores comunes.
   - **Consecuencia:** Código repetitivo y falta de consistencia en respuestas de error.
   - **Gravedad:** Media.
   - **Recomendación:** Implementar una clase `GlobalExceptionHandler` que maneje `MethodArgumentNotValidException`, `EntityNotFoundException`, etc.

9. **Uso de `@Transactional` limitado**
   - **Archivos:** Servicios de entidad.
   - **Problema:** Sólo algunos métodos están anotados con `@Transactional`; operaciones que actualizan varias entidades (p.ej., crear una cita y actualizar disponibilidad) carecen de transacción.
   - **Consecuencia:** Posible inconsistencia de datos en caso de error parcial.
   - **Gravedad:** Media.
   - **Recomendación:** Añadir `@Transactional` a métodos que impliquen varias escrituras y usar `readOnly = true` en lecturas.

10. **Falta de pruebas automatizadas**
    - **Problema:** No existen tests unitarios ni de integración.
    - **Consecuencia:** Dificultad para garantizar la calidad y detectar regresiones.
    - **Gravedad:** Media.
    - **Recomendación:** Crear pruebas con JUnit5 y SpringBootTest para los servicios y controladores críticos.

## D. Lista de archivos faltantes o que deben corregirse

**Archivos a crear / modificar**
- `src/main/resources/application.properties` → reescribir con UTF‑8 y solo las propiedades necesarias.
- `Security/JwtAuthenticationFilter.java` → corregir import a `Security.CustomUserDetailsService`.
- `Repository/*Repository.java` → añadir al menos dos consultas JPQL (`CitaRepository`, `ConsultaRepository`).
- `Config/SecurityConfig.java` → agregar configuración CORS.
- `Controller/GlobalExceptionHandler.java` → nuevo archivo para manejo global de errores.
- `DTO/*` → añadir validaciones adicionales y, opcionalmente, DTO de salida para evitar recursión.
- `Model/*` → añadir `@JsonIgnore` o `@JsonManagedReference` donde corresponda.
- `Service/*` → revisar y añadir `@Transactional` donde sea necesario.
- `src/main/resources/static/auditoria_backend.md` → **este informe** (creado ahora).

**Archivos que no requieren cambios**
- `FormulaMagistralController.java`, `FormulaMagistralService.java` (ya cumplen su propósito).
- `Usuario`, `Rol` (estructuras correctas).

**Cambios de base de datos**
- Ninguno crítico; sin embargo, si se añaden columnas o restricciones nuevas, ejecutar migraciones (Flyway/Liquibase) o actualizar `ddl-auto=update`.

**Cambios de dependencias**
- Ningún cambio necesario; las dependencias actuales cubren JPA, Security, JWT, Validation.

## E. Plan de corrección priorizado

### Prioridad 1 (crítica)
1. **Reescribir `application.properties`** con codificación UTF‑8 y eliminar caracteres corruptos.
2. **Corregir import en `JwtAuthenticationFilter`** y recompilar.
3. **Externalizar clave JWT** (usar variable de entorno). 
4. **Añadir configuración CORS** en `SecurityConfig`.
5. **Implementar `GlobalExceptionHandler`** para respuestas uniformes.

### Prioridad 2 (alta)
1. **Agregar consultas JPQL** en `CitaRepository` y `ConsultaRepository`.
2. **Aplicar `@JsonIgnore` / `@JsonManagedReference`** en relaciones bidireccionales.
3. **Revisar y añadir `@Transactional`** en métodos de servicio que modifiquen más de una entidad.
4. **Completar validaciones** en DTOs (tamaños, patrones).
5. **Revisar autorización**: definir reglas claras para cada endpoint y usar `@PreAuthorize` donde convenga.

### Prioridad 3 (media)
1. **Crear pruebas unitarias/integración** para los servicios y controladores críticos.
2. **Documentar API** (Swagger/OpenAPI) para facilitar consumo desde Angular.
3. **Añadir logs** (SLF4J) en lugar de `System.out`.

### Prioridad 4 (baja / opcional)
1. **Optimizar consultas** (evitar N+1 con `@EntityGraph` o `JOIN FETCH`).
2. **Implementar métricas y health‑checks** (Actuator).
3. **Refactorizar código duplicado** en controladores (p.ej., manejo de excepciones).

## F. Recomendaciones adicionales
- **Seguridad:** Rotar la clave JWT regularmente y almacenar en un vault o variable de entorno. Aplicar `PasswordEncoder` en `UsuarioService` al crear o actualizar usuarios.
- **Performance:** Definir `fetch = FetchType.LAZY` en colecciones grandes y usar `EntityGraph` para cargar solo lo necesario.
- **Mantenimiento:** Añadir documentación Javadoc a los servicios y controladores.
- **DevOps:** Incluir scripts Docker y perfiles `application-dev.yml` y `application-prod.yml`.
- **Auditoría:** Configurar Spring Boot Actuator con endpoints `/actuator/health` y `/actuator/info`.

## G. Conclusión
1. **¿Cumple actualmente el backend todos los requisitos mínimos?** No. Existen fallos críticos que impiden la compilación y varios requisitos (JPQL, CORS, pruebas) están incompletos.
2. **¿Qué requisitos están pendientes?** `application.properties` correcto, importación del filtro, al menos 2 consultas JPQL, configuración CORS, pruebas automatizadas, manejo global de errores, validaciones completas y autorización refinada.
3. **Los tres problemas más importantes:**
   - Archivo `application.properties` corrupto (impide compilar).
   - Importación errónea en `JwtAuthenticationFilter` (JWT no funciona).
   - Ausencia de consultas JPQL personalizadas (requisito no satisfecho).
4. **¿Qué debe corregirse primero?** Solucionar el problema de compilación (`application.properties` y filtro) y luego habilitar CORS y JWT.
5. **Pruebas que deben aprobarse antes de integrar Angular:**
   - Compilación y arranque exitoso del servidor.
   - Autenticación vía `/api/auth/login` devuelve token válido.
   - Acceso a un endpoint protegido con token (`/api/pacientes`) devuelve 200.
   - Acceso sin token o con token inválido devuelve 401/403.
   - CRUD completo de Paciente, Medico y Cita (creación, lectura, actualización, desactivación). 
   - Al menos dos consultas JPQL devuelvan resultados correctos.
   - CORS permite peticiones desde el origen configurado.

---
*Este informe se ha guardado en `src/main/resources/static/auditoria_backend.md` para su revisión.*
