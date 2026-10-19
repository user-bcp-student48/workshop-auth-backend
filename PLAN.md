# Plan de corrección

## Alcance y selección

Los apartados H-02 y H-04 siguientes documentan el plan anterior y ya están resueltos. El alcance vigente de este plan son todos los items que continúan `Pendiente` en `HALLAZGOS.md`: H-01, H-03, H-05, H-06 y H-07. Se mantienen separados por item.

El contrato de `docs/ENDPOINTS.md` se conserva. `POST /auth/login` mantiene sus códigos HTTP, campos y valores documentados, incluyendo `expiresIn: 3600` y el objeto `user`. El contenido del token permanece opaco en la respuesta. Antes del despliegue de un token nuevo, coordinar su validación con consumidores externos sin ampliar el alcance de este repositorio.

## Historial resuelto: H-02 — Concatenación de credenciales en SQL

**Hallazgo:** H-02, “Concatenación de credenciales en SQL”, Vulnerabilidad alta, estado `Resuelto`.

**Problema:** `LegacyDatabase.findByCredentials` concatena `username` y `password` en la sentencia. Una entrada puede alterar la consulta y la decisión de autenticación. Además, los errores SQL quedan convertidos en ausencia de usuario.

**Cambio propuesto:** usar `PreparedStatement` con placeholders para usuario y contraseña, enlazados con `setString`. Conservar las columnas consultadas, el criterio de búsqueda, el retorno de `UserRecord` y el comportamiento HTTP 401 cuando no hay coincidencia. Mantener el método de persistencia enfocado en ejecutar la consulta. La extracción transversal de Resource/Service/Repository pertenece al H-07 y no se mezcla en este cambio.

**Archivos a tocar:**

- `src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`
- `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`; si se opta por aislar la persistencia, agregar `src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java`.

**Caracterización antes de tocar implementación:** ampliar las pruebas para fijar login válido → 200 y credenciales inválidas → 401. Añadir un username literal con una comilla, por ejemplo `O'Brien`, y comprobar que se trata como dato y no autentica (401), sin payloads de explotación. Ejecutar con base de prueba aislada para evitar depender de datos locales. Estas pruebas fijan el comportamiento HTTP previo a parametrizar la consulta.

## Historial resuelto: H-04 — Token Base64 con secreto embebido y sin firma

**Hallazgo:** H-04, “Token Base64 con secreto embebido y sin firma”, Vulnerabilidad alta, estado `Resuelto`.

**Problema:** Base64 no aporta firma ni integridad; el contenido, incluido el secreto, se recupera al decodificar el token. El vencimiento del token también debe seguir correspondiendo a `expiresIn` de la respuesta.

**Cambio propuesto:** sustituir el formato casero por un JWT firmado con claims explícitos: sujeto/usuario (`sub`), rol, emisor y expiración (`exp`). Leer la clave de firma desde configuración externa; no guardar secretos reales en código o pruebas. Usar una constante nombrada para el TTL de 3600 segundos y compartirla para generar `exp` y `expiresIn`, evitando números mágicos y duplicación. Mantener métodos pequeños y con una sola responsabilidad; no añadir anidamiento al flujo de login.

No existe una dependencia JWT declarada en el `pom.xml` actual. Si la comprobación del árbol confirma que no hay una API adecuada disponible, agregar `io.quarkus:quarkus-smallrye-jwt-build` con versión administrada por el BOM. Se justifica para emitir un estándar JOSE firmado y evitar implementar criptografía/serialización propia. No añadir otra biblioteca si una ya está disponible y es compatible.

**Archivos a tocar:**

- `pom.xml`, solo si se confirma necesaria la dependencia de construcción JWT.
- `src/main/resources/application.properties` para enlazar configuración externa de la clave, sin valor secreto comprometido.
- `src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java` para sustituir Base64 por emisión firmada y, si corresponde, renombrar la clase para reflejar su responsabilidad.
- `src/main/java/com/bcp/workshop/auth/AuthResource.java` para consumir el generador y el TTL nombrado, sin alterar la respuesta HTTP.
- `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` para fijar el contrato.
- `src/test/java/com/bcp/workshop/auth/JwtTokenServiceTest.java` (nuevo, o nombre equivalente si se conserva `LegacyTokenUtil`) para verificar firma, claims y expiración.

**Caracterización antes de tocar implementación:** ampliar el test de login exitoso para comprobar 200, token no vacío, `expiresIn` igual a 3600 y los campos/valores de `user` descritos en `docs/ENDPOINTS.md`. No afirmar un formato concreto del token actual, pues es opaco según el contrato y la codificación Base64 no debe quedar congelada. Tras la caracterización, añadir pruebas del generador firmado con clave de prueba aislada: verificar firma mediante clave pública, claims requeridos y consistencia entre `exp` y el TTL reportado.

## Secuencia

1. Escribir y ejecutar primero las pruebas de caracterización para ambos hallazgos contra el comportamiento actual.
2. Implementar H-02 de forma aislada y ejecutar sus pruebas y la suite del backend.
3. Comprobar disponibilidad de la API JWT; implementar H-04, agregar la dependencia solo si es necesaria, y ejecutar pruebas de firma y contrato.
4. Ejecutar `mvn verify` y revisar que la cobertura del código modificado alcance al menos 85%. No silenciar CodeQL, Dependency Review ni JaCoCo.

## Item: H-01 — SCA: Commons Text 1.9 y Quarkus BOM 3.27.4

**Hallazgo:** H-01, Vulnerabilidad alta, estado `Pendiente`.

**Problema:** Commons Text 1.9 está declarado y el análisis registrado detectó CVE-2022-42889. El mismo análisis no reportó CVE para Quarkus BOM 3.27.4, por lo que esa versión no debe cambiarse sin evidencia de advisory aplicable.

**Cambio propuesto:** ejecutar SCA actualizado sobre las dependencias resueltas. Actualizar Commons Text a una versión mantenida sin el advisory, o retirarlo sustituyendo `StringEscapeUtils.escapeHtml4` por una API segura disponible. Actualizar Quarkus solo si un advisory vigente lo requiere. Mantener activos CodeQL y Dependency Review.

**Archivos previstos:** `pom.xml`; `src/main/java/com/bcp/workshop/auth/AuthResource.java` solo si se reemplaza la API de escape; `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` para verificar `displayName`; `HALLAZGOS.md` para registrar versión y evidencia del escaneo.

**Prueba antes de tocar implementación:** guardar el resultado SCA baseline de las coordenadas afectadas y ejecutar `mvn test`. Caracterizar que `displayName` conserva el escape HTML actual. Tras el cambio, repetir la misma prueba y el escaneo, además de Dependency Review.

## Item: H-03 — Contraseñas almacenadas en texto plano

**Hallazgo:** H-03, Vulnerabilidad alta, estado `Pendiente`.

**Problema:** las cuentas semilla y los registros SQLite existentes almacenan contraseñas legibles; una copia de la base expone credenciales reutilizables.

**Cambio propuesto:** guardar y verificar hashes BCrypt con salt y no registrar contraseñas ni hashes. Comprobar primero si existe una API compatible en el árbol; añadir una dependencia Quarkus mantenida solo si hace falta. Diseñar una migración SQLite transaccional y versionada que convierta una sola vez los registros plaintext preexistentes y cree las cuentas semilla con hash, manteniendo el login y sus respuestas.

**Archivos previstos:** `pom.xml` solo si falta la API BCrypt; `src/main/java/com/bcp/workshop/auth/LegacyDatabase.java` para migración/persistencia; `PasswordService.java` nuevo si la verificación debe separarse del repositorio; `LegacyDatabaseTest.java` nuevo y `AuthResourceTest.java`.

**Prueba antes de tocar implementación:** con una base temporal, caracterizar login válido de `student`/`trainer` e inválido, y registrar el esquema/filas legacy iniciales. No codificar el plaintext como resultado esperado. Las pruebas posteriores deben demostrar que el valor guardado no es la contraseña, que BCrypt la verifica y que una segunda inicialización no vuelve a transformar filas migradas.

## Item: H-05 — Secreto y CORS permisivos

**Hallazgo:** H-05, Vulnerabilidad media, estado `Pendiente`.

**Problema:** `application.properties` permite todos los orígenes con `/.*/`. La parte del secreto parece ya cubierta por H-04, que externalizó la clave como `AUTH_JWT_PRIVATE_KEY_LOCATION`; este plan se limita al CORS restante y no vuelve a modificar el token.

**Cambio propuesto:** reemplazar el patrón global por una allowlist de orígenes configurable externamente. Confirmar con los responsables del cliente los orígenes reales; no inventar dominios de producción. Conservar métodos y headers actuales salvo decisión documentada.

**Archivos previstos:** `src/main/resources/application.properties`; `src/test/java/com/bcp/workshop/auth/CorsPolicyTest.java` nuevo; documentación de configuración si se agrega una variable; `HALLAZGOS.md` para aclarar la parte ya resuelta y cerrar solo tras validar la allowlist.

**Prueba antes de tocar implementación:** escribir una prueba de preflight para origen permitido y no permitido usando dominios `.test` solo como fixtures. Registrar el comportamiento actual; la expectativa final será que el permitido recibe `Access-Control-Allow-Origin` y el no permitido no. Resolver los orígenes reales de despliegue como pregunta previa al cambio.

## Item: H-06 — La respuesta 500 expone detalles internos

**Hallazgo:** H-06, Vulnerabilidad media, estado `Pendiente`.

**Problema:** el catch del login incluye `error.getMessage()` y `error.getClass().getName()` en la respuesta, filtrando información interna.

**Cambio propuesto:** conservar HTTP 500 pero responder con mensaje genérico y estable, sin campo `exception`; enviar detalles solo al log del servidor y no registrar tokens, claves ni credenciales. Actualizar el cuerpo 500 en `docs/ENDPOINTS.md`, ya que este hallazgo pide explícitamente eliminar el formato actual.

**Archivos previstos:** `src/main/java/com/bcp/workshop/auth/AuthResource.java` o un mapper de errores dedicado; `src/test/java/com/bcp/workshop/auth/AuthResourceErrorTest.java`; `docs/ENDPOINTS.md`; `HALLAZGOS.md`.

**Prueba antes de tocar implementación:** usar una clave de firma ausente en fixture de test para inducir una excepción y caracterizar el 500 actual. Después cambiar la expectativa a status 500, mensaje genérico y ausencia de clase/mensaje interno. Verificar además que el log no registre datos sensibles.

## Item: H-07 — `AuthResource.login` acumula responsabilidades

**Hallazgo:** H-07, Refactor, severidad baja, estado `Pendiente`.

**Problema:** el resource valida, accede a persistencia, genera el token y construye la respuesta dentro de un método con anidamiento profundo.

**Cambio propuesto:** aplicar las capas de `docs/GOOD_PRACTICES.md`: Resource para HTTP, Service para reglas de login y Repository para persistencia; mapear `UserRecord` a DTO de salida. Extraer validaciones con guard clauses, evitar duplicación y mantener nombres de propósito. No cambiar códigos/campos del contrato ni mezclar la migración de contraseñas de H-03 o el saneamiento 500 de H-06.

**Archivos previstos:** `src/main/java/com/bcp/workshop/auth/AuthResource.java`; nuevos `AuthService.java`, `UserRepository.java`/implementación y DTO de respuesta; `LegacyDatabase.java` para implementar el repositorio; tests de caracterización y unitarios del servicio.

**Prueba antes de tocar implementación:** ejecutar y conservar la matriz HTTP de `docs/ENDPOINTS.md`: login válido, `client` opcional, body/campos nulos o vacíos, credenciales inválidas y error 500 vigente al iniciar H-07. Probar mapeo de respuesta y servicio con datos aislados. Repetir la matriz tras el refactor y comparar status y campos.

## Orden y quality gates para los pendientes

1. H-01: SCA baseline, actualización/retirada aislada y reescaneo.
2. H-03: caracterización con SQLite temporal y migración BCrypt.
3. H-05: confirmar allowlist, probar CORS y restringir orígenes.
4. H-06: caracterizar el 500, sanearlo y actualizar su contrato.
5. H-07: separar capas con la matriz contractual verde.

En cada item, escribir y ejecutar primero su prueba de caracterización. No silenciar CodeQL, Dependency Review ni JaCoCo. Tras cambios Java, ejecutar `mvn verify` y verificar al menos 85% de cobertura del código modificado.