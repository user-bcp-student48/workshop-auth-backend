# Estrategia y resultados de pruebas

Alcance: endpoints documentados en `docs/ENDPOINTS.md`. No se agregan expectativas para respuestas que ese contrato deja abiertas.

## Matriz de cobertura

| endpoint | caso | tipo | resultado esperado |
|---|---|---|---|
| `POST /auth/login` | `student` con contraseña válida y `client` | Camino feliz | 200; token no vacío; `expiresIn=3600`; usuario, rol y `debug` con los valores del contrato. |
| `POST /auth/login` | Login válido sin `client` | Borde | 200; el campo opcional no es requisito. |
| `POST /auth/login` | Username literal con comilla | Borde | 401 y mensaje contractual; la entrada se trata como valor SQL. |
| `POST /auth/login` | Body JSON `null` | Error | 400, `{"message":"Request is null"}`. |
| `POST /auth/login` | Username ausente, nulo o en blanco | Error/borde | 400, `{"message":"Username is required"}`. |
| `POST /auth/login` | Password ausente, nulo o en blanco | Error/borde | 400, `{"message":"Password is required"}`. |
| `POST /auth/login` | Credenciales no coincidentes | Error | 401 y `Invalid user <username> or password`. |
| `POST /auth/login` | Firma JWT falla por clave no disponible | Error | 500 con propiedades `message` y `exception`; sus valores dependen de la excepción. |
| `GET /q/health` | Instancia saludable | Camino feliz | 200 con `status=UP`. |
| `GET /q/health` | Sin checks personalizados | Borde | 200 con `checks=[]`. |
| `GET /q/health` | Estado DOWN/error | Pendiente | `ENDPOINTS.md` no define código/cuerpo para DOWN y el proyecto no registra health checks propios; no se afirma un resultado hasta confirmar el comportamiento runtime. |

## Ejecución y resultados

Proyecto/directorio de trabajo: `workshop-auth-backend/` (raíz con `pom.xml`). Las pruebas son Quarkus Test/JUnit 5; las de HTTP usan REST Assured.

### Ejecución de tests

- Herramienta ejecutada: runner de tests Java `appmod-run-tests-for-java`, con `projectPath` en la raíz del repositorio.
- Comando Maven reproducible: `mvn test` (el runner no expuso la línea de comando interna ni un código numérico de salida).
- Resultado comunicado por el runner: `All tests passed! There are no test errors/failures in the project.`
- Código de salida numérico: no expuesto por la herramienta; el resultado fue PASS.
- Directorio: `workshop-auth-backend/`.
- Resultado Surefire actual: **15 tests, 0 fallos, 0 errores, 0 omitidos**.

| clase | tests | fallos | errores | omitidos | resultado |
|---|---:|---:|---:|---:|---|
| `AuthResourceTest` | 11 | 0 | 0 | 0 | PASS |
| `AuthResourceErrorTest` | 1 | 0 | 0 | 0 | PASS |
| `HealthEndpointTest` | 2 | 0 | 0 | 0 | PASS |
| `JwtTokenServiceTest` | 1 | 0 | 0 | 0 | PASS |

Reportes Surefire: `target/surefire-reports/com.bcp.workshop.auth.*.txt`.

### Build y cobertura

- Herramienta ejecutada: `appmod-build-java-project`, con `projectPath` en la raíz del repositorio.
- Resultado: build PASS, sin errores de compilación reportados. La herramienta no expuso el comando Maven interno ni código de salida numérico.
- Método configurado en `pom.xml`: JaCoCo 0.8.12, goal `prepare-agent` y goal `report` ligado a la fase Maven `verify`. El reporte esperado por clase es `target/site/jacoco/index.html` (y CSV, si se genera).
- **Cobertura del código modificado >=85%: pendiente de verificación.** Tras el build no se encontraron `target/jacoco.exec`, `target/site/jacoco/index.html` ni `target/site/jacoco/jacoco.csv`; por tanto no hay porcentaje medido que se pueda reportar sin inventarlo. En un entorno con Maven disponible, ejecutar `mvn verify` desde `workshop-auth-backend/` y revisar el reporte JaCoCo por clase modificada.

## Fallos observados

- Durante la primera ejecución de los tests nuevos, la configuración de clave privada inexistente contaminó los tests felices y estos recibieron 500; además, el test JWT tenía un import faltante. Se aislaron los `QuarkusTestResource` con `restrictToAnnotatedClass=true` y se añadió el import. La última ejecución pasó los 15 tests; no queda un fallo de tests conocido.

## Pendientes

- Medir y adjuntar el reporte JaCoCo, y confirmar cobertura >=85% del código modificado; el build disponible no produjo artefactos de cobertura.
- Confirmar códigos/cuerpos de Quarkus para JSON malformado y `Content-Type` no aceptado; `docs/ENDPOINTS.md` los deja como preguntas abiertas.
- Confirmar comportamiento de `/q/health` cuando el estado sea DOWN y si la extensión registra checks implícitos. El código del repositorio no tiene checks propios.
- `GET /q/health` no tiene respuesta de error documentada. No se añade una prueba de error con status/cuerpo supuesto; primero debe verificarse y acordarse ese contrato.

## Pruebas propuestas

1. Después de confirmar el comportamiento runtime, añadir pruebas de integración para JSON malformado y `Content-Type` no aceptado con los códigos/cuerpos observados.
2. Para health DOWN, registrar una condición reproducible y documentar primero la respuesta real del framework; luego fijar esa respuesta en un test.
3. Ejecutar `mvn verify` en un entorno con Maven/JaCoCo accesibles y conservar el reporte por clase como evidencia del umbral.

## Caracterizacion de prevent-sql-injection-auth (2026-10-21)

Issue: `user-bcp-student48/workshop-auth-backend#11`. Se mantuvo la rama `lab-complete`, sin cambiar codigo funcional ni contratos HTTP. Esta evidencia actualiza los recuentos para este paso; los resultados y fragmentos del Dia 1 que siguen son historicos, no una copia actual de las clases.

| Prueba reutilizada o completada | Comportamiento protegido |
|---|---|
| `logsInWithWorkshopUser`, `logsInWithTrainerUser` | Credenciales validas: `200`, token no vacio, usuario y rol correspondientes, `expiresIn=3600` y `debug` vigente. Se reforzaron las aserciones de trainer. |
| `logsInWithoutOptionalClient` | Endpoint publico sin Authorization ni client; conserva los mismos campos de exito. Se completaron las aserciones existentes. |
| `rejectsInvalidCredentials`, `treatsQuotedUsernameAsLiteral` | `401` y mensaje contractual; se agrego comprobacion de ausencia de la clave token. |
| `rejectsWrongPasswordForExistingUser` (nueva) | Un username existente no basta: contraseña incorrecta produce `401` sin token. |
| `treatsSqlSyntaxInUsernameAsLiteral` (nueva) | Tres entradas con sintaxis SQL en username y contraseña incorrecta producen `401`, mensaje contractual y ausencia de token. |
| `treatsSqlSyntaxInPasswordAsLiteral` (nueva) | Tres entradas con sintaxis SQL en password y username existente producen `401`, mensaje contractual y ausencia de token. |
| Pruebas existentes de cuerpo nulo y credenciales ausentes, nulas o en blanco | Conservan los siete casos `400` documentados; no se duplicaron. |
| `escapesHtmlInDisplayName` | Escape HTML4 preservado; la prueba existente restaura el displayName modificado. |
| `JwtTokenServiceTest.createsSignedTokenWithExplicitClaimsAndExpiry` | Firma y claims JWT, audiencia y vigencia; reutilizada sin modificar. |

Comandos ejecutados desde `workshop-auth-backend/`:

| Etapa | Comando Maven | Resultado |
|---|---|---|
| Baseline | `mvn -Dtest=AuthResourceTest,JwtTokenServiceTest test` | Surefire: 14 pruebas (13 login + 1 JWT), 0 fallos, errores u omisiones. |
| Tras completar pruebas | `mvn -Dtest=AuthResourceTest,JwtTokenServiceTest test` | 17 pruebas (16 login + 1 JWT), 0 fallos, errores u omisiones; BUILD SUCCESS, salida 0. |
| Tareas 1.1 y 2.1 | `mvn -Dtest=AuthResourceTest test` | 16 pruebas, 0 fallos, errores u omisiones; BUILD SUCCESS, salida 0. |

Para aislar cada ejecucion, se asigno temporalmente un SQLite nuevo bajo `target/` mediante `AUTH_DB_URL`, restaurando despues el valor previo. Ejemplo reproducible en PowerShell, desde la raiz del backend:

```powershell
$previousAuthDbUrl = $env:AUTH_DB_URL
try {
    $env:AUTH_DB_URL = "jdbc:sqlite:./target/characterization-$([guid]::NewGuid().ToString('N')).db"
    mvn '-Dtest=AuthResourceTest' test
} finally {
    $env:AUTH_DB_URL = $previousAuthDbUrl
}
```

Evidencia: `target/surefire-reports/com.bcp.workshop.auth.AuthResourceTest.txt` y `target/surefire-reports/com.bcp.workshop.auth.JwtTokenServiceTest.txt`. Cada ejecucion sobrescribe el reporte de las clases seleccionadas.

Riesgo residual: el runtime registro errores `IllegalAccessError`/`NoClassDefFoundError` de JBoss Threads por acceso a `java.lang` en Java 24+, pese al resultado exitoso de Maven y Surefire. No se silenciaron ni se cambiaron dependencias, flags JVM o gates. Queda pendiente repetir en JDK 21, documentado por el proyecto; no se declara un runtime libre de errores.

Solo se completaron 1.1 y 2.1. No se ejecuto correccion funcional, suite completa ni medicion de cobertura; 3.1 y 4.1 permanecen pendientes. Ninguna prueba contradijo los artefactos OpenSpec.

## Apply de prevent-sql-injection-auth (2026-10-21)

Continuacion de la etapa anterior, exclusivamente para el Issue `user-bcp-student48/workshop-auth-backend#11`, conservando `lab-complete`.

| Paso | Comando desde la raiz del backend | Evidencia |
|---|---|---|
| Workflow OpenSpec apply | `openspec instructions apply --change prevent-sql-injection-auth --json` | Estado inicial ready, 2/4 tareas completas; salida 0. |
| Tarea 3.1 | `mvn -Dtest=AuthResourceTest test` | 16 pruebas, 0 fallos, errores u omisiones; BUILD SUCCESS, salida 0. |
| Tarea 4.1 | `mvn test` | 20 pruebas, 0 fallos, errores u omisiones; BUILD SUCCESS, salida 0. |
| Limite funcional | `git diff --exit-code HEAD -- src/main pom.xml docs/ENDPOINTS.md .github/workflows` | Salida 0; produccion, dependencias, contratos y controles intactos. |
| Entorno | `mvn --version` | Maven 3.10.0, Microsoft Java 25.0.4.1. |

No se reprodujo bypass en las regresiones representativas. La consulta de credenciales ya es estatica, con placeholders para username/password y ambos valores enlazados mediante `PreparedStatement.setString`. Se cumplio la alternativa explicita de 3.1: conservar la logica y registrar evidencia, sin correcciones funcionales ni refactors cosmeticos. No se afirma que seis entradas prueben toda variante posible de inyeccion.

Cada comando de pruebas utilizo una base SQLite nueva bajo `target/` mediante `AUTH_DB_URL`, restaurando la variable al terminar, igual que en el ejemplo de caracterizacion. Los logs completos se conservaron en `target/apply-auth-test.log` y `target/apply-full-test.log`.

| Clase (Surefire, suite completa) | Pruebas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| AuthResourceTest | 16 | 0 | 0 | 0 |
| AuthResourceErrorTest | 1 | 0 | 0 | 0 |
| HealthEndpointTest | 2 | 0 | 0 | 0 |
| JwtTokenServiceTest | 1 | 0 | 0 | 0 |

Las tareas 3.1 y 4.1 quedan completas por esta ejecucion, a diferencia del registro historico anterior. No se cambiaron otros endpoints, archivos funcionales ni los cambios previos de configuracion del usuario.

Riesgo residual confirmado: ambas ejecuciones Maven finalizaron con salida 0, pero los logs registran `IllegalAccessError` y `NoClassDefFoundError` asincronos de JBoss Threads por acceso a `java.lang` en Java 25. Se conserva la evidencia sin silenciarla. Repetir en JDK 21 queda pendiente como gate de entorno, sin modificar JVM, dependencias ni controles dentro de este alcance.

Evaluacion de Definition of Done para este cambio:

- CUMPLIDO - contratos del backend preservados: 20 pruebas y ausencia de diff en produccion y `docs/ENDPOINTS.md`.
- CUMPLIDO - SQL de credenciales parametrizado, inspeccionado y protegido por regresiones HTTP; no se incorporaron dependencias ni secretos reales.
- PENDIENTE DE OTRO GATE - CI remoto y revision del PR; no se abrio ni se aprobo un PR en esta etapa.
- CUMPLIDO - reejecucion local con JDK 21: 20 pruebas pasan y no se reproducen los errores asincronos observados con Java 25; evidencia en la seccion siguiente.
- FALTA EVIDENCIA - porcentaje de cobertura critica >=85%; `mvn test` no acredita ese porcentaje. No se afirma un escaneo SCA actualizado.
- NO APLICA AL ALCANCE - contratos de tareas, resumen y requirements de los otros servicios, que no se leyeron ni modificaron en este paso.

## Revalidacion con JDK 21 (2026-10-21)

Se uso la instalacion existente `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`. Se asignaron `JAVA_HOME` y PATH solo en la sesion PowerShell de ejecucion; no se cambio el JDK global ni la configuracion de terminales Bash ya abiertas.

- `mvn --version`: Maven 3.10.0 y Eclipse Adoptium Java 21.0.12.1.
- `mvn test`, desde la raiz del backend con SQLite ficticio aislado bajo `target/`: 20 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS, salida 0. La variable `AUTH_DB_URL` se restauro al terminar.
- Log: `target/jdk21-full-test.log`; reportes: `target/surefire-reports/`.
- Comprobacion del log con el patron `IllegalAccessError|NoClassDefFoundError|Exception in thread`: 0 coincidencias. Los errores historicos de Java 25 no se reprodujeron, sin agregar `--add-opens` ni desactivar controles.
- El gate local de entorno queda cumplido. CI remoto, cobertura >=85%, SCA actualizado y revision del PR no se acreditan mediante esta ejecucion.

Para seleccionar el mismo JDK en una sesion PowerShell antes de repetir las pruebas aisladas:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn --version
```

## Código de los tests

### `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`

```java
package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(value = JwtTokenServiceTest.SigningKeyResource.class, restrictToAnnotatedClass = true)
class AuthResourceTest {
    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("token", notNullValue())
                .body("token.length()", greaterThan(0))
                .body("expiresIn", is(3600))
                .body("debug", is("authenticated-with-legacy-flow"));
    }

    @Test
    void rejectsInvalidCredentials() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"unknown\",\"password\":\"wrong\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user unknown or password"));
    }

    @Test
    void treatsQuotedUsernameAsLiteral() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"O'Brien\",\"password\":\"wrong\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user O'Brien or password"));
    }

    @Test
    void logsInWithoutOptionalClient() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"));
    }

    @Test
    void rejectsNullRequestBody() {
        given()
                .contentType("application/json")
                .body("null")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Request is null"));
    }

    @Test
    void rejectsMissingUsername() {
        given()
                .contentType("application/json")
                .body("{\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsNullUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":null,\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsBlankUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"  \",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsMissingPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsNullPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":null}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsBlankPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"  \"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }
}
```

### `src/test/java/com/bcp/workshop/auth/AuthResourceErrorTest.java`

```java
package com.bcp.workshop.auth;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(value = AuthResourceErrorTest.MissingSigningKeyResource.class, restrictToAnnotatedClass = true)
class AuthResourceErrorTest {
    @Test
    void returnsDocumentedServerErrorWhenTokenSigningFails() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(500)
                .body("message", notNullValue())
                .body("exception", notNullValue());
    }

    public static class MissingSigningKeyResource implements QuarkusTestResourceLifecycleManager {
        @Override
        public Map<String, String> start() {
            return Map.of("smallrye.jwt.sign.key.location", "missing-auth-test-private-key.pem");
        }

        @Override
        public void stop() {
        }
    }
}
```

### `src/test/java/com/bcp/workshop/auth/HealthEndpointTest.java`

```java
package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;

@QuarkusTest
class HealthEndpointTest {
    @Test
    void reportsHealthyApplication() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void reportsNoCustomHealthChecks() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("checks", empty());
    }
}
```

### `src/test/java/com/bcp/workshop/auth/JwtTokenServiceTest.java`

```java
package com.bcp.workshop.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(value = JwtTokenServiceTest.SigningKeyResource.class, restrictToAnnotatedClass = true)
class JwtTokenServiceTest {
    @Inject
    JwtTokenService tokenService;

    @Test
    void createsSignedTokenWithExplicitClaimsAndExpiry() throws Exception {
        long earliestExpiry = Instant.now().getEpochSecond() + JwtTokenService.TOKEN_TTL_SECONDS;
        String token = tokenService.create("student", "STUDENT");
        String[] tokenParts = token.split("\\.");

        assertEquals(3, tokenParts.length);

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(SigningKeyResource.publicKey());
        verifier.update((tokenParts[0] + "." + tokenParts[1]).getBytes(StandardCharsets.UTF_8));
        assertTrue(verifier.verify(Base64.getUrlDecoder().decode(tokenParts[2])));

        byte[] claimsJson = Base64.getUrlDecoder().decode(tokenParts[1]);
        Map<String, Object> claims = new ObjectMapper().readValue(claimsJson, new TypeReference<>() {});
        long expiresAt = ((Number) claims.get("exp")).longValue();
        assertEquals("student", claims.get("sub"));
        assertEquals("STUDENT", claims.get("role"));
        assertEquals("workshop-auth", claims.get("iss"));
        assertTrue(expiresAt >= earliestExpiry);
        assertTrue(expiresAt <= Instant.now().getEpochSecond() + JwtTokenService.TOKEN_TTL_SECONDS);
    }

    public static class SigningKeyResource implements QuarkusTestResourceLifecycleManager {
        private static PublicKey publicKey;
        private Path privateKeyPath;

        @Override
        public Map<String, String> start() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                publicKey = keyPair.getPublic();
                privateKeyPath = Files.createTempFile("auth-jwt-test-", ".pem");
                String encodedKey = Base64.getMimeEncoder(64, new byte[] {'\n'})
                        .encodeToString(keyPair.getPrivate().getEncoded());
                Files.writeString(privateKeyPath, "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----\n");
                return Map.of("smallrye.jwt.sign.key.location", privateKeyPath.toAbsolutePath().toString());
            } catch (Exception error) {
                throw new IllegalStateException("Could not create isolated JWT test key", error);
            }
        }

        @Override
        public void stop() {
            if (privateKeyPath == null) {
                return;
            }
            try {
                Files.deleteIfExists(privateKeyPath);
            } catch (Exception error) {
                throw new IllegalStateException("Could not remove isolated JWT test key", error);
            }
        }

        static PublicKey publicKey() {
            return publicKey;
        }
    }
}
```

## Documentos consultados

- `docs/ENDPOINTS.md`
- `REGLAS.md`
- `README.md`
- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/java/com/bcp/workshop/auth/AuthResource.java`
- `src/main/java/com/bcp/workshop/auth/LoginRequest.java`
- `src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`
- `src/main/java/com/bcp/workshop/auth/JwtTokenService.java`
- Los cuatro archivos de test incluidos en la sección anterior.
