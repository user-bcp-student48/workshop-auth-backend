# Proposal

## Why

El Issue [#11: HU-01: Blindar autenticación contra inyección SQL](https://github.com/user-bcp-student48/workshop-auth-backend/issues/11) plantea proteger el inicio de sesión frente a entradas que intenten alterar la consulta de credenciales. Un bypass podría emitir un JWT a quien no demuestre conocer credenciales válidas y permitir acceso no autorizado.

## What Changes

- Definir que el inicio de sesión solo autentica cuando el par username/password corresponde a una cuenta existente; texto con sintaxis SQL no puede cambiar ese resultado.
- Especificar los resultados observables de intentos de autenticación no válidos y preservar el contrato publicado de `/auth/login`.

## Alcance

- `POST /auth/login`: evitar que entradas manipuladas en username o password permitan obtener una autenticación o un JWT sin credenciales válidas.
- Mantener los resultados documentados para credenciales válidas, inválidas y datos requeridos ausentes o en blanco.

## Fuera de alcance

- Cambiar rutas, campos del request, formato de respuesta, códigos HTTP o el flujo de emisión y contenido del JWT.
- Modificar `/q/health`, el modelo de cuentas, las políticas de contraseñas, el almacenamiento de credenciales, el control de intentos o la autorización de otros servicios.
- Definir la solución técnica; este cambio establece comportamiento y criterios observables, no el diseño de implementación.

## Comportamiento que debe preservarse

Según el baseline de [docs/ENDPOINTS.md](../../../docs/ENDPOINTS.md), `/auth/login` sigue siendo público y acepta `username` y `password` requeridos, además de `client` opcional que no afecta el resultado. Un login válido continúa respondiendo `200` con `token`, `expiresIn` igual a `3600`, `user` con `username`, `displayName` escapado y `role`, y el campo `debug` actual. Un login inválido mantiene `401` y el mensaje `Invalid user <username> or password`. Se mantienen las respuestas `400` documentadas para cuerpo nulo y username/password nulos, ausentes o en blanco. El endpoint `/q/health` no cambia.

## Capabilities

### New Capabilities

- `auth-login`: comportamiento observable del inicio de sesión y de la validación de credenciales.

### Modified Capabilities

- Ninguna. El repositorio no tiene specs existentes; la capacidad se incorpora como una spec nueva.

## Impact

- Contrato: `POST /auth/login`; `/q/health` queda sin cambios.
- Área funcional relacionada: autenticación de credenciales.
- No se propone modificar dependencias ni otros endpoints.
