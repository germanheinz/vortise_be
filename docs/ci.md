# Backend CI

`.github/workflows/ci.yml` ejecuta verify en Ubuntu 24.04 con Java 17 para PR,
push a main y ejecución manual. Los tres grupos PostgreSQL optativos quedan
desactivados; las pruebas HTTP conservan su guard H2 efímero. Correo, superadmin
y Stripe están deshabilitados/vacíos. No configurar secretos de aplicaciones.

El E2E integrado vive en el repositorio frontend `germanheinz/vortise_fe`.
Comprueba un SHA backend completo fijado allí; una PR backend por sí sola no
lo dispara. Antes de aceptar cambios de contrato, ejecutar ese workflow manual
con el SHA backend publicado y registrar la pareja. La primera referencia debe
incluir la adaptación Linux de `E2eLocalConfiguration`.

Los workflows no despliegan ni alteran protección de ramas o configuración de
Render. Verificación local y ejecución real de GitHub se registran por separado.
