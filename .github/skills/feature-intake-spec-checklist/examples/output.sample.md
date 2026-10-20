# Recopilación de funcionalidades -> Lista de verificación de especificación

## 1. Resumen
Agregar una confirmación de transferencia antes del envío, con un resumen conciso
y validación de los datos de la transferencia. Objetivo: reducir en un 20% las
quejas por transferencias duplicadas en un mes.

## 6. Criterios de aceptación (medibles)
- En al menos el 95% de los intentos válidos se muestra una confirmación antes del envío.
- Los datos no válidos bloquean el envío y muestran un mensaje de validación.
- Las quejas por duplicados disminuyen un 20% en el plazo de un mes.
- No aparecen datos sensibles en los registros de eventos de confirmación.

## 9. Preguntas abiertas
- ¿Qué línea base se usará para medir la reducción del 20%?
- ¿Qué versión de la API define el contrato de confirmación?