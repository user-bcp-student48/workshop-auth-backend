# Tasks

## 1. Caracterización del contrato existente

- [x] 1.1 Asegurar que las pruebas HTTP de `POST /auth/login` caractericen los logins válidos, las respuestas `401` para credenciales inválidas, las validaciones `400`, el username con comilla y el uso sin `client`; verificar ejecutando `mvn -Dtest=AuthResourceTest test`.

## 2. Regresión de autenticación ante entradas manipuladas

- [x] 2.1 Añadir o completar pruebas HTTP con texto de sintaxis SQL en `username` y `password`, usando pares que no sean credenciales válidas; verificar que cada caso responde `401` sin emitir un token al ejecutar `mvn -Dtest=AuthResourceTest test`.

## 3. Corrección acotada si se reproduce un bypass

- [x] 3.1 Si alguna prueba de regresión demuestra autenticación sin credenciales válidas, corregir únicamente la consulta de credenciales en `LegacyDatabase` para tratar ambos valores como datos; si no se reproduce el bypass, no modificar la lógica funcional y registrar el resultado de la verificación. En ambos casos, verificar que las regresiones y los casos de login válido pasan con `mvn -Dtest=AuthResourceTest test`.

## 4. Verificación de integración

- [x] 4.1 Ejecutar la suite Maven completa y verificar que todas las pruebas pasan y que el diff funcional se limita a la corrección aprobada, si fue necesaria, sin cambios en otros endpoints ni en el contrato documentado.

## Evidencia de caracterizacion (2026-10-21)

- Issue: user-bcp-student48/workshop-auth-backend#11. Rama conservada: `lab-complete`.
- Baseline Surefire: 13 pruebas de `AuthResourceTest` y 1 de `JwtTokenServiceTest`, sin fallos, errores ni omisiones.
- `mvn -Dtest=AuthResourceTest,JwtTokenServiceTest test`: 17 pruebas, 0 fallos, 0 errores, 0 omitidas; `BUILD SUCCESS`, salida 0.
- `mvn -Dtest=AuthResourceTest test`: 16 pruebas, 0 fallos, 0 errores, 0 omitidas; `BUILD SUCCESS`, salida 0.
- Cada ejecucion uso `AUTH_DB_URL` con un archivo SQLite nuevo bajo `target/`; se restauro la variable al terminar. Solo datos ficticios de laboratorio.
- Se reutilizaron las pruebas del Dia 1, reforzando respuestas validas y ausencia de token en rechazos. Se agregaron contraseña incorrecta de usuario existente y tres entradas SQL por campo, todas rechazadas con `401` sin token.
- No se modifico codigo de produccion. Las tareas 3.1 y 4.1 no se ejecutaron en esta etapa.
- Riesgo de entorno: el runtime registro `IllegalAccessError`/`NoClassDefFoundError` en hilos JBoss por acceso a `java.lang` en Java 24+, aunque Surefire y Maven finalizaron correctamente. No se modificaron flags JVM ni gates; queda pendiente repetir en el JDK 21 documentado.
- Detalle de requisitos protegidos y reproduccion: `docs/TESTING.md`, seccion de caracterizacion de `prevent-sql-injection-auth`.

## Evidencia de apply (2026-10-21)

- Workflow: `openspec instructions apply --change prevent-sql-injection-auth --json`, estado inicial `ready`, 2/4 tareas completas, salida 0.
- Tarea 3.1: `mvn -Dtest=AuthResourceTest test`, 16 pruebas, 0 fallos, 0 errores, 0 omitidas; `BUILD SUCCESS`, salida 0. Log: `target/apply-auth-test.log`.
- No se reprodujo bypass con las entradas representativas de las regresiones HTTP. La inspeccion de `LegacyDatabase.findByCredentials` confirma consulta estatica con `username=? AND password=?` y ambos valores ligados con `PreparedStatement.setString`.
- Conforme a 3.1 y al diseño aprobado, se conserva el codigo funcional existente; no se introducen cambios cosmeticos ni se declara que estos casos prueben toda variante posible de inyeccion.
- Se uso un SQLite ficticio nuevo bajo `target/` mediante `AUTH_DB_URL`, restaurando la variable al terminar. Persisten los errores de hilos JBoss ya registrados; no se alteraron JVM, dependencias ni controles para ocultarlos.
- Tarea 4.1: `mvn test`, 20 pruebas (16 login, 1 error de login, 2 health, 1 JWT), 0 fallos, 0 errores, 0 omitidas; `BUILD SUCCESS`, salida 0. Log: `target/apply-full-test.log`; reportes en `target/surefire-reports/`.
- `git diff --exit-code HEAD -- src/main pom.xml docs/ENDPOINTS.md .github/workflows`: salida 0, sin cambios funcionales, contractuales ni de controles. Los cambios previos de configuracion y archivos sin seguimiento del usuario se conservaron; no forman parte de la implementacion de esta etapa.
- `mvn --version`: Maven 3.10.0, Microsoft Java 25.0.4.1. La suite pasa, pero los errores asincronos de JBoss Threads impiden declarar el runtime libre de errores; queda pendiente una ejecucion con JDK 21. No se modificaron versiones ni opciones de la JVM en esta HU.
- Todas las tareas del plan tienen evidencia local; CI, porcentaje de cobertura y revision del PR siguen siendo gates separados, no aprobados por este registro.

## Revalidacion con JDK 21 (2026-10-21)

- Se selecciono el JDK 21 ya instalado mediante `JAVA_HOME` y PATH exclusivamente en la sesion PowerShell de ejecucion, sin modificar configuracion global, dependencias ni opciones JVM.
- `mvn --version`: Maven 3.10.0, Eclipse Adoptium Java 21.0.12.1.
- `mvn test`: 20 pruebas, 0 fallos, 0 errores, 0 omitidas; `BUILD SUCCESS`, salida 0. SQLite ficticio nuevo bajo `target/`; `AUTH_DB_URL` restaurado al terminar.
- Log completo: `target/jdk21-full-test.log`. Busqueda de `IllegalAccessError|NoClassDefFoundError|Exception in thread`: 0 coincidencias. El riesgo de entorno observado con Java 25 no se reprodujo con JDK 21; el gate local de reejecucion queda cumplido.
- CI remoto, cobertura >=85% y revision del PR siguen pendientes de evidencia. Las terminales existentes distintas de esta sesion no cambian de JDK automaticamente.
