# Monitoreo y reversión — operación inicial

Alcance y autorizaciones en [MONITORING-ROLLBACK-12](../../docs/specs/MONITORING-ROLLBACK-12/tasks.md). Estos comandos no autorizan despliegues, notificaciones, cambios de tráfico ni bases de datos.

## Señales disponibles en el código

| Señal | Uso | Acceso |
| --- | --- | --- |
| GET/actuator/health/liveness | Estado de vida del proceso; no incorpora DB | Público, solo estado |
| GET/actuator/health/readiness | Puede atender trabajo con DB disponible | Público, solo estado |
| GET/actuator/health | Estado general sin componentes/detalles | Público, solo estado |
| GET/actuator/metrics y métricas específicas | Diagnóstico mediante Actuator/Micrometer | Solo superadmin |
| GET/api/health en frontend | Servidor Next responde; no comprueba DB ni navegador | Público, no-store |

Los endpoints nuevos solo estarán en el servicio alojado después de desplegar el candidato correcto. Otros endpoints Actuator quedan denegados incluso al superadmin; no abrir env/configprops/heapdump/loggers para diagnosticar. No pegar JWT/headers de autenticación en chat o comandos compartidos.

X-Request-ID se genera en servidor para cada petición síncrona y se devuelve en la respuesta. Los registros http_request_completed tienen request_id, método normalizado, plantilla MVC o UNMATCHED, status y duration_ms. No contienen parámetros, cuerpo, principal ni cabeceras de la petición. El fallback inesperado emite unexpected_server_error con tipo de excepción y correlación MDC; no imprime message ni Throwable. MDC se restaura al finalizar.

Boot usa formato JSON nativo para consola. Esto no convierte automáticamente todos los logs existentes/dependencias en seguros: respetar exclusión de payloads/PII, restringir quién lee logs y revisar cualquier nuevo logger. No se agregó colector de errores JavaScript de usuarios ni panel de incidentes. Métricas son diagnóstico, no un servicio histórico de alertas.

## Sondeo puntual

Desde gestion_be, únicamente con ejecución aprobada:

```sh
python3 scripts/service-health.py --environment local
```

Destinos fijos locales: API18081 y frontend3001, en127.0.0.1. No arranca servicios; si no están listos falla con exit1. No usarlo como justificación para conectarse a bases existentes.

Modo staging (requiere autorización remota separada):

```sh
python3 scripts/service-health.py --environment staging --allow-staging --timeout 10
```

Solo usa HTTPS y hosts de staging conocidos. No acepta URLs arbitrarias, secretos ni query, no hereda proxies ni sigue redirects. Lee como máximo4097bytes y no publica contenido de respuesta ni errores de red internos. Salida: resultado por señal, códigoHTTP, duración y razón genérica; exit0 si todo responde200/statusUP. No se ejecuta periódicamente ni envía avisos por sí mismo.

## Alertas pendientes de activación

Dueño de respuesta inicial: dueño de plataforma; confirmar destinatario/canal antes de configurar. Usar primero notificaciones nativas de Render para build/deploy fallido y servicio unhealthy, revisando permisos y preferencias del workspace/servicio. Vercel: revisar notificaciones y herramientas de observabilidad disponibles en el plan real, sin dar por hecho cobertura equivalente. No se configuraron notificaciones ni se probó su entrega en esta implementación.

Propuesta inicial a validar:3fallos consecutivos en3min abren incidente de disponibilidad. En Free un arranque frío puede tardar aproximadamente1min: registrar tiempo de recuperación y repetir con espera acotada antes de diagnosticar caída sostenida. Un ping frecuente puede alterar el comportamiento de suspensión; decidir frecuencia/coste antes de programarlo.

Investigación adicional si5xx>5% con>=20requests/5min o p95>2s durante5min. Son umbrales de partida, no alarmas activas ni garantía/SLO. El sondeo puntual no calcula esas ventanas. Requieren un recolector/scheduler y fuente de métricas que aún deben configurarse. No contratar herramientas externas ni conectar Slack en este alcance.

ReadinessDOWN y livenessUP orientan primero a DB/dependencias. Un rollback de aplicación no corrige necesariamente una caída de PostgreSQL. Si se configura RenderHTTPhealth sobre readiness, una indisponibilidad persistente de DB puede disparar reinicios del servicio: evaluar ese efecto antes de activar. No provocar fallos artificiales en Render para probar alertas sin aprobación de ensayo.

## Procedimiento de incidente y rollback

1. Confirmar acceso y destino. Capturar IDs y commits del par FE/BE vivo, estado de salud, hora y señal; consultar logs con requestid sin publicar payloads. Verificar que es staging autorizado aunque Vercel lo denomine production.
2. Clasificar: fallo de build no es fallo del candidato activo; readinessDBDOWN no implica regresión del código. Evitar rollback automático por un único timeout/coldstart.
3. Identificar un par FE/BE anterior realmente elegible. Registrar API/contratos, Flyway mínimo/máximo, sesiones/JWT, configuración aplicable y medidas de seguridad. Si volver atrás resucita sesiones antiguas, expone fallos corregidos o no tolera el esquema actual, detener y preparar corrección hacia adelante.
4. No revertir migraciones ni restaurar DB como parte de rollback de código. No hay respaldo real de Render verificado en esta fase: el ensayo local de backup no sustituye esa evidencia.
5. Preparar operación revisable con deploymentIDs específicos, orden compatible de FE/BE, resultado esperado y cómo retornar al candidato actual. Solicitar aprobación concreta antes de alterar tráfico/configuración.
6. Render: comprobar artefacto retenido. Dashboard rollback desactiva auto-deploy; API no lo desactiva. No cambiar autodeploy sin autorización; evitar un push que reintroduzca el candidato fallido. Revisar entorno/configuración aplicada por rollback sin volcar valores.
7. Vercel: elegibilidad depende del plan y de haber servido el dominio correspondiente. Hobby permite el inmediatamente anterior; no asumir que cualquier preview es elegible. Rollback desactiva auto-asignación hasta promover un candidato aprobado. Configuración compilada/variables antiguas pueden diferir de los valores actuales del proyecto.
8. Tras operación autorizada: esperar estados terminales, comprobar salud FE/BE, login y flujo esencial con datos sintéticos, revisar errores y registrar IDs/commits/resultados. No declarar éxito solo porque se aceptó la solicitud de rollback. Definir cuándo restaurar despliegues automáticos y comprobarlo.

No se ha seleccionado ni ensayado un candidato de reversión todavía. Una simulación de respuestas o un runbook escrito no equivalen a rollback remoto verificado.

Fuentes: [Renderhealth](https://render.com/docs/health-checks), [Renderrollback](https://render.com/docs/rollbacks), [notificaciones](https://render.com/docs/notifications), [Vercelrollback](https://vercel.com/docs/instant-rollback).
