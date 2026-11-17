# 012 — Estado

- [x] Alcance confirmado por el dueño (2026-11-17): empresas 20337746083 y 20337746084; origen Render; con usuarios y contraseñas cifradas; sin Stripe; «conservar todo lo relacionado a estas dos empresas» (invitaciones vinculadas incluidas).
- [x] Discovery estático del esquema (migraciones V1–V21): tablas, claves foráneas, tablas globales y de Stripe.
- [x] Especificación y plan aprobados por el dueño (2026-11-17).
- [x] Implementación del script y sus pruebas.
- [x] Pruebas sin base y ensayo sintético con PostgreSQL desechable local (resultados abajo).
- [ ] Volcado de seguridad del origen: el dueño debe crear el archivo privado de conexión (petición oculta); luego se ejecuta `backup` (autorizado).
- [x] Volcado de seguridad real hecho por el dueño (2026-11-17): 38 tablas, 120.320 bytes, SHA-256 `c3f70053…1dcb3e6a`, sin escrituras en el origen.
- [x] Simulacro real (`prune`, solo local), autorizado por el dueño. Primer intento descartado: dejó 0 usuarios porque ninguno de los 7 usuarios del origen pertenece a las dos empresas. Decisión del dueño: conservar al creador de las empresas y asignarlo a una de ellas. Segundo intento: 38 tablas, 433 → 199 filas, 2 empresas, 1 usuario (1 reasignado), 46 claves foráneas y 30 secuencias verificadas; volcado depurado SHA-256 `22ca9238…e771e`.
- [ ] Revisión independiente del informe de recuentos (pendiente; el dueño revisó los recuentos y aprobó).
- [x] Segunda aprobación del dueño y `apply` ejecutado (2026-11-17): `target_verified`, 38 tablas, 199 filas, 46 claves foráneas y 30 secuencias; huellas de esquema y de cada tabla idénticas a la copia verificada. Destino: gestion-prod-db (`dpg-dbu4qijl550s739uhur0-a`), contraseña rotada antes de cargar. Acceso externo sin limitar a una IP por decisión del dueño (riesgo aceptado).
- [ ] Verificación en destino, primer arranque `production` y comprobación funcional del dueño.

## Implementación

- `gestion_be/scripts/extract-empresas.py` (nuevo): subcomandos `configure-source`, `configure-target`, `backup`, `prune`, `apply`. Reutiliza sin modificarlas las protecciones de `backup-restore-drill.py` (origen fijado a la instancia confirmada, TLS verificado, sesión de solo lectura con instantánea, archivos privados 600, contenedor propio con limpieza verificada).
- Clasificación explícita de las 38 tablas; **aborta** ante cualquier tabla no clasificada. Dependencia por claves foráneas, recuentos esperados calculados con una formulación independiente (uniones hasta la empresa), comprobación de huérfanos sobre las 46 claves foráneas y de secuencias.
- `apply` exige el hash del volcado depurado revisado, identidad del destino distinta del origen, destino vacío, PostgreSQL 18, y compara esquema y huellas por tabla con la copia verificada.
- `gestion_be/scripts/tests/test_extract_empresas.py` (15 pruebas sin base) y `test_extract_empresas_postgres.py` (4 pruebas opt-in con PostgreSQL desechable y datos sintéticos).
- Runbook: [migracion-selectiva.md](../../docs/migracion-selectiva.md).

## Verificación ejecutada — 2026-11-17

| Comando (desde `gestion_be`) | Resultado |
| --- | --- |
| `python3 -B -m unittest discover -s scripts/tests -p test_extract_empresas.py -v` | 15 pruebas correctas. Incluye que toda tabla creada por las migraciones esté clasificada. |
| `EXTRACT12_LOCAL_CHECK=1 python3 -B -m unittest discover -s scripts/tests -p test_extract_empresas_postgres.py -v` (con `DOCKER_HOST` del socket de Rancher Desktop y `libpq` 18.4 en `PATH`) | 4 pruebas correctas en 22,7 s: de 3 empresas sintéticas quedan 2 con todo lo relacionado; Stripe, tokens de reinicio y tablas sin dueño vacíos; accesos e invitaciones externos descartados; referencias a usuarios no migrados anuladas; `apply` sobre un destino vacío verificado y rechazo sobre uno no vacío; aborto sin salida cuando una fila conservada referencia otra empresa, cuando hay una tabla no clasificada y cuando falta una empresa. |
| Comprobación de mutación | Quitando del borrado de accesos la condición de empresa, el recuento independiente detecta la diferencia y la prueba falla; restaurado, pasa. |
| Contenedores | Todos con etiqueta propia y eliminados; 0 restantes. |

Límites: solo datos sintéticos. **No se ha conectado a Render ni leído ningún dato real.** Los esquemas del ensayo se crean aplicando las migraciones V1–V21, no copiando el esquema real de staging. El destino Render real (TLS verificado, host fijado) solo se ejercita en `apply` real; el ensayo local usa una costura de prueba para el destino.

## Pendiente de verificar (supuestos)

- Que `flyway_schema_history` del origen coincide con V1–V21 (el volcado real lo mostrará).
- Tamaño real del volcado frente al límite de 128 MiB del script (`backup` lo comprueba y aborta con mensaje claro).
- Si el superadmin actual pertenece a una de las dos empresas.
- Qué usuarios de esas empresas son colaboradores con acceso a empresas ajenas (esos accesos se descartan).
- Tablas nuevas que el staging real tenga y las migraciones no (el script aborta y las nombra).

## Cambio de regla tras el primer simulacro real — 2026-11-17

- `extract-empresas.py`: los creadores de las empresas conservadas se conservan y, si pertenecen a otra empresa, se mueven a la de menor id que crearon; los recuentos esperados de usuarios, accesos e invitaciones usan esa misma definición; se aborta si una empresa conservada quedara sin miembro propio ni creador conservado.
- Pruebas: 17 sin base y 5 con PostgreSQL desechable (nuevo escenario de creador de otra empresa y aborto por empresa inaccesible), todas correctas. Dos fallos intermedios fueron del escenario/aserción de la prueba, no del script.
- No se leyeron valores: solo recuentos. No se conoce quién es el usuario conservado ni su rol; el dueño verificará el inicio de sesión tras el primer arranque.

## Pendiente antes de `apply`
- Contraseña de la base de destino rotada (la anterior se expuso en el chat) y archivo `target.json` creado con `configure-target`.
- Revisión del informe y segunda aprobación explícita del dueño, con el hash del volcado depurado.
- Sin suscripciones en destino: el administrador conservado no tendrá Pro hasta decidirlo (tarea P2).

## `apply` ejecutado — 2026-11-17

`python3 -B scripts/extract-empresas.py apply …` con el hash `22ca9238…e771e`: `{"status":"target_verified","tables":38,"rows":199,"foreign_keys_checked":46,"sequences_checked":30}`. Origen sin escrituras. No se arrancó la aplicación contra la base nueva.

Pendiente: subir de plan `vortise_be` y apuntarlo a la base nueva con perfil `production`; comprobar `readiness`, Flyway (sin migraciones inesperadas) e inicio de sesión del usuario conservado; limitar el acceso externo de la base; conservar cifrados `staging-completo.dump` y `depurado.dump` y borrar los que sobren; no eliminar la base antigua hasta verificar todo.
