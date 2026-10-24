# 007 — Actividad de usuarios y métricas por empresa en Superadmin

## Estado

APROBADO PARA IMPLEMENTACIÓN — 2026-10-23. El dueño autorizó implementar este alcance tras aprobar la preparación. Las pruebas/build y cualquier operación contra PostgreSQL persistente/Render o despliegue requieren autorización específica.

## Contexto

La consola de plataforma permite consultar empresas y usuarios, pero no muestra el último login ni quién creó cada empresa ni métricas de recursos por empresa. La navegación está repetida dentro de cada página y vuelve a verificar el acceso al cambiar entre secciones.

## Objetivo

Mejorar el panel de solo lectura para que el superadmin pueda ver cuándo inició sesión por última vez cada usuario, identificar al creador de cada empresa, entender con conteos numéricos su volumen de uso y navegar entre secciones sin perder la shell del panel.

## Alcance

1. **Último login exitoso por usuario:** persistir la fecha/hora del login más reciente que autenticó correctamente. No medir cada request ni presentar el dato como presencia o actividad en tiempo real.
2. **Listado de usuarios:** añadir `ultimoLoginExitosoEn`; devolver `null` para usuarios sin un login registrado (incluidas cuentas anteriores a la migración). Los intentos fallidos no actualizan la fecha.
3. **Listado de empresas:** agregar nombre y correo del creador resoluble y los siguientes conteos:
   - usuarios totales y activos, excluyendo cuentas superadmin;
   - obras/proyectos;
   - restricciones;
   - registros de horas.
4. **Criterio de pertenencia:** atribuir datos de obra solo por relaciones JPA/FK hasta un proyecto cuyo `empresaRelacionada.id` sea la empresa. No inferir pertenencia desde nombres/campos de texto. Sin relación comprobable, no contar.
5. **Navegación:** toda la barra de secciones del panel queda sticky y su shell/layout común se conserva al navegar entre Resumen, Empresas, Usuarios, Suscripciones y Pagos. El cambio de sección no debe reinicializar la verificación de superadmin ni el contexto global de la aplicación innecesariamente.
6. Mantener autorización `ROLE_SUPER_ADMIN`, paginación del servidor, vistas de solo lectura y exclusión de empresa interna/cuenta superadmin según la spec 006.

## Fuera de alcance

- Registrar la última request autenticada, duración de sesión, presencia en línea o métricas de eventos detalladas.
- Mostrar contenido de obras, restricciones o registros; solo sus cantidades.
- Contar tareas, entregas, rubros, plantas, gastos fijos u otras entidades no seleccionadas en esta definición. Gastos fijos no tienen ownership de empresa confirmado.
- Nuevas acciones de administración, detalle editable, exportación, alertas o cambios de contratos comerciales.
- Backfill estimado de actividad histórica.
- Conexión, migración o despliegue en Render.

## Contrato API propuesto

Mantener endpoints y paginación existentes, con campos aditivos:

- `GET /api/admin/usuarios`: cada elemento incluye `ultimoLoginExitosoEn: Instant | null`.
- `GET /api/admin/empresas`: cada elemento incluye `creadorNombre: string | null`, `creadorEmail: string | null`, `usuariosTotales: number`, `usuariosActivos: number`, `obras: number`, `restricciones: number`, `registrosHoras: number`.
- Si `creadoPorUsuarioId` es nulo o no resuelve una cuenta, los dos campos de creador son `null`; la empresa sigue apareciendo.
- No devolver entidades JPA, hashes, credenciales, tokens, datos de proyectos ni PII distinta del nombre/correo del creador solicitado.

## Criterios de aceptación

1. Solo un login con credenciales válidas de una cuenta activa actualiza `ultimoLoginExitosoEn`; login inválido/inactivo no lo modifica.
2. El timestamp se persiste en UTC, sobrevive a reinicios y la API lo serializa como ISO-8601; los datos históricos no disponibles aparecen como `null`.
3. La lista de usuarios presenta “Último login exitoso” con fecha local legible y un estado explícito “Sin registro” para `null`.
4. Cada fila de empresa presenta creador resoluble (o “No disponible”) y los cinco conteos definidos; todos son numéricos y no exponen filas asociadas.
5. Los conteos excluyen empresa interna/superadmin donde aplique y solo incluyen pertenencia demostrable por relaciones.
6. No se generan consultas N+1 ni se cargan colecciones completas para contar; búsquedas y paginación mantienen el contrato actual.
7. El acceso sigue siendo exclusivo de `ROLE_SUPER_ADMIN`; usuario no superadmin no obtiene estos datos.
8. La barra permanece visible al desplazarse y el layout/chequeo de acceso permanece montado al navegar por secciones. La navegación conserva URL, accesibilidad y botones atrás/adelante; la carga de datos de una sección no bloquea ni desmonta toda la consola.
9. La migración es nueva, aditiva y forward-only; no editar migraciones aplicadas, reiniciar base ni inventar valores históricos.
