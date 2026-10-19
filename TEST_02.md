# TEST_02 — Firma y claims del JWT

- **Archivo Java ejecutado:** `src/test/java/com/bcp/workshop/auth/JwtTokenServiceTest.java`
- **Tipo:** prueba del servicio generador de tokens.
- **Hallazgo relacionado:** H-04 (reemplazo de Base64 por token firmado).

## Comprobaciones

- El token contiene tres segmentos JWT.
- La firma RSA se verifica con la clave pública correspondiente a una clave efímera creada para el test.
- Los claims incluyen `sub`, `role`, `iss` y `exp`.
- `exp` cae dentro del TTL de 3600 segundos.
- La clave privada temporal se elimina al detener el recurso de test; no se usa un secreto real.

## Resultado registrado

Surefire (`target/surefire-reports/com.bcp.workshop.auth.JwtTokenServiceTest.txt`):

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

**Estado: PASS.**