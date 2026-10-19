# TEST_01 — Login HTTP

- **Archivo Java ejecutado:** `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`
- **Tipo:** integración HTTP con Quarkus Test y REST Assured.
- **Hallazgo relacionado:** H-02 (SQL parametrizado); también caracteriza el contrato de login usado por H-04.

## Casos

1. Login válido de `student`: espera HTTP 200, token no vacío, `expiresIn` 3600 y los campos `username`, `displayName`, `role` y `debug`.
2. Credenciales desconocidas: espera HTTP 401 y el mensaje contractual.
3. Username literal `O'Brien`: espera HTTP 401 y confirma que la comilla se procesa como dato, no como parte de la consulta SQL.

## Resultado registrado

Surefire (`target/surefire-reports/com.bcp.workshop.auth.AuthResourceTest.txt`):

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

**Estado: PASS.**