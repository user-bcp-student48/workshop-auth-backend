package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.empty;
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
    void logsInWithTrainerUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"trainer\",\"password\":\"Trainer123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("trainer"))
                .body("user.displayName", is("Workshop Trainer"))
                .body("user.role", is("TRAINER"));
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

    @Test
    void escapesHtmlInDisplayName() throws Exception {
        String originalDisplayName = findDisplayName("student");
        try {
            updateDisplayName("student", "<Workshop & Student>");
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(200)
                    .body("user.displayName", is("&lt;Workshop &amp; Student&gt;"));
        } finally {
            updateDisplayName("student", originalDisplayName);
        }
    }

    private static String findDisplayName(String username) throws Exception {
        String databaseUrl = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement("SELECT display_name FROM legacy_users WHERE username=?")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getString("display_name");
            }
        }
    }

    private static void updateDisplayName(String username, String displayName) throws Exception {
        String databaseUrl = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement("UPDATE legacy_users SET display_name=? WHERE username=?")) {
            statement.setString(1, displayName);
            statement.setString(2, username);
            statement.executeUpdate();
        }
    }
}
