## Contexto del negocio (breve)
Reducir las quejas causadas por transferencias duplicadas.

## Solicitud de funcionalidad / historia de usuario
Como usuario, quiero confirmar una transferencia antes de enviarla
para evitar duplicados.

## Impacto esperado
Reducir las quejas por transferencias duplicadas en un 20% en un mes.

## Alcance (incluido/excluido)
Incluido: confirmación antes del envío, mensaje de resumen y validación de datos.
Excluido: cambios de infraestructura y un rediseño completo de la interfaz.

## Restricciones técnicas conocidas
API REST existente, frontend web y registro estandarizado.

## Riesgos conocidos
Exposición de datos sensibles en los registros; errores en la validación del monto.

## Dependencias
Servicio de cuentas y servicio de transferencias.

## Definición de terminado del equipo (DoD)
Pruebas unitarias, documentación breve y solicitud de cambios revisada.