# Ensayo de respaldo y restauración PostgreSQL

Este procedimiento es opt-in y requiere aprobación de ejecución. No sustituye retención, almacenamiento externo, alertas ni recuperación productiva. Alcance: [BACKUP-RESTORE-11](../../docs/specs/BACKUP-RESTORE-11/plan.md).

## Preparar conexión sin revelar secretos

El conector Render consultado no entrega contraseñas. El operador puede copiar la External Database URL del panel de la base confirmada y guardarla mediante este asistente en terminal local:

```sh
python3 scripts/backup-restore-drill.py --configure-source --source-config /private/tmp/backup11-source.json
```

El prompt oculta la entrada; no pegar la URL en chat, argumentos de shell ni logs. El asistente exige el host de staging conocido y crea un archivo nuevo600 fuera del workspace, sin abrir conexiones. No sobrescribe uno existente. Indicar al agente únicamente la ruta del archivo. La configuración incluye nombre/usuario/contraseña y se trata como secreta; permanece bajo control del operador y no se elimina automáticamente como si fuera un archivo del ensayo.

Alternativamente, un archivo privado600 del operador puede contener campos `source_id`, `host`, `database`, `expected_database`, `username`, `password`. Identidad y permisos se validan antes de usarlo. Confirmar que `expected_database` corresponde a la base del panel; el host es exclusivo de la instancia Render aprobada. No leer .env arbitrarios ni modificar accesos de Render para facilitar la prueba.

## Ejecutar después de aprobación separada

Pruebas unitarias (sin conexiones):

```sh
python3 -m unittest discover -s scripts/tests -p test_backup_restore_drill.py -v
```

Ensayo completamente sintético, dos contenedores propios PostgreSQL18: origen15437 y destino15436. No consume credenciales Render. El test sustituye explícitamente la conexión origen solo en su proceso de prueba; no valida TLS de Render ni demuestra que se haya exportado staging.

```sh
BACKUP11_LOCAL_CHECK=1 python3 -m unittest discover -s scripts/tests -p test_backup_restore_postgres.py -v
```

Ensayo real, origen Render en lectura y destino local nuevo15436:

```sh
python3 scripts/backup-restore-drill.py --source-config /private/tmp/backup11-source.json --execute
```

Requisitos: clientes PostgreSQL18, Python3, Docker, acceso externo autorizado al origen. No cambia allowlist, servicio o plan. TLS verify-full con raíces del sistema; si no se puede verificar certificado o faltan permisos, falla sin bajar a conexión insegura. Limita el ensayo a128MiB para evitar tratar una base de tamaño inesperado con el presupuesto Free. No arranca Spring/Next, correo ni Stripe.

## Qué verifica

Conexión e identidad origen, transacción read-only, instantánea compartida con pg_dump custom, archivo legible y SHA256. Requiere esquema de aplicación poblado e historial Flyway sin migraciones fallidas. Comprueba columnas/defaults, restricciones, índices y conteos/huellas de tablas public sobre la misma instantánea. Para evitar publicar filas, calcula MD5 por fila en PostgreSQL y SHA256 del stream de hashes ordenados. Esa huella detecta diferencias de datos; no es una certificación criptográfica frente a un adversario.

Restaura solo en base vacía del contenedor propio con no-owner/no-acl, single-transaction/exit-on-error y sin clean. Verifica que secuencias positivas no cíclicas no colisionen con las filas restauradas. Las secuencias no son MVCC: no se exige igualdad con el valor actual del origen mientras recibe escrituras. El archivo se verifica otra vez por SHA256 al terminar.

No compara exhaustivamente funciones/vistas/políticas/objetos fuera de public ni restaura roles globales o privilegios del proveedor; el dump lógico contiene el esquema de base soportado por pg_dump y la comparación es relacional. No demuestra funcionamiento de la aplicación recuperada ni continuidad de fotografías/archivos externos. Antes de conectar una recuperación a usuarios reales hay que definir secretos nuevos, invalidación de sesiones/tokens, integraciones y cambio de destino mediante un plan propio.

## Limpieza y evidencia

El ensayo elimina su dump, copias temporales de credenciales y logs privados bajo un directorio700. El contenedor se elimina solo tras comprobar ID/label; nunca elimina otros contenedores/volúmenes. La configuración privada del operador se preserva. No guarda respaldos permanentes: la salida contiene solo veredicto, identificador público del servicio, número de tablas/secuencias, tiempos y checksum. No publicar errores SQL ni tablas.

Los tiempos reflejan exportación y restauración técnica de este volumen; no son un RTO de aplicación. Sin backups programados y retención no existe un RPO garantizado. Para producción definir frecuencia, destino durable/cifrado, conservación, monitoreo y ensayo periódico. Este procedimiento no habilita PITR del plan Free.
