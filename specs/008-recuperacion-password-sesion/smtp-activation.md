# Activación posterior del correo

Implementación local verificada; entrega real y despliegue no forman parte del alcance autorizado de este hito. Ver [evidencia](tasks.md) y [plan](plan.md).

## Configuración fuera del repositorio

Mantener `APP_PASSWORD_MAIL_ENABLED=false` hasta configurar y validar el proveedor. Cuando corresponda, configurar en el gestor de secretos del hosting:

| Nombre | Uso |
| --- | --- |
| APP_PASSWORD_MAIL_ENABLED | Activa recuperación y notificaciones; false por defecto |
| APP_PASSWORD_MAIL_FROM | Remitente autorizado por el proveedor, sin saltos de línea |
| SPRING_MAIL_HOST | Host SMTP del proveedor |
| SPRING_MAIL_PORT | Puerto con STARTTLS; predeterminado587 |
| SPRING_MAIL_USERNAME | Usuario SMTP, secreto |
| SPRING_MAIL_PASSWORD | Credencial SMTP, secreto |
| APP_FRONTEND_URL | Origen frontend exacto permitido; el enlace usa /restablecer-password#token=… |

No pegar secretos en el chat, terminal, docs, fixtures, URLs o archivos versionados. No reemplazar el conjunto completo de variables del hosting al configurar estas claves. No utilizar las credenciales de bootstrap como contraseña SMTP.

El adaptador usa SpringMail, STARTTLS obligatorio, comprobación de identidad del servidor TLS, autenticación y timeouts5s. Consulta de referencia: [Spring Boot3.2 — Sending Email](https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/io.html#io.email).

## Compatibilidad con hosting

Si el backend continúa en Render Free, el SMTP convencional de este adaptador no puede activarse en los puertos25/465/587: [Render documenta su bloqueo en Free](https://render.com/docs/free#other-limitations). El puerto25 se bloquea en todos los planes. Resolver transporte y conectividad con el proveedor antes de activar: un puerto STARTTLS admitido por ambos, un entorno compatible o un adaptador de API de correo bajo un alcance posterior. No se eligió proveedor ni se autorizó gasto, plan de pago o evasión de restricciones.

Detrás de proxy, el límite por IP usa la dirección de conexión y no confía en X-Forwarded-For arbitrario. Puede agrupar clientes bajo la IP del proxy. Configurar el proxy de confianza/WAF de forma específica al entorno antes de abrir la recuperación al público. Las cuotas globales y por email persisten y se comparten entre instancias.

## Secuencia de validación cuando se autorice

1. Verificar proveedor, conectividad, remitente y SPF/DKIM/DMARC. Usar buzón de prueba controlado.
2. Aplicar V8 sin editar migraciones anteriores, con respaldo o excepción revisada. El despliegue requiere nueva autorización; los JWT anteriores sin versión exigirán nuevo login. Backups H2 anteriores tienen fingerprint de esquema diferente y deben tratarse como incompatibles hasta revisión.
3. Configurar SMTP y activar envíos; comprobar arranque y diagnóstico sin divulgar secretos. No usar `spring.mail.test-connection` en pruebas automatizadas.
4. Solicitar recuperación de cuenta controlada; confirmar respuesta202 idéntica para cuenta ausente y presente, enlace con token en fragmento y retirada inmediata de la URL.
5. Confirmar nueva contraseña, correo de aviso y rechazo inmediato de JWT antiguo; repetir enlace y verificar rechazo. Comprobar login nuevo y contraseña anterior rechazada.
6. Revisar logs sanitizados y límites. Cambiar también la configuración de bootstrap si corresponde, sabiendo que cambiar APP_SUPERADMIN_PASSWORD no modifica una cuenta ya persistida.

La cola SMTP está acotada en memoria; no contiene persistencia de tokens/cuerpos. Un reinicio puede perder un envío en cola, y un fallo de entrega no se reintenta automáticamente. El usuario puede solicitar otro enlace tras el cooldown. Un fallo del aviso no revierte una contraseña ya confirmada. La prueba SMTP de este hito sustituye el transporte; no acredita recepción real ni validación de un proveedor.
