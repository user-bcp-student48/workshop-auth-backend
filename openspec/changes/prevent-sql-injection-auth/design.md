# Design

## Context

Ver `proposal.md` para la motivación y `specs/auth-login/spec.md` para los requisitos. `POST /auth/login` valida la solicitud en `AuthResource`, consulta las cuentas locales a través de `LegacyDatabase.findByCredentials` y solo genera un JWT cuando recibe un usuario. `LegacyDatabase` usa JDBC `PreparedStatement` y enlaza username y password como parámetros. `AuthResourceTest` verifica el contrato HTTP existente y trata un username con comilla como literal. La base, ruta, request, JWT y respuestas descritas en `docs/ENDPOINTS.md` son restricciones.

## Goals / Non-Goals

**Goals:**

- Demostrar mediante pruebas HTTP que entradas con sintaxis SQL se tratan como datos y no autentican si no son credenciales válidas.
- Mantener el lookup acotado y el contrato vigente.

**Non-Goals:**

- Cambiar el esquema o contenido de la base, las credenciales de laboratorio, rutas, DTO, formato de errores, manejo de tokens, dependencias ni endpoints distintos a `/auth/login`.

## Protected Contracts

Se preservan las respuestas `200`, `401` y `400` y sus cuerpos documentados, los campos del JWT y de usuario, el escape HTML4 de `displayName`, el `expiresIn` de 3600, `debug`, `client` opcional y la ausencia de autenticación previa. Ver `docs/ENDPOINTS.md` y la spec delta.

## Components Affected

- `LegacyDatabase.findByCredentials`: límite de persistencia que compara ambas credenciales; no construir SQL concatenando valores de entrada.
- `AuthResourceTest`: pruebas de caracterización y regresión a través de `POST /auth/login`.
- `AuthResource`: se mantiene sin cambios salvo que las pruebas de caracterización demuestren una desviación necesaria para cumplir la spec; no se prevé cambiar el contrato HTTP.

## Decisions

1. **Mantener una consulta JDBC parametrizada y estática para comparar ambas credenciales.** Los parámetros se enlazan como valores; no se incorporan al texto SQL. La inspección actual ya muestra `PreparedStatement` y bindings. La corrección, si una prueba demuestra bypass, se limitará al lookup.
   - Alternativa descartada: concatenar strings con escape manual, por ser frágil.
   - Alternativa descartada: filtrar o denegar caracteres SQL, porque puede bloquear valores legítimos y no es una garantía de seguridad.
   - Alternativa descartada: introducir un ORM, dependencias o rediseñar la persistencia por ser desproporcionado al alcance.
2. **Verificar por el endpoint HTTP y resultados observables.** Probar entradas manipuladas en username y password contra pares inválidos; esperar `401` y ausencia de JWT, preservando los casos de login correcto e inválido. Las pruebas no dependerán de mensajes del motor SQL.
3. **No cambiar el manejo de excepciones de base de datos en esta HU.** Ajustarlo podría modificar el contrato y requiere una decisión separada.

## Incremental Strategy

1. Añadir o ejecutar primero caracterizaciones del contrato vigente: login correcto para student y trainer, campos de respuesta y JWT, `401` para credenciales incorrectas y username con comilla, validaciones `400` y login sin `client`.
2. Añadir regresiones HTTP para texto con sintaxis SQL en username y password, verificando `401` y que la respuesta no emita token.
3. Si la caracterización detecta bypass, corregir únicamente el acceso de credenciales en `LegacyDatabase`; si no lo detecta, conservar el comportamiento y documentar la evidencia sin reescrituras cosméticas.
4. Ejecutar `AuthResourceTest` y después la suite Maven del repositorio; revisar el diff para confirmar que no cambió el contrato.

## Characterization Tests

Antes de modificar cualquier comportamiento crítico, fijar las respuestas existentes con pruebas de integración HTTP:

- `200` y campos exactos para cuentas válidas.
- Verificación de JWT y TTL conforme a las pruebas existentes.
- `401` y mensaje para credenciales inválidas.
- Username con comilla tratado literalmente.
- `400` para cuerpo nulo y credenciales requeridas nulas, ausentes o en blanco.
- Aceptación sin `client`.

Mantener las pruebas aisladas del estado de datos que modifican y restaurar los datos de laboratorio cuando corresponda.

## Risks / Trade-offs

- [Las pruebas con entradas SQL representativas no prueban por sí solas toda clase de inyección] → revisar que la consulta permanezca estática y que ambos valores sigan ligados como parámetros; probar ambos campos a través del endpoint.
- [La implementación actual ya parece parametrizada, por lo que puede no existir un cambio funcional que hacer] → establecer primero la caracterización y modificar solo ante una falla reproducible; evitar cambios cosméticos.
- [Los fallos JDBC pueden confundirse con credenciales inválidas por el comportamiento actual] → no ampliar esta HU ni cambiar el contrato; registrar la limitación para una decisión independiente.

## Migration Plan

No requiere migración de datos ni despliegue escalonado: el esquema de base y la API no cambian. Validar primero en pruebas.

**Rollback:** revertir el cambio acotado del lookup y sus pruebas si la regresión afecta cuentas válidas o el contrato; no requiere revertir datos ni configuración.

## Open Questions

Ninguna que impida implementar el alcance aprobado.
