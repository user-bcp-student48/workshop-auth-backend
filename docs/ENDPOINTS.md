# API del backend de autenticación

Base URL local: `http://localhost:8080` (README.md). Las respuestas de la API usan JSON.

## POST /auth/login

### 1. Qué hace

Valida las credenciales del usuario del taller y, si son correctas, devuelve sus datos públicos junto con un JWT firmado para que el cliente lo use con los servicios que lo acepten.

### 2. Autenticación

No requiere autenticación previa. No se exige header `Authorization`; este endpoint es el que emite el token. Envía `Content-Type: application/json` y, opcionalmente, `Accept: application/json`.

### 3. Request

| campo | tipo | obligatorio | descripción |
|---|---|---|---|
| `username` | string | Sí | Usuario. No puede ser nulo ni estar vacío o compuesto solo por espacios. |
| `password` | string | Sí | Contraseña. No puede ser nula ni estar vacía o compuesta solo por espacios. |
| `client` | string | No | Campo aceptado en el DTO; el endpoint no lo consulta ni lo devuelve. |

Ejemplo completo:

```json
{
	"username": "student",
	"password": "Workshop123!",
	"client": "angular-workshop"
}
```

### 4. Response exitosa

Estado: **200 OK**.

| campo | tipo | descripción |
|---|---|---|
| `token` | string | JWT firmado generado para el usuario. El valor cambia en cada generación. |
| `expiresIn` | integer | Vigencia del token en segundos: `3600`. |
| `user` | object | Datos públicos del usuario autenticado. |
| `user.username` | string | Nombre de usuario. |
| `user.displayName` | string | Nombre visible con escape HTML4 aplicado. |
| `user.role` | string | Rol asociado al usuario, por ejemplo `STUDENT` o `TRAINER`. |
| `debug` | string | Valor fijo actual: `authenticated-with-legacy-flow`. |

Ejemplo completo (el token es un valor dinámico; no reutilizar este marcador como credencial):

```json
{
	"token": "<JWT firmado generado para esta respuesta>",
	"expiresIn": 3600,
	"user": {
		"username": "student",
		"displayName": "Workshop Student",
		"role": "STUDENT"
	},
	"debug": "authenticated-with-legacy-flow"
}
```

### 5. Respuestas de error

**400 Bad Request — cuerpo JSON nulo.** Ocurre cuando el cuerpo enviado es el literal JSON `null`.

```json
{"message":"Request is null"}
```

**400 Bad Request — username ausente, nulo o en blanco.**

```json
{"message":"Username is required"}
```

**400 Bad Request — password ausente, nulo o en blanco.**

```json
{"message":"Password is required"}
```

**401 Unauthorized — credenciales que no coinciden.** El username recibido se incorpora al mensaje.

```json
{"message":"Invalid user unknown or password"}
```

**500 Internal Server Error — excepción durante el login, por ejemplo al generar el token.** El cuerpo refleja el mensaje de excepción (o `Unknown error` si es nulo) y el nombre de su clase; los valores concretos dependen del error ocurrido.

```json
{
	"message": "<mensaje de la excepción o Unknown error>",
	"exception": "<nombre completo de la clase Java>"
}
```

La conversión de JSON malformado y el rechazo de un `Content-Type` no aceptado los gestiona Quarkus/JAX-RS, no el método del resource. **Pregunta abierta:** confirmar en una ejecución HTTP real los códigos y cuerpos exactos que la versión desplegada produce para esos errores de framework.

### 6. Notas de seguridad

- El JWT se firma; sus claims incluyen `sub` (username), `role`, `iss` y `exp`. El TTL usado al generar `exp` es el mismo que se devuelve como `expiresIn`.
- La clave privada se configura mediante `AUTH_JWT_PRIVATE_KEY_LOCATION`; no hay una clave privada real en el repositorio. `AUTH_JWT_ISSUER` configura `iss` y tiene valor predeterminado `workshop-auth`.
- Este endpoint no valida un token de entrada ni requiere `Authorization`; el backend no declara protección JWT para `/auth/login`.
- Las credenciales de las cuentas de laboratorio se comparan con la base local. No enviar credenciales reales a este servicio de taller.

## GET /q/health

### 1. Qué hace

Permite a un monitor comprobar si la instancia del backend reporta estado saludable.

### 2. Autenticación

No requiere autenticación ni header `Authorization`. La ruta la proporciona la extensión Quarkus SmallRye Health.

### 3. Request

| campo | tipo | obligatorio | descripción |
|---|---|---|---|
| — | — | No | No recibe parámetros ni cuerpo JSON. |

Ejemplo de solicitud (GET sin cuerpo):

```http
GET /q/health HTTP/1.1
Host: localhost:8080
Accept: application/json
```

Ejemplo de JSON de request: no aplica; GET no envía cuerpo en este endpoint.

### 4. Response exitosa

Estado: **200 OK** cuando SmallRye Health informa `UP`.

| campo | tipo | descripción |
|---|---|---|
| `status` | string | Estado agregado de salud; `UP` en la respuesta saludable. |
| `checks` | array | Checks de salud incluidos por la extensión. El repositorio no declara checks personalizados. |

Ejemplo estándar de SmallRye Health cuando no hay checks personalizados:

```json
{
	"status": "UP",
	"checks": []
}
```

### 5. Respuestas de error

El repositorio no define respuestas de error propias para esta ruta: el endpoint lo implementa SmallRye Health y no hay checks personalizados declarados.

**Pregunta abierta:** confirmar con una ejecución del servicio si esta versión expone otro código/cuerpo cuando el estado agregado sea `DOWN`, y si alguna extensión añade checks automáticamente. No hay un ejemplo de error local verificable para documentar sin asumirlo.

### 6. Notas de seguridad

- Esta ruta es pública y no exige bearer token.
- El endpoint es agregado por SmallRye Health (`quarkus-smallrye-health` en `pom.xml`); no se encontró una implementación de `HealthCheck` en el código de la aplicación.

## Fuentes consultadas

- `README.md`: URL local y endpoints publicados.
- `src/main/java/com/bcp/workshop/auth/AuthResource.java`: ruta, validación, estados y cuerpos de login.
- `src/main/java/com/bcp/workshop/auth/LoginRequest.java`: campos y tipos del request.
- `src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`: lookup de credenciales, SQLite y comportamiento cuando no hay resultado.
- `src/main/java/com/bcp/workshop/auth/JwtTokenService.java`: firma, claims y expiración.
- `src/main/resources/application.properties`: propiedades de clave y emisor.
- `pom.xml`: extensiones REST/Jackson, JWT Build y SmallRye Health.
- `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` y `src/test/java/com/bcp/workshop/auth/JwtTokenServiceTest.java`: ejemplos y assertions de contrato/tokens.