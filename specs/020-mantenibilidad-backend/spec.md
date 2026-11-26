# 020 — Reducción de duplicación y mantenibilidad del backend

## Estado

Borrador para aprobación. Revisión realizada el 2026-11-26 sobre `gestion_be` tras el refactor 017 y la migración a Clerk (019). No hay cambios de código en este hito.

## Objetivo

Reducir la duplicación y agrupar las clases por responsabilidad, sin cambiar el comportamiento observable. Los contratos REST, los códigos HTTP, los mensajes de error, las guardas de autorización, el aislamiento entre empresas, los límites transaccionales y el orden de los bloqueos se mantienen.

## Fuera de alcance

- Cambios de API o de DTO visibles por `gestion_fe`.
- Migraciones destructivas de esquema. La baja de columnas sin uso es un corte separado y requiere aprobación de datos.
- Cambios en fórmulas de certificados, Previsto vs. Real o estructura de rubros. Solo se permite moverlas de lugar, con pruebas de caracterización.
- Reemplazar la arquitectura Controller → Service → Repository definida en 017.

## Criterios de aceptación

1. **Invitaciones.**
   - La aceptación con contraseña y la aceptación con identidad comparten un único flujo por tipo de invitación.
   - Las reglas comunes viven en un solo lugar: hash del token, invitación disponible, "no disponible", normalización de email, URL del frontend y mensaje de conflicto.
   - Se conservan los mensajes, el orden de bloqueo y las validaciones actuales.
2. **Sesión.** `AutenticacionService.sesion` y `me` arman con un único componente el plan efectivo, la empresa inicial y las obras asignadas.
3. **Búsqueda y propiedad.** La búsqueda de un recurso de una empresa con "no encontrado" y "no pertenece a la empresa" usa un helper común, con mensajes por recurso. Esto cubre, al menos, `EmpresaConfiguracionService`, `ConfiguracionEmpresaObraService`, `UsuarioEmpresaService` y `ObraTareaService`.
4. **Excepciones.**
   - Los servicios de aplicación no dependen de `org.springframework.security.access.AccessDeniedException`.
   - Usan excepciones de `application/exception`, y `GlobalExceptionHandler` mantiene la respuesta 403.
5. **Paquetes.**
   - Los servicios se agrupan por funcionalidad dentro de `service/` (`invitacion`, `empresa`, `proyecto`, `certificado`, `restriccion`, `equipo`), siguiendo lo que ya hacen `service/authentication` y `service/pagos`.
   - Las reglas puras de dominio viven en `domain/rule` y las utilidades de aplicación en `application/helper`, con un criterio documentado.
6. **Código de prueba.** Las configuraciones exclusivas de pruebas (`PlantaTestIsolation*`) salen de `src/main`. Si alguna no puede moverse, se documenta por qué.
7. **Sin código muerto.** Se eliminan los métodos de repositorio, los DTO y los accesores sin referencias en `src/main` ni en `src/test`. Esto incluye `UsuarioEmpresa.codigoInvitacion` e `invitacionExpiraEn` a nivel de entidad.
8. Todas las pruebas existentes pasan. Se agregan pruebas solo donde una consolidación cambia la forma del código sin una prueba previa que lo cubra.
