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
