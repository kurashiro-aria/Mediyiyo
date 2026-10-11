# Mediyiyo 0.4.1 — identidad y ficha

## Comprobaciones en el repositorio y CI

- La cabecera usa `mediyiyo_brand_header.jpg`, el recurso original conservado desde 0.3.6. El icono externo conserva `mediyiyo_launcher_square.jpg`.
- Se restauran el fondo azul oscuro, la barra de sistema oscura y el color turquesa de los controles. Las tarjetas siguen grises o verdes según la toma. No hay casillas ni menú inferior.
- Las zonas de información (icono, nombre, descripción y flecha) abren la ficha; el botón independiente `Confirmar` o `Ver toma` abre el flujo de registro. Abrir información no llama a `DoseRecords.confirm`.
- `testDebugUnitTest` verifica que el catálogo recupera una descripción existente, respeta el texto personalizado y deja en blanco lo desconocido. También comprueba claves independientes de fecha y toma y orden cronológico.
- `assembleRelease`, `apksigner verify` y la comprobación de paquete, versión y certificado original se ejecutan antes de publicar la APK.

## Pruebas pendientes en teléfono

Instalar sobre 0.4.0 sin desinstalar. Confirmar que medicamentos, registros y horarios permanecen. Tocar el nombre: aparece dosis, descripción, uso e intervalos programados, sin poner verde la tarjeta. Tocar `Confirmar`: cancelar y luego aceptar; sólo esa toma cambia a verde y guarda hora real. Cerrar y reabrir; navegar a otro día y volver. Tocar `Ver toma`, editar el registro y deshacerlo con confirmación. Probar un medicamento personalizado sin información, añadirla y verificar que se ve en su ficha.

Probar en el dispositivo las alarmas exactas, sonido, vibración, pantalla bloqueada, recuperación tras reiniciar y `¡YA SÉ, WN!` para una sola toma. Comprobar el historial, la edición y eliminación, la ausencia de menú inferior y la firma de actualización. La CI no sustituye estas interacciones físicas.
