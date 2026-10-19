# Resultado de tests

- Fecha: 2026-10-19
- Proyecto: `workshop-auth-backend`
- Ejecución: runner Java del proyecto (Maven)
- Resultado general: **PASS**
- Total: 4 tests, 0 fallos, 0 errores, 0 omitidos

## Detalle

| Suite | Tests | Fallos | Errores | Omitidos | Resultado |
|---|---:|---:|---:|---:|---|
| `AuthResourceTest` | 3 | 0 | 0 | 0 | PASS |
| `JwtTokenServiceTest` | 1 | 0 | 0 | 0 | PASS |

Salida del ejecutor: `All tests passed! There are no test errors/failures in the project.`

## TEST_01 — `AuthResourceTest.java`

**Propósito:** probar el flujo HTTP de `POST /auth/login` y que el cliente conserve el contrato.

**Casos actuales:**

- Login válido: espera 200, usuario y campos de respuesta documentados, token no vacío y `expiresIn` igual a 3600.
- Credenciales inválidas: espera 401 y el mensaje de error contractual.
- Username con comilla (`O'Brien`): verifica que la entrada se trate como valor literal y no autentique; espera 401.

**Resultado más reciente:** 3 tests, 0 fallos, 0 errores, 0 omitidos (PASS), según Surefire.

**Comparación con el archivo inicial:** antes de los cambios H-02/H-04, este archivo solo contenía el caso de login válido y comprobaba 200, `user.username` y token no nulo. Se añadieron las comprobaciones de contrato y los casos inválido/comilla para caracterizar H-02 antes y después de parametrizar SQL. Para H-04, se ampliaron las aserciones del token opaco y del TTL antes de reemplazar Base64.

## TEST_02 — `JwtTokenServiceTest.java`

**Propósito:** probar directamente la corrección criptográfica de H-04, complementando la prueba HTTP de TEST_01.

**Comprobaciones:** formato JWT de tres segmentos, firma RSA válida usando la clave pública de la pareja efímera del test, claims `sub`, `role` e `iss`, y `exp` dentro del TTL de 3600 segundos. La clave privada se crea en un archivo temporal durante la prueba y se elimina al detener el recurso de test; no se usa un secreto real.

**Resultado más reciente:** 1 test, 0 fallos, 0 errores, 0 omitidos (PASS), según Surefire.

## Comparación y conclusiones

| Aspecto | TEST_01 — `AuthResourceTest.java` | TEST_02 — `JwtTokenServiceTest.java` |
|---|---|---|
| Nivel | Integración HTTP del endpoint | Prueba del generador de token |
| Verifica | Estados HTTP y compatibilidad de respuesta para login válido/inválido | Firma, claims y expiración criptográfica |
| Tests actuales | 3 | 1 |
| Resultado | PASS | PASS |
| Cambio que cubre | H-02: SQL parametrizado, incluido el username con comilla; contrato de login | H-04: reemplazo de Base64 por JWT firmado y TTL compartido |

Las suites son complementarias: TEST_01 detecta regresiones visibles para el cliente y TEST_02 evita que una respuesta HTTP correcta oculte un token inválido. En la primera corrida de caracterización de H-02, el ejecutor informó que todos los tests pasaron, pero no devolvió conteos; el resultado cuantificado disponible corresponde a la corrida actual: 4 tests, 0 fallos, 0 errores, 0 omitidos.
