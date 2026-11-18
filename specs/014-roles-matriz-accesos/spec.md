# Spec 014 — Roles y accesos con Spring Security (3 roles, permisos en el token)

Estado: **BORRADOR PARA APROBACIÓN** (no implementar hasta aprobar spec y plan).
Repositorios: `gestion_be` (propietario) y `gestion_fe` (consumidor).
Fuente funcional: `VÓRTISE BUILD _ Matriz de accesos.xlsx`. El anexo A la resume con el guard que corresponde a cada fila.

## 1. Enfoque

Se usan **3 roles de Spring Security** y un `@PreAuthorize` en cada endpoint. Lo que puede hacer el usuario viaja en el **token** (rol y obras asignadas). No hay catálogo de capacidades, niveles ni áreas.

| Acceso de la matriz | Rol | Alcance |
|---|---|---|
| 2 · Gerencia de obras / Coordinación de obras | `ADMINISTRADOR` | Todo |
| 3 · Jefatura de obra | `JEFE_DE_OBRA` | Solo lo marcado "Obra asignada", y únicamente en sus obras |
| 1 · Visualizar (Dirección, Oficina técnica, Administración, etc.) | `CONSULTA` | Solo lectura |

`RESPONSABLE` se trata como `CONSULTA` (solo lectura). `COLABORADOR` (invitado del plan Pro) no cambia.

## 2. Problema actual
1. La mayoría de los endpoints de escritura no tienen autorización por rol: un `CONSULTA` o `RESPONSABLE` puede modificar datos por API. Sin `@PreAuthorize`: `TareaController`, `ObraTareaController`, `RestriccionController`, `PlantaController`, `RendimientoController`, `ReporteObraController`, `RegistroHorasController`, `PresupuestoLineaController`, `GastoFijoController`, `IngresoController`, `CostoIndirectoController` y `ProyectoController` (`PUT`/`DELETE`).
2. No existe la relación usuario↔obra, así que `JEFE_DE_OBRA` no se limita a su obra. Hoy solo se usa en `CertificadoObraController`.
3. El frontend decide con strings de rol sueltos (`lib/permissions.ts`), solo como UX.

## 3. Estado actual verificado
- `JwtService`: token de máx. 1 h con claims `userId`, `empresaId`, `rol`, `superAdmin`, `sessionVersion`.
- `JwtAuthenticationFilter` **recarga el usuario de la base en cada request**, valida `activo` y `sessionVersion`, y arma `ROLE_<rol>` desde la base (hoy ignora el `rol` del token). Deja los claims en `authentication.details`.
- `TenantAccessInterceptor` ya resuelve la empresa y la obra de las rutas `/api/proyectos/{id}/**`, `/api/restricciones/{id}`, `/api/tareas/{id}/status` y `/api/registro-horas/{id}`. Los certificados cuelgan de `/api/proyectos/{id}/certificados`, por lo que quedan cubiertos.
- `@EnableMethodSecurity` activo. Migraciones Flyway V1–V21 (siguiente V22), `ddl-auto: validate`. `scripts/extract-empresas.py` (spec 012) exige clasificar tablas nuevas.
- No existen los módulos "Automatización con IA" ni "Asistente por WhatsApp".

## 4. Decisiones aprobadas
| # | Decisión |
|---|---|
| D1 | Quien da de alta usuarios y asigna accesos: Acceso 2 (`ADMINISTRADOR`). |
| D2 | Un acceso (rol) por usuario; el Acceso 3 se limita por lista de obras asignadas. |
| D3 | Jefatura cierra certificados solo en su obra asignada. |
| D4 | Imágenes en reportes, fotos y envío manual: lo que dice la matriz (obra asignada). |
| D5 | Vista previa y exportar planilla: lo que dice la matriz. |
| D6 | Administrador de plataforma: `SUPER_ADMIN` existente, sin cambios. |
| D7 | `ADMINISTRADOR` = gerente (todo); `COLABORADOR` sin cambios; `CONSULTA` y `RESPONSABLE` = solo lectura; `JEFE_DE_OBRA` = Acceso 3. |
| D8 | La matriz queda fija en código (anotaciones por endpoint). |
| D9 | Primero se corrige la brecha de escritura sin autorización (Fase 0, desplegable por separado). |
| D10 | Lo que puede hacer el usuario se define en el **token** (rol y obras asignadas); los endpoints deciden por rol. |

## 5. Rol y obras en el token

### 5.1 Claims
```json
{ "sub": "ana@empresa.demo", "userId": 12, "empresaId": 3,
  "rol": "JEFE_DE_OBRA", "obras": [7, 9], "sessionVersion": 41, "exp": 1760000000 }
```
- `rol` ∈ `ADMINISTRADOR | JEFE_DE_OBRA | CONSULTA | RESPONSABLE | COLABORADOR`.
- `obras`: ids de obras asignadas; solo se emite para `JEFE_DE_OBRA`.
- `JwtService.generate(...)` incorpora `obras`; el login y `/api/auth/me` lo devuelven al frontend junto al rol.

### 5.2 Cómo se usan
- `JwtAuthenticationFilter` construye las authorities **desde los claims del token** (`ROLE_<rol>`, `ROLE_SUPER_ADMIN` si corresponde) y deja `obras` disponible en `authentication.details`.
- El token está firmado: mientras la firma sea válida, sus claims son confiables hasta que expira. Nunca se toma un rol, un `proyectoId` ni una lista de obras del body o de headers del cliente.

### 5.3 Revocación (se mantiene)
El filtro **conserva** la validación contra la base de `activo` y `sessionVersion`. Sin ella, un cambio de rol, obra o baja de usuario no tendría efecto hasta que el token venza (hasta 1 h).
- Al cambiar rol, obras asignadas o estado del usuario, se incrementa `sessionVersion`: el token anterior queda inválido y el usuario debe volver a iniciar sesión.
- Consecuencia aceptada: el cambio de permisos obliga a reloguear.
- Alternativa descartada: confiar solo en el token sin consultar la base. Es más rápido, pero un usuario dado de baja o degradado conservaría acceso hasta 1 h.

## 6. Autorización por endpoint

### 6.1 Guards (constantes reutilizables)
```java
SOLO_GERENTE   = "hasRole('ADMINISTRADOR')"
GERENTE_O_JEFE = "hasAnyRole('ADMINISTRADOR','JEFE_DE_OBRA')"   // + obra asignada si es JEFE_DE_OBRA
// lecturas (GET): sin restricción de rol, solo aislamiento por empresa/obra existente
```
Regla para cada fila de la matriz: A3 = "Denegado" y A2 = "Permitido" → `SOLO_GERENTE`; A3 = "Obra asignada" → `GERENTE_O_JEFE`; "Consulta" → lectura.

### 6.2 Obra asignada (un solo lugar)
En `TenantAccessInterceptor`, tras resolver la obra de la ruta o del recurso hijo: si el rol es `JEFE_DE_OBRA`, la petición es de escritura y la obra no está en `obras` del token → **403 `OBRA_NO_ASIGNADA`**. No se repite en cada controlador.
- Las lecturas de un `JEFE_DE_OBRA` siguen la matriz ("Consulta"): sin restricción por obra (ver Q-1).
- La obra siempre se resuelve desde la ruta o el recurso en base de datos, nunca desde el body.

### 6.3 Fase 0 — default-deny para solo lectura (hotfix)
Un filtro/interceptor devuelve **403 `ACCESO_SOLO_LECTURA`** para `POST/PUT/PATCH/DELETE` bajo `/api/**` cuando el rol es `CONSULTA` o `RESPONSABLE` (y no es `SUPER_ADMIN`).
- Excepciones (lista cerrada): `/api/auth/**`, restablecer contraseña, aceptar invitación, `/api/stripe/webhook`, `/api/me/foto`.
- No cambia `ADMINISTRADOR`, `JEFE_DE_OBRA` ni `COLABORADOR`. Sin migración. Se puede desactivar por propiedad de configuración.
- Antes de activarlo se inventarían los `POST` que sean lecturas.
- Se mantiene después como defensa en profundidad.

### 6.4 Reglas finas (con el estado del recurso, en los casos de uso)
| Regla | Rol afectado |
|---|---|
| Cambiar el estado de una restricción es **una sola vez** (una vez resuelta no puede volver a cambiarse) | `JEFE_DE_OBRA` |
| Marcar/destildar "Ítem completado" solo mientras el certificado **no esté cerrado** | `JEFE_DE_OBRA` |
| Completar horas/cantidades y cerrar tarea: solo tareas **abiertas** | `JEFE_DE_OBRA` |
| Editar tareas cerradas, eliminar tareas, editar/eliminar restricciones, editar certificados | solo `ADMINISTRADOR` (guard `SOLO_GERENTE`) |

Error: 403 `REGLA_DE_ESTADO`.

## 7. Gestión de usuarios y obras asignadas
- Solo `ADMINISTRADOR` crea/edita usuarios, cambia el rol y asigna obras (D1).
- Reglas anti-escalada: nadie cambia su propio rol ni sus propias obras; no se crea `SUPER_ADMIN`; solo obras de la misma empresa; `obras` solo para `JEFE_DE_OBRA`.
- Cada cambio de rol/obras/estado incrementa `sessionVersion`.
- Datos: tabla `usuario_obra (usuario_id, proyecto_id)` con clave única y FKs (migración V22, aditiva). Se clasifica en `scripts/extract-empresas.py` en el mismo cambio.
- Migración de usuarios existentes: `JEFE_DE_OBRA` sin obras asignadas queda en solo escritura denegada hasta que se asignen (ver Q-3).

## 8. Frontend (`gestion_fe`)
- El rol y las obras vienen del login/`/me` y se guardan en la sesión.
- `lib/permissions.ts` pasa a derivar de rol: `puedeEscribir`, `esGerente`, `puedeEnObra(obraId)`. Es **solo UX**; la autoridad es el backend.
- Ocultar/deshabilitar acciones según el rol y mostrar "Solicitalo a Gerencia o Coordinación de obras" cuando corresponda.
- Manejo explícito del 403 por `code`, sin convertirlo en un resultado que parezca válido.
- Pantalla de usuarios: rol (Gerente/Jefe de obra/Consulta) y selector de obras para jefe. Tipos TS alineados con los DTOs.

## 9. Criterios de aceptación
1. **F0:** `CONSULTA` y `RESPONSABLE` reciben 403 en todo endpoint de escritura; `ADMINISTRADOR`, `JEFE_DE_OBRA` y `COLABORADOR` conservan su comportamiento; `/api/auth/**` y el perfil propio siguen funcionando.
2. Cada fila del anexo A se cumple para los 3 roles: guard `SOLO_GERENTE` → jefe y consulta reciben 403; `GERENTE_O_JEFE` → jefe solo en obra asignada, consulta recibe 403.
3. Un `JEFE_DE_OBRA` no escribe en una obra no asignada, ni por recurso hijo (IDOR).
4. Se cumplen las reglas finas de 6.4.
5. El token trae `rol` (y `obras` para jefe) y las authorities salen del token; un token de usuario dado de baja o con `sessionVersion` vieja es rechazado.
6. Cambiar rol u obras invalida la sesión anterior.
7. Nadie escala privilegios; un `ADMINISTRADOR` no crea `SUPER_ADMIN` ni se cambia su propio rol.
8. Aislamiento entre empresas intacto (prueba cruzada).
9. La UI no muestra acciones que el backend va a rechazar.
10. Verificación ejecutada y reportada por repositorio; revisión de seguridad cerrada.

## 10. Riesgos
- **R-1:** la F0 puede bloquear un flujo legítimo que use `POST` para leer. Mitigación: inventario y lista de excepciones.
- **R-2:** un `JEFE_DE_OBRA` existente pierde escritura hasta que se le asignen obras.
- **R-3:** certificados hoy permiten escribir al jefe en cualquier obra; se restringe a propósito (D3).
- **R-4:** un `ADMINISTRADOR` gerente también ve facturación e invitaciones. Se acepta para la demo; si se necesita separar al dueño, se agrega otro rol.
- **R-5:** sin catálogo, un endpoint nuevo puede quedar sin guard. Mitigación: el filtro de F0 y un test que enumera los endpoints de escritura y exige guard o excepción.
- **R-6:** pruebas HTTP arrancan Spring/H2 y requieren aprobación de ejecución (AGENTS.md).

## 11. Preguntas abiertas
- **Q-1:** ¿`JEFE_DE_OBRA` consulta todas las obras de la empresa o solo las suyas? Se asume **todas** (la matriz dice "Consulta" sin restricción).
- **Q-2:** `COLABORADOR` no está en la matriz. Se asume **sin cambios**.
- **Q-3:** al activar la F1, ¿se asignan automáticamente a los jefes existentes todas las obras de su empresa, o se asignan a mano? Se asume **a mano**.

---

## Anexo A — Matriz y guard por fila

Guard: **L** lectura (sin restricción de rol) · **G** `SOLO_GERENTE` · **J** `GERENTE_O_JEFE` con obra asignada. Los accesos de la matriz son A1=CONSULTA, A2=ADMINISTRADOR, A3=JEFE_DE_OBRA.

| # | Módulo · Apartado | Acción | Guard | Observaciones |
|---|---|---|---|---|
| 1 | Startup de empresa · Consulta | Ver datos de empresa, responsables, calendario y catálogos | L | |
| 2 | Startup de empresa · Paso 1 | Cargar nombre e identificación fiscal | G | |
| 3 | Startup de empresa · Paso 2 | Cargar y editar responsables | G | |
| 4 | Startup de empresa · Paso 3 | Cargar calendario laboral | G | |
| 5 | Startup de empresa · Paso 4 | Agregar unidades, tipos de restricción y causas de no cumplimiento | G | |
| 6 | Startup de empresa · Paso 5 | Crear empresa | G | |
| 7 | Startup de obra · Consulta | Ver datos de la obra, equipo y contratistas | L | |
| 8 | Startup de obra · Paso 1 | Cargar superficies, sectores y niveles | G | |
| 9 | Startup de obra · Paso 2 | Cargar nombre, dirección, número y fechas | G | |
| 10 | Startup de obra · Paso 3 | Asignar responsables de la obra | G | |
| 11 | Startup de obra · Paso 3 | Definir anticipación mínima de restricciones | G | |
| 12 | Startup de obra · Paso 3 | Categorías de mano de obra propia y costo de HH | G | |
| 13 | Startup de obra · Paso 4 | Agregar contratistas y contratos | G | |
| 14 | Startup de obra · Paso 5 | Crear obra | G | |
| 15 | Pantalla de inicio · Indicadores | Ver restricciones, avance y horas por rubro | L | Solo consulta para todos. |
| 16 | Actividades · Consulta | Ver itemizado y filtrar por empresa | L | |
| 17 | Actividades · Carga de rubros | Alta de rubros, estructura, ejecutor y contrato | G | |
| 18 | Actividades · Carga de subrubros | Subrubros, niveles, cantidades y unidades | G | |
| 19 | Actividades · Edición | Modificar o eliminar rubros y subrubros | G | Se refleja en la estructura de certificados. |
| 20 | Productividad · Indicadores y horas | Ver indicadores y horas consumidas | L | Solo consulta para todos. |
| 21 | Productividad · Rendimiento | Cargar y editar rendimiento previsto | G | |
| 22 | Productividad · Cronograma | Inicio previsto, personas y frentes | G | |
| 23 | Productividad · Cronograma | Fin previsto de contratistas y ajuste manual | G | |
| 24 | Tareas · Indicadores y cargas diarias | Ver indicadores y cargas diarias | L | |
| 25 | Tareas · Cargar avance (1 y 2) | Cargar avance | J | |
| 26 | Tareas · Cargar avance (1) | Cumplimiento, causa y medida correctiva | J | |
| 27 | Tareas · En proceso | Completar horas/cantidades y cerrar tarea | J | Solo tareas abiertas (6.4). |
| 28 | Tareas · Cerradas | Editar horas y cantidades de tarea cerrada | G | Jefatura lo solicita a Gerencia/Coordinación. |
| 29 | Tareas · Tareas | Eliminar tareas | G | Jefatura lo solicita a Gerencia/Coordinación. |
| 30 | Restricciones · Cumplimiento | Ver restricciones y capacidad de respuesta | L | |
| 31 | Restricciones · Nueva | Crear restricción | J | |
| 32 | Restricciones · Estado | Cambiar el estado (liberar) | J | Jefatura una sola vez (6.4). |
| 33 | Restricciones · Edición | Editar tipo, prioridad, subrubro, área, fecha límite | G | Jefatura lo solicita. |
| 34 | Restricciones · Edición | Eliminar restricciones | G | Jefatura lo solicita. |
| 35 | Certificados · Consulta | Ver certificados, historial, excedentes y ajustes | L | |
| 36 | Certificados · Creación | Crear certificado | J | |
| 37 | Certificados · Cierre de ítems | Marcar/destildar "Ítem completado" | J | Solo con certificado abierto (6.4). |
| 38 | Certificados · Edición | Editar un certificado creado | G | Jefatura lo solicita. |
| 39 | Certificados · Cierre | Cerrar certificado | J | D3: solo obra asignada. |
| 40 | Certificados · Cierre | Editar fecha de corte de certificado cerrado | G | |
| 41 | Certificados · Exportación | Exportar certificado a PDF | L | |
| 42 | Reportes · Historial | Ver reportes emitidos | L | |
| 43 | Reportes · Configurar | Fecha de corte, formato y secciones (vista previa) | J | No guarda datos (D5). |
| 44 | Reportes · Imágenes | Agregar imágenes con rubro y nivel | J | D4. |
| 45 | Reportes · Imprimir/PDF | Imprimir o guardar PDF sin emitir | L | |
| 46 | Reportes · Emitir | Emitir y guardar en el historial | J | |
| 47 | Automatización con IA · Startup | Cargar planilla del Startup | G | Reservada (módulo no existe; ver spec 013). |
| 48 | Automatización con IA · Obra anterior 1 | Exportar planilla de una obra | G | Reservada (D5). |
| 49 | Automatización con IA · Obra anterior 2 | Crear obra nueva desde una anterior | G | Reservada. |
| 50 | Asistente por WhatsApp · Consultas | Consultar reportes, certificados, restricciones, avance, horas y tareas | L | Reservada. |
| 51 | Asistente por WhatsApp · Carga | Cargar avance de tareas | J | Reservada. |
| 52 | Asistente por WhatsApp · Carga | Cargar restricciones | J | Reservada. |
| 53 | Asistente por WhatsApp · Carga | Cargar fotos para el informe | J | Reservada (D4). |
| 54 | Envío automático | Configurar envío automático o manual | J | |
| 55 | Envío automático | Enviar manualmente y elegir personas en copia | J | D4. |
| — | Usuarios (fuera de la matriz) | Crear/editar usuarios, rol y obras asignadas | G | D1. |

### Correspondencia con lo existente
- Filas 2–6 → `EmpresaController`, `EmpresaConfiguracionController`.
- Filas 8–14 → `PlantaController`, `ProyectoController`, `EmpresaConfiguracionController`.
- Filas 17–19 → `ProyectoController` (`PUT`) / `UpdateProyectoUseCase`.
- Filas 21–23 → `RendimientoController`, `ProyectoController`.
- Filas 25–29 → `ObraTareaController`, `TareaController`, `RegistroHorasController`.
- Filas 31–34 → `RestriccionController`.
- Filas 36–41 → `CertificadoObraController`.
- Filas 43–46 → `ReporteObraController`.
