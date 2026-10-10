# Mediyiyo 0.4.0 — verificación

## Automatizada en GitHub Actions

- `testDebugUnitTest`: identifica la misma toma desde el ID de alarma y la pantalla; separa fechas y tomas; ordena varios horarios sin combinar sus identidades.
- `assembleRelease` y `apksigner verify`: compilan y verifican la APK con los Secrets existentes.

## Pendiente de prueba en un teléfono Android

Instalar la APK sobre 0.3.6 sin borrar datos, comprobar que los medicamentos, sus horarios y el historial siguen allí. Agregar y editar un medicamento personalizado, seleccionar una sugerencia, configurar tres horarios distintos y verificar el orden cronológico. Tocar una tarjeta pendiente, cancelar y confirmar; sólo la toma elegida debe pasar de gris a verde con la hora real. Cerrar y reabrir la app y cambiar de fecha; los estados deben mantenerse independientes. Editar la hora registrada y luego deshacer con confirmación.

Con permiso de alarmas exactas y notificaciones, seleccionar sonido, probar vibración y aviso en pantalla bloqueada. Pulsar “¡YA SÉ, WN!” para una sola toma: debe cerrarse esa notificación, quedar verde la tarjeta correspondiente y conservarse la alarma del día siguiente. Reiniciar el teléfono y verificar la recuperación de la alarma. Probar edición de horas, orden y eliminación, conservando el historial de medicamentos quitados. Verificar que la lista llega hasta la barra inferior del sistema y que no aparece un menú inferior.

Estas comprobaciones requieren interacción y permisos reales del dispositivo; la compilación no las sustituye.
