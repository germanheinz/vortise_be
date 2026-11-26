# 020 — Estado y evidencia

- [x] Revisión de descubrimiento del backend (auth, dominio, persistencia y configuración) con evidencia en archivo y línea. Ver [plan](plan.md#hallazgos-verificados).
- [x] Spec y plan aprobados el 2026-11-26.
  - D1: el login con contraseña se elimina en un corte posterior (020-b), no en este.
  - D2: la migración de las columnas de invitación se pospone.
- [x] Ejecución autorizada: `mvn verify` (línea base y final) y `mvn -Dtest=... test` por corte, desde `gestion_be`. Sin operaciones sobre la base persistente `./data`, sin commits ni push y sin tocar el frontend.
- [ ] C0 Línea base con `mvn verify`.
- [ ] C4 Excepciones de aplicación.
- [ ] C1 Invitaciones.
- [ ] C2 Contexto de sesión.
- [ ] C3 Búsqueda y propiedad por empresa.
- [ ] C6 Guardas de controllers.
- [ ] C7 Limpieza de código de prueba y código muerto.
- [ ] C5 Agrupación de paquetes.
- [ ] `mvn verify` final y revisión independiente.
