# Migración selectiva de empresas — runbook

Alcance, reglas y aceptación en [012-migracion-selectiva-empresas](../specs/012-migracion-selectiva-empresas/spec.md). Este documento **no autoriza** ejecutar nada: cada paso real requiere la aprobación indicada. Solo se imprimen recuentos, identificadores técnicos y hashes; nunca filas, credenciales ni direcciones de conexión. No pegues contraseñas ni URL de conexión en el chat.

## Requisitos locales

- `libpq` 18.x (`pg_dump`, `pg_restore`, `psql`): en esta máquina están en `/opt/homebrew/opt/libpq/bin` y el script lo añade a `PATH`.
- Docker accesible (Rancher Desktop: `export DOCKER_HOST=unix://$HOME/.rd/docker.sock`) y la imagen `postgres:18`.
- Una carpeta privada **fuera del repositorio**: `mkdir -p ~/vortise-private && chmod 700 ~/vortise-private`.
- Respaldos y volcados contienen datos de clientes y hashes de contraseñas: guárdalos cifrados y no los subas a ningún lugar.

## Paso 1 — Archivo de conexión al origen (lo haces tú)

```sh
cd gestion_be
python3 -B scripts/extract-empresas.py configure-source --config ~/vortise-private/source.json
```
Pide la *External Database URL* de Render con entrada oculta; crea un archivo con permiso 600 y no abre ninguna conexión. El script rechaza cualquier host que no sea la instancia confirmada.

## Paso 2 — Volcado de seguridad (solo lectura, autorizado)

```sh
python3 -B scripts/extract-empresas.py backup --source-config ~/vortise-private/source.json --output ~/vortise-private/staging-completo.dump
```
Instantánea de solo lectura; verifica el archivo con `pg_restore --list` y muestra tablas, tamaño y SHA-256. Aborta si la base supera 128 MiB.

## Paso 3 — Simulacro de depuración (solo local; requiere aprobación específica)

```sh
export DOCKER_HOST=unix://$HOME/.rd/docker.sock
python3 -B scripts/extract-empresas.py prune --dump ~/vortise-private/staging-completo.dump \
  --cuit 20337746083 --cuit 20337746084 \
  --output ~/vortise-private/depurado.dump --report ~/vortise-private/informe.json
```
Restaura el volcado en un PostgreSQL desechable, conserva solo esas empresas y todo lo relacionado, y comprueba: recuentos por tabla contra una formulación independiente, integridad de todas las claves foráneas y secuencias. Si algo falla, **no deja ningún archivo de salida**. Imprime recuentos por tabla y el SHA-256 del volcado depurado.

Regla de usuarios: se conservan los miembros de esas empresas **y quien las creó**. Si el creador pertenece a otra empresa, se asigna a la de menor id entre las que creó (`users_rehomed` en el resumen). Si una empresa quedara sin miembro ni creador, el proceso aborta.

Qué revisar con el informe (puedes enviármelo, no contiene datos): que queden exactamente 2 empresas, que los recuentos por tabla sean los esperables, qué se descartó (otras empresas, Stripe, tokens, gastos fijos y rendimientos) y que no haya tablas sin clasificar.

## Paso 4 — Destino vacío (lo haces tú, en Render)

1. Crea la base PostgreSQL **18** de pago, vacía, en la misma región que el backend.
2. `python3 -B scripts/extract-empresas.py configure-target --config ~/vortise-private/target.json` (URL externa, entrada oculta). El script exige un host de Render distinto del origen.
3. **No arranques la aplicación contra esa base** antes del paso 5: el volcado trae el esquema y el historial de Flyway.

## Paso 5 — Aplicar al destino (segunda aprobación explícita)

```sh
python3 -B scripts/extract-empresas.py apply --target-config ~/vortise-private/target.json \
  --pruned-dump ~/vortise-private/depurado.dump --report ~/vortise-private/informe.json \
  --confirm-sha256 <SHA-256 impreso en el paso 3 y revisado>
```
Rechaza el volcado si su hash no coincide con el informe revisado; comprueba identidad, PostgreSQL 18 y que el destino esté vacío; restaura en una transacción; y compara esquema, huellas por tabla, claves foráneas y secuencias con la copia verificada.

## Paso 6 — Primer arranque de producción

1. Perfil `production`, variables de [production-configuration.md](production-configuration.md) y la URL interna de la nueva base con TLS exigido.
2. Si el superadmin actual está entre los usuarios migrados, no definas `APP_SUPERADMIN_*`; si no, defínelas ahora (las secuencias ya son altas y no colisionan).
3. Comprueba `readiness`, que Flyway valida sin migraciones inesperadas y que el inicio de sesión de un usuario migrado funciona.
4. Los usuarios migrados no tienen suscripción: decide cómo se activa Pro (tarea P2 del [plan de salida](../../docs/plan-salida-produccion-rapida.md)).

## Qué hace y qué no hace

| Hace | No hace |
| --- | --- |
| Lee el origen solo en una transacción de solo lectura | Escribir en el origen o ejecutar DDL en él |
| Depura en una base local desechable (loopback, en memoria) | Tocar el destino antes del paso 5 |
| Aborta ante tablas no clasificadas o referencias entre empresas | Adivinar, ignorar en silencio o copiar parcial |
| Elimina archivos intermedios y contenedores propios | Subir nada a ningún servicio |

## Si algo falla

- Antes del paso 5 no hay nada que revertir: el origen no cambió y el destino no se tocó.
- Si `apply` falla, la restauración es de una sola transacción: el destino queda vacío. Vuelve a crearlo vacío si hace falta y repite.
- Aborto por «tablas sin clasificar»: no es un error del dato; hay que extender la clasificación (con revisión) antes de continuar.
- Aborto por integridad referencial: hay filas de las dos empresas que apuntan a otra empresa; el mensaje no incluye valores. Se decide caso por caso con el dueño.
