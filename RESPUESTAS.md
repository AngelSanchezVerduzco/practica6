# Práctica 6 — Respuestas de análisis (MobileConnect)

## 1. ¿Qué significa la interacción entre dispositivos móviles?

Es el intercambio de información entre dos o más teléfonos de forma que una acción en uno se refleja en el otro. En esta práctica: un mensaje enviado desde un celular aparece en el del compañero por un canal inalámbrico.

## 2. ¿Qué tecnologías permiten intercambiar información entre dispositivos?

Bluetooth / BLE, Wi‑Fi Direct, Wi‑Fi/LAN, internet (HTTP/WebSocket), NFC, y APIs como **Nearby Connections** (usa Bluetooth y Wi‑Fi bajo el capó). Cada una tiene distinto alcance y requisitos de permisos.

## 3. ¿Qué diferencia existe entre una simulación local y una comunicación real?

En la **simulación** ambos “dispositivos” viven en la misma app/memoria. En la **comunicación real** hay dos procesos en dos teléfonos y los datos cruzan el aire (Nearby/Bluetooth/Wi‑Fi), con permisos, latencia y posibles desconexiones.

## 4. ¿Qué función cumple el estado de la aplicación en Jetpack Compose?

Guarda lo que la UI muestra (mensajes, conexión, contadores, texto). Al cambiar el estado, Compose **recompone** y actualiza la pantalla. Sin estado reactivo, el chat no se refrescaría al recibir un mensaje.

## 5. ¿Por qué es importante validar los mensajes antes de enviarlos?

Evita basura en el historial, reduce tráfico inútil por la red y mejora la experiencia (no enviar cadenas vacías o solo espacios).

## 6. ¿Qué cambios serían necesarios para implementar comunicación real mediante Bluetooth o Wi‑Fi?

Permisos, descubrimiento/emparejamiento, sockets o una API (Nearby, BluetoothSocket, Wi‑Fi Direct), hilos/coroutines, serializar mensajes y manejar caídas de enlace. En MobileConnect eso se resolvió con Nearby Connections.

## 7. ¿Qué ventajas tendría utilizar un servidor intermediario para intercambiar mensajes?

Los teléfonos no necesitan estar cerca ni en la misma red; funciona por internet; historial centralizado; autenticación; escala a muchos usuarios; y facilita notificaciones push.
