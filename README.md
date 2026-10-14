# MobileConnect — contrato WebSocket v2

URL: `ws://10.13.8.252:8080/ws`  
Sala: `practica6`

## Roles (el servidor asigna)

1. Ambos hacen `unirse` (solo `sala`).
2. Quien envía el **primer** mensaje válido → **A**.
3. El otro (al recibir) → **B**.
4. `limpiar` reinicia mensajes y roles.

## Cliente → servidor

- `{"tipo":"unirse","sala":"practica6"}`
- `{"tipo":"enviar","texto":"..."}`
- `{"tipo":"limpiar"}`

## Servidor → cliente

- `historial` / `mensaje` / `asignacion` / `error`
