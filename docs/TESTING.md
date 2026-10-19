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
