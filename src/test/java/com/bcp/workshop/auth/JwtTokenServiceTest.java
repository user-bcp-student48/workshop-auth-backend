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