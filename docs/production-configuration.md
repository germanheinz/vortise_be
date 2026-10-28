# Configuración de producción

`prod` y `production` son perfiles equivalentes. Requieren PostgreSQL, Flyway y validación del esquema. No habilitan scripts SQL de arranque, consola H2, seeders de demo ni exportación/restauración H2. El guard comprueba la configuración efectiva antes de inicializar DataSource/JPA y rechaza overrides incompatibles, incluidos perfiles combinados que intenten usar H2.

Variables que debe configurar el operador mediante secretos del entorno (sin valores en el repositorio):

| Variable | Finalidad |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Seleccionar `production` o `prod` |
| `APP_DATABASE_JDBC_URL`, `APP_DATABASE_USERNAME`, `APP_DATABASE_PASSWORD` | Conexión PostgreSQL explícita |
| `APP_JWT_SECRET` | Clave de firma exigida por JwtService |
| `APP_ALLOWED_ORIGINS`, `APP_FRONTEND_URL` | Orígenes HTTPS explícitos admitidos por WebEnvironmentConfiguration |
| `APP_SUPERADMIN_EMAIL`, `APP_SUPERADMIN_PASSWORD` | Alta opcional y explícita del administrador; no rota una cuenta existente |

El catálogo existente conserva la sincronización de precios reconocidos y Price IDs configurados; no se crean planes ausentes mediante seeders de demo en estos perfiles. Revisar catálogo/migraciones y precios comerciales antes de habilitar Checkout. No se generan usuarios ni claves predeterminados. La configuración de correo continúa deshabilitada hasta activar un proveedor en otro hito.

Este documento no autoriza activar perfiles en procesos actuales, ejecutar migraciones ni desplegar. Antes del primer arranque remoto se requieren backup/restore revisado, credenciales propias de producción, migraciones verificadas y autorización de release. La configuración H2 local se mantiene exclusivamente para desarrollo.
