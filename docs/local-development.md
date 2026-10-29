# Desarrollo local desde main

Las funcionalidades de recuperación, calidad e invitaciones están integradas en main. Usar main actualizado en ambos repositorios; no mezclar sus fuentes con target/classes compilado desde otra rama.

Si se cambia de rama o versión de Spring Boot, ejecutar primero `mvn clean -DskipTests test-compile` para regenerar compilados sin iniciar la aplicación. Para pruebas, seguir docs/testing-checklist.md y el alcance aprobado: H2 memoria protegida, PostgreSQL optativo desactivado y servicios externos deshabilitados. No usar el arranque por defecto como comprobación: puede abrir H2 persistente y actualizar esquema/datos.

Los archivos .env.* y nuevas bases data/*.db se ignoran. Las bases históricas ya rastreadas se conservan en el repositorio: este ajuste no las elimina. Los cambios de la base local se excluyen del índice de trabajo mediante skip-worktree; no agregarlos a commits ni borrar archivos para arreglar compilación. Antes de cambiar de rama con datos existentes, preservar esos archivos fuera del repositorio. Las ramas de trabajo ya integradas pueden eliminarse después de confirmar su inclusión en main.

El frontend fija una revisión backend en .github/ci/backend.sha. Actualizar ese pin únicamente a un commit backend publicado que incluya los contratos, fixtures E2E y migraciones correspondientes.
