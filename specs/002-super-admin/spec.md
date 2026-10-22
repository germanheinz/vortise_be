# Superadministrador configurado en una base existente

## Aprobación y alcance

El usuario solicitó crear el superadministrador mediante el seeder para continuar las
pruebas aunque H2 ya esté poblada. Se aprobó que el seeder evalúe la cuenta de plataforma
configurada incluso cuando la política global haya desactivado los demás seeders de datos.

## Requisitos

- `SuperAdminSeeder` puede crear la cuenta superadmin configurada aunque la base ya tenga
	filas. No debe volver a ejecutar ni habilitar los seeders demo/configuración sobre una
	base existente.
- La cuenta solo se crea cuando están configurados `APP_SUPERADMIN_EMAIL` y
	`APP_SUPERADMIN_PASSWORD`, y la contraseña cumple el mínimo de 12 caracteres.
- No incluir usuario ni contraseña por defecto en el código, archivos de ejemplo,
	frontend o respuestas HTTP. La contraseña se guarda con el `PasswordEncoder` existente.
- Si el correo ya corresponde a un superadmin, no hacer cambios. Si corresponde a una
	cuenta normal, no elevarla; registrar advertencia y no sobrescribirla.
- No ejecutar el seeder ni alterar H2 como parte de este cambio.

## Verificación

Tests fuente actualizados para creación con base ya inicializada, validación de correo y
contraseña y no elevación de usuarios existentes. Ejecutar Maven en H2 aislada queda
pendiente; no se ejecutan tests ni operaciones sobre la base persistente durante esta
implementación.

## Uso para prueba local

Configurar en el entorno de ejecución del backend `APP_SUPERADMIN_EMAIL` con un correo
nuevo y `APP_SUPERADMIN_PASSWORD` con una contraseña elegida por el usuario de al menos
12 caracteres; reiniciar el backend y entrar al frontend con esos mismos valores. No
guardar la contraseña en este repositorio, en el frontend ni enviarla por chat. Si el
correo ya pertenece a un usuario normal, el seeder no lo convierte: elegir un correo
distinto. El seeder puede crear la cuenta aunque H2 ya contenga datos, pero no modifica
ni borra los demás registros.
