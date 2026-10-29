# CODE-QUALITY-14 — Cambios de contrato y operación

Implementado y verificado localmente: mvn verify436 casos (420PASS,16PG optativos omitidos), PostgreSQL18 desechable10PASS y Python4PASS. Revisión independiente aprobada tras corregir cuatro hallazgos; evidencia y límites en CODE-QUALITY-14/tasks.md del workspace. No es autorización de despliegue o migración de una base existente.

- Horas: se serializan las mutaciones por proyecto y el total se obtiene por SUM dentro de la misma transacción. Editar una obra con registros conserva el total derivado; sin registros mantiene la entrada manual. Los locks recargan entidades que el interceptor HTTP pudiera haber leído previamente.
- Configuración: los GET ya no crean filas. El alta prepara defaults en su transacción. `POST /api/empresas/{id}/configuracion/inicializar` permite preparar empresas anteriores, exige administrador y acceso al tenant, y devuelve204. Una marca de preparación conserva eliminaciones intencionales después de la primera inicialización.
- Migración V9: unicidad por empresa/código de unidad y empresa/nombre de categoría, más marca booleana de inicialización. Falla ante duplicados, sin borrarlos ni renombrarlos. Revisar datos mediante un plan autorizado antes de aplicarla en otra base. No modificar V1–V8.
- `PUT /api/proyectos/{id}` conserva plantas y rubros cuando se omiten/null; [] representa una colección explícitamente vacía y mantiene las restricciones de relaciones existentes.
- `PUT /api/empresas/{empresaId}/usuarios/{id}` recibe nombre, rol, activo y contraseña opcional. No exige ni modifica email. Null/blank en contraseña conserva el hash. Cambios reales aplican 8–72 caracteres y máximo72 bytes UTF-8, como alta/registro/reset.
- Validación400, recurso ausente404, conflicto409 y acceso tenant403 son categorías separadas. Las excepciones desconocidas no publican errores internos de persistencia.
- `GET /api/proyectos/pagina` y `GET /api/empresas/{id}/obras/pagina`: page0/size25 por defecto, size1–100, q opcional hasta160 caracteres sobre nombre/número/dirección. Respuesta `{content,page,size,totalElements,totalPages}`, orden id. Endpoints de listas anteriores conservados, sin truncado silencioso.

La separación de puertos de identidad/políticas conserva los adaptadores actuales. JPA sigue en el modelo existente; no es una conversión completa a dominio independiente de persistencia.

La verificación PostgreSQL optativa está en `scripts/quality-postgres.py`: contenedor nuevo tmpfs, loopback15438, base/usuario quality14_test, credenciales efímeras y cleanup propietario. No ejecutarla sin aprobación del entorno; no usa laboratorios anteriores ni archivos .env. `quality14.pg.enabled` permanece desactivado en verify normal. Los tests cubren rollback, concurrencia de horas/defaults/cupos, entidades leídas antes de lock y migración con duplicados en un esquema exclusivamente efímero.
