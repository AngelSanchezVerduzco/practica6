# Práctica 6 — Respuestas de análisis (MobileConnect)

## 1. ¿Qué significa la interacción entre dispositivos móviles?

Es el intercambio de información entre dos o más teléfonos de forma que una acción en uno se refleja en el otro. En esta práctica: un mensaje enviado desde un celular aparece en el del compañero a través de un canal WebSocket compartido (misma sala en el servidor).

## 2. ¿Qué tecnologías permiten intercambiar información entre dispositivos?

Bluetooth / BLE, Wi‑Fi Direct, Wi‑Fi/LAN, internet (HTTP/WebSocket), NFC y APIs como Nearby Connections. En MobileConnect usamos **WebSocket sobre Wi‑Fi/LAN** (`ws://IP:8080/ws`) hacia el servidor `canal_servidor` de la pareja.

## 3. ¿Qué diferencia existe entre una simulación local y una comunicación real?

En la **simulación** ambos “dispositivos” viven en la misma app/memoria (modo local A/B). En la **comunicación real** hay dos procesos en dos teléfonos; los datos cruzan la red (IP del servidor, sala `practica6`), con latencia, posibles desconexiones (`Socket closed`) y roles asignados por el servidor.

## 4. ¿Qué función cumple el estado de la aplicación en Jetpack Compose?

Guarda lo que la UI muestra (mensajes, conexión, rol A/B, contadores, texto del campo). Al cambiar el estado, Compose **recompone** y actualiza la pantalla. Sin estado reactivo, el chat no se refrescaría al recibir un `mensaje` o una `asignacion` por WebSocket.

## 5. ¿Por qué es importante validar los mensajes antes de enviarlos?

Evita basura en el historial, reduce tráfico inútil por la red y mejora la experiencia (no enviar cadenas vacías o solo espacios). El cliente hace `trim()`; el servidor también rechaza texto vacío con un `error`.

## 6. ¿Qué cambios serían necesarios para implementar comunicación real mediante Bluetooth o Wi‑Fi?

Permisos (Bluetooth/ubicación/Wi‑Fi), descubrimiento o emparejamiento, sockets o una API (Nearby, `BluetoothSocket`, Wi‑Fi Direct), corrutinas, serializar mensajes y manejar caídas de enlace. En esta práctica la comunicación real se resolvió con **Wi‑Fi + WebSocket** y un servidor intermediario, sin emparejar Bluetooth punto a punto.

## 7. ¿Qué ventajas tendría utilizar un servidor intermediario para intercambiar mensajes?

Los teléfonos no necesitan emparejarse entre sí: solo se conectan al mismo canal. El servidor guarda historial, asigna roles A/B, hace broadcast y puede escalar a más clientes. Facilita depuración (`/salud`) y reinicio con `limpiar`. En internet permitiría chat a distancia, autenticación y notificaciones.
