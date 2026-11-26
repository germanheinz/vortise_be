# 020 — Plan técnico

Requisitos en [spec](spec.md). El estado y la evidencia se registran en [tasks](tasks.md).

## Hallazgos verificados

| # | Hallazgo | Evidencia | Impacto |
|---|---|---|---|
| H1 | `InvitacionUsuarioService.aceptar` y `aceptarConIdentidad` repiten unas 20 líneas: bloqueo, disponibilidad, conflicto, límite, alta, obras, consumo, auditoría y sesión. Además, `disponible(token)` se consulta dos veces por aceptación. | `service/InvitacionUsuarioService.java:97-150` | Alto |
| H2 | `obrasValidas` y `obrasDeLaEmpresa` repiten el predicado "la obra pertenece a la empresa". | `service/InvitacionUsuarioService.java:160-182` | Medio |
| H3 | Las dos invitaciones (usuario y equipo) duplican varias piezas: hash más búsqueda disponible, `noDisponible()`, el mensaje de conflicto (4 copias), la normalización de email y la URL del frontend (5 copias de `replaceAll("/+$")`). | `InvitacionUsuarioService.java:184-193`, `InvitacionesService.java:108-132` | Medio |
| H4 | `AutenticacionService.sesion` y `me` repiten el plan efectivo, la empresa inicial y las obras. | `service/authentication/AutenticacionService.java:69-92` | Medio |
| H5 | Se repite 40 veces el lambda `orElseThrow(() -> new RecursoNoEncontradoException(...))` y 21 veces la comprobación "no pertenece a la empresa". `EmpresaConfiguracionService` concentra 9 y 7. | `service/EmpresaConfiguracionService.java:68-128` | Medio |
| H6 | 12 clases de servicio usan `AccessDeniedException` de Spring Security, y `ReglaDeEstadoException` hereda de ella. Acopla la capa de aplicación al framework de seguridad. | `service/EquipoAccessPolicy.java:7,60-64`, `application/exception/ReglaDeEstadoException.java` | Medio |
| H7 | `@PreAuthorize("hasRole('ADMINISTRADOR')")` se repite 13 veces en `EmpresaConfiguracionController`. `InvitacionAdministracionController` mezcla la expresión literal con `Guards.SOLO_ADMINISTRADOR`. | `controller/EmpresaConfiguracionController.java`, `controller/authentication/InvitacionAdministracionController.java` | Bajo |
| H8 | `service/` tiene 19 clases planas, mientras que `authentication/` y `pagos/` ya están agrupadas. Empresa tiene 4 servicios (`EmpresaService`, `EmpresaConsultaService`, `EmpresaConfiguracionService`, `ConfiguracionEmpresaObraService`) sin un paquete común. | `service/` | Medio |
| H9 | Las reglas están repartidas sin criterio: `RegistroHorasRules` y `CalculoCertificado` están en `application/helper`, mientras que `EstructuraRubros` y `PrevistoRealCalculator` están en `domain/rule`. | `application/helper/`, `domain/rule/` | Bajo |
| H10 | `PlantaTestIsolationConfiguration` y `PlantaTestIsolationPolicy` solo sirven a pruebas, pero viven en `src/main`. | `infrastructure/config/` | Bajo |
| H11 | `UsuarioEmpresa.codigoInvitacion` e `invitacionExpiraEn` no tienen uso tras Clerk. | `model/UsuarioEmpresa.java:40-43`, `V30__clerk_integration.sql` | Bajo |
| H12 | Hay 71 DTO planos en `application/dto`. | `application/dto/` | Bajo |
| H13 | El login con contraseña sigue vivo detrás del flag `app.auth.password-login.enabled`. Lo forman `AutenticacionService.login`, `LoginThrottle`, `PasswordReset*`, `aceptar` con contraseña y `PasswordPolicy`, y el e2e lo usa. | `controller/authentication/AuthController.java:45-54`, `application-e2e-local.yml` | Alto (requiere decisión) |

Descartado tras verificar:

- `repository/` frente a `infrastructure/persistence/` no es duplicación. Son el contrato y el adaptador de Spring Data, una frontera intencional de 017.
- `RefreshTokenService` forma parte de la sesión actual (`SesionService.abrir`) y se mantiene.

## Diseño propuesto

### C1 — Invitaciones (H1, H2, H3)

- Nuevo paquete `service/invitacion/` con `InvitacionUsuarioService`, `InvitacionesService` (equipo) y un componente común `InvitacionSoporte`.
  - `InvitacionSoporte` concentra el hash del token, el "no disponible", la normalización de email y el mensaje de conflicto.
  - `FrontendUrl` es un `@Component` que se inyecta con `app.frontend-url` ya normalizado. Lo usan las invitaciones y `PasswordResetService`.
- `InvitacionUsuarioService` adopta el mismo patrón que ya usa `InvitacionesService.aceptarComoColaborador`.
  - Un único `aceptarConCuenta(token, validarCredencial, crearUsuario)`.
  - Ese método resuelve la invitación una sola vez antes del bloqueo y conserva el orden empresa → invitación.
- Un único `obrasDeLaEmpresa(empresaId, ids)` con el predicado compartido.
  - La creación lanza un error si hay diferencias.
  - La aceptación filtra las obras que ya no pertenecen a la empresa.
- No se fusionan las dos clases de invitación. Sus reglas son distintas: rol fijo y email obligatorio en un caso, colaborador multiempresa con Pro en el otro.

### C2 — Sesión (H4)

- `AutenticacionService` gana un método privado `contextoDeSesion(usuario)` que devuelve un record con el plan, la empresa y las obras. Lo usan `sesion` y `me`.
- Si en la decisión D1 se elimina el login con contraseña, `AutenticacionService` pasa a llamarse `SesionUsuarioService` (o se integra en `SesionService`), porque ya no autentica.

### C3 — Búsqueda y propiedad por empresa (H5)

- Se crea `application/helper/Recursos` con dos utilidades estáticas:
  - `encontrado(Optional<T>, String recurso)`, que lanza `RecursoNoEncontradoException("<recurso> no encontrado")`;
  - `deEmpresa(T, Function<T, Long> empresaId, Long empresaId, String mensaje)`.
- Primero se aplica en `EmpresaConfiguracionService`. Después en los demás servicios, uno por commit.
- Se conservan exactamente los mensajes actuales: el helper recibe el texto, no lo genera.

### C4 — Excepciones de aplicación (H6)

- Nueva excepción `application/exception/OperacionNoPermitidaException` (403). `ReglaDeEstadoException` la extiende en lugar de `AccessDeniedException`.
- `GlobalExceptionHandler` la mapea al mismo cuerpo 403 que hoy produce `AccessDeniedException`.
- Se reemplaza en los 12 servicios.
- Se verifica que `TenantAccessInterceptor` y Spring Security siguen recibiendo `AccessDeniedException` donde son ellos quienes la lanzan.

### C5 — Agrupación de paquetes (H8, H9, H12)

```
service/
  authentication/   (sin cambios)
  pagos/            (sin cambios)
  invitacion/       InvitacionUsuarioService, InvitacionesService, InvitacionSoporte
  equipo/           EquipoAccessPolicy, UsuarioEmpresaService
  empresa/          EmpresaService, EmpresaConsultaService, EmpresaConfiguracionService, ConfiguracionEmpresaObraService
  proyecto/         ProyectoService, ProyectoEdicionService, ObraTareaService, PlantaService, RegistroHorasService, PrevistoRealService
  certificado/      CertificadoObraService, CertificadoBloqueos
  restriccion/      RestriccionService
  reporte/          ReporteObraService
  superadmin/       SuperAdminConsultaService
```

- Los controllers siguen la misma agrupación.
- Los DTO se agrupan por funcionalidad en `application/dto/<feature>`. Es solo un cambio de paquete: los records y los nombres JSON no cambian.
- Criterio de reglas: `domain/rule` contiene lógica pura sin repositorios ni Spring. `application/helper` contiene utilidades que coordinan entidades o repositorios. `RegistroHorasRules` y `CalculoCertificado` se mueven solo si cumplen el criterio de reglas puras.
- No se fusionan servicios grandes por tamaño. `CertificadoObraService` (382 líneas) y `ProyectoEdicionService` (307 líneas) se dividen solo si se encuentran responsabilidades independientes durante C3. Si es así, se propone en un corte aparte.
- Los movimientos se hacen con refactor de IDE o `git mv`, en un commit por paquete y sin cambios de lógica.

### C6 — Guardas de controllers (H7)

- En `EmpresaConfiguracionController`, la anotación `@PreAuthorize(Guards.SOLO_ADMINISTRADOR)` pasa a nivel de clase. Las lecturas que hoy no la tienen se dejan explícitas a nivel de método. Antes hay que comprobar que todas las rutas del controller exigen administrador.
- `InvitacionAdministracionController` usa `Guards.*` en todas sus rutas.
- `WriteEndpointsGuardTest` debe seguir pasando.

### C7 — Limpieza (H10, H11)

- Las clases `PlantaTestIsolation*` se mueven a `src/test`, si el perfil solo se activa desde pruebas.
- Se eliminan los campos, accesores y el índice JPA sin uso de `UsuarioEmpresa`. La columna en la base no se toca.
- Se hace un inventario de métodos de repositorio sin referencias, comprobando cada candidato con grep sobre `src/main` y `src/test`, y se eliminan.

## Decisiones abiertas

- **D1 — Login con contraseña (H13).** Hay dos opciones:
  - **a)** Mantenerlo detrás del flag. Es la opción actual y no reduce código.
  - **b)** Eliminarlo. Se van unas 10 clases, entre ellas `AutenticacionService.login`, `LoginThrottle`, `PasswordResetService`, `PasswordResetTransactions`, `PasswordResetController`, `LoginConContrasenaPolicy`, los endpoints `aceptar` con contraseña, `LoginRequestDto` y las pruebas asociadas.

  La opción b) obliga a migrar el e2e a `@clerk/testing` en `gestion_fe` y a decidir qué pasa con las tablas de reset y throttle. Recomiendo b), pero como corte separado (020-b), después de C1 a C7.
- **D2 — Columnas `codigo_invitacion` e `invitacion_expira_en`.** Su baja requiere una migración Flyway, que es una operación de datos y necesita aprobación explícita. Se propone dejarlas para un corte posterior.

## Orden y verificación

0. **Línea base.**
   - Ejecutar `mvn verify` antes de tocar nada. El refactor 017 dejó pendiente la integración con Spring; sin línea base no se puede distinguir una regresión de una falla previa.
   - Si falla, se reporta y se decide antes de seguir.
1. C4 (excepciones): es de bajo riesgo y deja el terreno listo para mover paquetes.
2. C1 (invitaciones) y C2 (sesión).
3. C3 (búsqueda y propiedad), un servicio por paso.
4. C6 (guardas).
5. C7 (limpieza).
6. C5 (paquetes), al final: es solo un movimiento mecánico y no se mezcla con cambios de lógica.

Después de cada corte: `mvn -Dtest=<pruebas del área> test` y `git diff --check`. Al final: `mvn verify` y revisión independiente (Senior Security Engineer para C4 y C6, por autorización).

Riesgos:

- C4 y C6 tocan autorización, así que una regresión podría abrir acceso. Se mitiga con las pruebas de guardas y de tenant existentes, más la revisión de seguridad.
- C5 genera diffs grandes de imports. Hacerlo por paquete y sin cambios de lógica lo deja fácil de revisar.
- Hay pruebas que usan clases package-private (`ClerkTokenVerifierTest`, `InvitacionUsuarioService.ROLES_INVITABLES`). Al mover paquetes, cada prueba se mueve junto a su clase.
