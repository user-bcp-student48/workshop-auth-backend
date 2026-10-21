# Spec Delta

## Purpose

Define el comportamiento observable del inicio de sesión para que solo credenciales válidas autentiquen al usuario, preservando el contrato HTTP existente del backend.

## ADDED Requirements

### Requirement: Credenciales no pueden alterar el resultado de autenticación
El sistema SHALL autenticar una solicitud únicamente cuando el username y el password proporcionados correspondan a una cuenta existente. El contenido con sintaxis SQL en esos campos MUST NOT alterar la evaluación de las credenciales ni permitir autenticación por sí solo.

#### Scenario: Credenciales válidas
- **WHEN** se envían el username y el password correspondientes a una cuenta existente
- **THEN** la solicitud se autentica para esa cuenta

#### Scenario: Entrada con sintaxis SQL sin credenciales válidas
- **WHEN** el username o el password contiene texto con sintaxis SQL pero el par enviado no corresponde a una cuenta existente
- **THEN** la solicitud se rechaza como credenciales inválidas y no se emite un token

### Requirement: Login válido conserva su respuesta documentada
El sistema SHALL conservar para un login válido el estado y los campos de respuesta documentados para `POST /auth/login`.

#### Scenario: Respuesta a credenciales válidas
- **WHEN** un usuario envía credenciales válidas a `POST /auth/login`
- **THEN** el sistema responde `200` con `token`, `expiresIn` igual a `3600`, `user.username`, `user.displayName` con escape HTML4, `user.role` y el valor actual de `debug`

### Requirement: Credenciales inválidas conservan su respuesta documentada
El sistema SHALL responder a credenciales no coincidentes con el estado `401` y el mensaje documentado, sin autenticar al usuario ni emitir un token.

#### Scenario: Credenciales incorrectas
- **WHEN** el username y el password no corresponden a una cuenta existente
- **THEN** el sistema responde `401` con el mensaje `Invalid user <username> or password` y no incluye un token de autenticación

### Requirement: Validación de la solicitud de login conserva el contrato documentado
El endpoint SHALL permanecer público, exigir username y password no nulos ni en blanco, y aceptar el campo opcional `client` sin que este altere la autenticación.

#### Scenario: Solicitud sin campo client
- **WHEN** se envían credenciales válidas sin header `Authorization` y sin el campo opcional `client`
- **THEN** el login se procesa conforme al mismo contrato que una solicitud que sí lo incluye

#### Scenario: Cuerpo JSON nulo
- **WHEN** el cuerpo de la solicitud es el literal JSON `null`
- **THEN** el endpoint responde `400` con `{"message":"Request is null"}`

#### Scenario: Username ausente, nulo o en blanco
- **WHEN** username está ausente, es nulo o contiene solo espacios
- **THEN** el endpoint responde `400` con `{"message":"Username is required"}`

#### Scenario: Password ausente, nulo o en blanco
- **WHEN** password está ausente, es nulo o contiene solo espacios
- **THEN** el endpoint responde `400` con `{"message":"Password is required"}`
