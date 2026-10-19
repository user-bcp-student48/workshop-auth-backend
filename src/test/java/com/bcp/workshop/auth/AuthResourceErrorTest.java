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
