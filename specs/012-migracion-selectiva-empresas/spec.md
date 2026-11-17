# 012 — Migración selectiva de empresas a producción

Estado: propuesta pendiente de aprobación. **Sin ejecución, sin lectura de datos y sin código.** Parte de [plan-salida-produccion-rapida.md](../../../docs/plan-salida-produccion-rapida.md), tarea B1.

## Objetivo

Llevar a la base PostgreSQL de producción **solo** estas dos empresas y todo lo que depende de ellas, tomándolas de la base de staging de Render:

| Empresa | Identificación fiscal |
| --- | --- |
| Vórtise Build NO BORRAR | 20337746084 |
| Vórtise TEST 2 | 20337746083 |

Datos confirmados por el dueño el 2026-11-17: origen = base PostgreSQL de Render (staging actual); incluir los usuarios de esas empresas con sus contraseñas ya cifradas; **no** migrar suscripciones ni pagos (son de Stripe en sandbox; el plan se asigna a mano).

## Qué se conserva

Para las dos empresas: la empresa, sus usuarios, su configuración (calendario laboral, catálogos, categorías de mano de obra, responsables, unidades) y todas sus obras con lo que cuelga de ellas: plantas, rubros/actividades, contratos, subcontratistas de obra, tareas de obra, restricciones y comentarios, registros de horas, presupuesto, ingresos, costos indirectos, entregas, certificados y sus ítems, reportes emitidos e imágenes.

El script **descubre las relaciones desde el catálogo de PostgreSQL** (claves foráneas) en lugar de depender de una lista escrita a mano. Si encuentra una tabla con datos y sin relación demostrable con una empresa, **se detiene** y la informa: no la copia ni la ignora en silencio.

## Qué no se migra o se limpia

| Dato | Tratamiento | Motivo |
| --- | --- | --- |
| Otras empresas y todo lo suyo | Eliminado de la copia | Fuera de alcance |
| `suscripciones`, `pagos_suscripcion`, `devoluciones_pago`, `stripe_eventos_procesados` | Vaciados | Datos de Stripe en sandbox |
| `planes_suscripcion.stripe_price_id` | Se pone a nulo | Son identificadores de sandbox; producción los toma de su configuración |
| `password_reset_tokens`, `password_reset_quotas` | Vaciados | Datos efímeros de seguridad; el usuario puede pedir un enlace nuevo |
| `invitaciones_equipo`, `invitacion_empresas` | **Se conservan** las invitaciones cuyo titular se migra, solo con las filas hacia empresas conservadas; se descartan las que quedan sin ninguna empresa conservada | El dueño pidió conservar todo lo relacionado con las dos empresas (2026-11-17) |
| `usuario_empresa_accesos` | Solo filas donde usuario y empresa se conservan | Evita referencias a empresas ajenas |
| `gastos_fijos`, `rendimientos` | Vaciados | Sin dueño de empresa; la función está deshabilitada en producción |
| Usuarios | Se conservan los miembros de las dos empresas **y los creadores de esas empresas** (`empresas.creado_por_usuario_id`). Un creador que pertenece a otra empresa se asigna a la de menor id entre las que creó | Decisión del dueño 2026-11-17. En el origen ningún usuario pertenecía a estas dos empresas: un administrador accede a las empresas que creó, así que sin su creador nadie podría abrirlas |
| `usuarios_empresa.invitante_id` | A nulo si el invitante no se conserva | Evita referencia fuera del conjunto |
| `empresas.creado_por_usuario_id` | Se conserva (el creador siempre se conserva) | Es lo que da acceso al administrador |
| `flyway_schema_history` | Se conserva tal cual | El destino ya tiene el esquema y su historial |

**Efecto a conocer:** sin suscripciones, ningún administrador tiene Pro; las funciones de equipo (suspensión de colaboradores sin Pro) quedan afectadas hasta decidir P2 (activación manual de Pro).

## Aceptación

1. La copia depurada contiene exactamente 2 empresas y ninguna fila alcanzable desde otra empresa, recorriendo todas las claves foráneas.
2. Todas las restricciones de integridad validan en la copia y en el destino.
3. Por cada tabla, el recuento en la copia coincide con un recuento independiente calculado sobre la misma instantánea del origen filtrado por las dos empresas.
4. No quedan filas de Stripe, de reinicio de contraseña ni de invitaciones; no quedan identificadores de precio de sandbox.
5. Las secuencias del destino son mayores que el identificador máximo de cada tabla.
6. El origen no recibe ninguna escritura.
7. Con el perfil `production`, la aplicación arranca contra el destino, Flyway valida sin pendientes inesperados y un usuario migrado puede iniciar sesión (verificado por el dueño).
8. Dos usuarios de empresas distintas siguen sin ver datos cruzados.
9. Ninguna empresa conservada queda sin un miembro propio o un creador conservado; si ocurriera, el proceso aborta sin publicar nada.
10. La evidencia contiene recuentos, identificadores técnicos y estado; **nunca** valores de datos, credenciales ni hashes.

## Seguridad y límites

- Las credenciales de origen y destino van en archivos privados con permiso 600 fuera del repositorio, creados con petición oculta local; nunca en chat ni en Git.
- Los archivos intermedios (volcado, copia depurada) son privados, se eliminan al terminar y no se suben a ningún lugar.
- La copia depurada se ejecuta en un PostgreSQL local desechable (loopback, memoria), como en `BACKUP-RESTORE-11`.
- El destino solo se escribe tras una **segunda aprobación explícita** y solo si está vacío y coincide con la identidad aprobada.
- No se modifica el origen ni se ejecuta DDL en él.
