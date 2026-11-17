# 012 — Plan técnico

Pendiente de aprobación de [spec.md](spec.md).

## Estrategia

Restaurar una instantánea del origen en una base local desechable, **depurarla ahí** y volcar la copia ya depurada al destino vacío. Ventajas: el origen solo se lee; la depuración se puede repetir y verificar sin riesgo; el volcado completo restaura las restricciones **después** de los datos, así que no hay problema de orden con referencias circulares (`usuarios_empresa.invitante_id`, `rubros_obra.parent_id`); incluye el historial de Flyway y las secuencias.

No se usa una carga «solo datos» sobre un esquema vacío: exigiría desactivar disparadores (requiere superusuario, que Render no entrega) o una carga en dos fases.

## Pasos

1. **Instantánea del origen** con el método ya aprobado en `BACKUP-RESTORE-11` (`pg_dump` con instantánea compartida, solo lectura, origen fijado a la instancia confirmada).
2. **Restauración en PostgreSQL 18 local desechable** (contenedor propio, tmpfs, loopback, con comprobación de dueño y limpieza).
3. **Depuración** con un script SQL generado por el programa:
   - Resolver el conjunto de empresas por identificación fiscal; si no son exactamente 2, abortar.
   - Leer el grafo de claves foráneas de `information_schema`/`pg_constraint`; marcar las tablas alcanzables desde `empresas` y calcular el orden de borrado inverso.
   - Abortar si hay una tabla con filas no alcanzable y no clasificada como «vaciar».
   - Aplicar la tabla de tratamiento de la spec (vaciar, anular columnas, filtrar accesos).
   - Borrar de hijos a padres lo que no pertenece al conjunto, comprobando que no quedan huérfanos.
4. **Verificación en la copia**: recuentos por tabla frente a los calculados sobre el origen; validación de todas las restricciones; recorrido «ninguna fila de otra empresa»; ausencia de datos de Stripe/reinicio/invitaciones; secuencias.
5. **Volcado de la copia depurada** (`pg_dump --format=custom --no-owner --no-acl`).
6. **Puerta de aprobación del dueño** con el informe de recuentos.
7. **Restauración en el destino vacío** (`pg_restore --exit-on-error --single-transaction --no-owner --no-acl`), tras comprobar identidad del destino y que no contiene tablas.
8. **Verificación en el destino**: mismos recuentos y restricciones; secuencias.
9. **Primer arranque** con perfil `production` y sin las variables del superadmin hasta confirmar si el superadmin ya está entre los usuarios migrados; revisar Flyway y la salud.
10. **Verificación funcional por el dueño**: inicio de sesión, aislamiento entre las dos empresas, asignación manual de Pro si se decide.

## Archivos previstos

- `gestion_be/scripts/extract-empresas.py` (nuevo): reutiliza las funciones de identidad, configuración privada y limpieza de `backup-restore-drill.py` sin modificarlo.
- `gestion_be/scripts/tests/test_extract_empresas.py` (nuevo): pruebas sin base (cálculo del grafo y del orden, abortos, redacción de mensajes) y un ensayo local con datos **sintéticos** de dos empresas más una ajena, siguiendo el patrón de `test_backup_restore_postgres.py`.
- `gestion_be/docs/migracion-selectiva.md` (nuevo): runbook.
- Sin cambios en código de aplicación, esquema ni dependencias.

## Comandos propuestos (no ejecutados)

Desde `gestion_be`:

```sh
python3 -B -m unittest discover -s scripts/tests -p test_extract_empresas.py -v
EXTRACT12_LOCAL_CHECK=1 python3 -B -m unittest discover -s scripts/tests -p test_extract_empresas_postgres.py -v
python3 -B scripts/extract-empresas.py --source-config <privado> --target-config <privado> --cuit 20337746083 --cuit 20337746084 --report <privado>        # simulacro: solo origen y base desechable
python3 -B scripts/extract-empresas.py ... --apply-target                                                         # destino: segunda aprobación
```

## Riesgos

| Riesgo | Mitigación |
| --- | --- |
| El origen vence el 22-nov y luego se borra | Hacer primero un volcado completo de seguridad; ejecutar los pasos 1–5 cuanto antes |
| El volcado supera el límite de 128 MB del script de respaldo | Medir el tamaño antes (solo metadatos); parametrizar el límite con aprobación |
| Esquema del origen desfasado respecto a V1–V21 | Comparar `flyway_schema_history` con las migraciones del código antes de decidir; Flyway migra hacia adelante al arrancar |
| Datos relacionados no previstos | El script aborta ante tablas no clasificadas |
| Comprobación de identidad del destino | Destino fijado por identificador y vacío antes de escribir |
| Los usuarios migrados no tienen Pro | Decisión P2 antes de invitar a nadie |
| Se pierde el superadmin si no está en esas empresas | `APP_SUPERADMIN_EMAIL`/`PASSWORD` tras la carga, con secuencias ya altas |

## Reversión

El origen no cambia. El destino solo se escribe tras la segunda aprobación; si falla, se descarta y se vuelve a crear vacío. Los archivos intermedios se eliminan.
