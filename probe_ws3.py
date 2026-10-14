import asyncio
import json
import sys

try:
    import websockets
except ImportError:
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "websockets", "-q"])
    import websockets

URI = "ws://10.13.8.252:8080/ws"

ROOMS = ["1", "sala1", "chat", "general", "default", "A", "B", "pareja", "mobileconnect", "test", "room1"]
KEYS = ["sala", "room", "idSala", "salaId", "canal", "channel"]


async def try_payload(payload):
    async with websockets.connect(URI, open_timeout=3) as ws:
        await ws.send(json.dumps(payload, ensure_ascii=False))
        try:
            msg = await asyncio.wait_for(ws.recv(), timeout=1.5)
        except asyncio.TimeoutError:
            msg = "(sin respuesta)"
        print(f"{payload} -> {msg[:220]}")
        if "error" not in str(msg).lower():
            await ws.send(
                json.dumps(
                    {"tipo": "enviar", "usuario": payload.get("nombre", "PC"), "mensaje": "hola-chat"},
                    ensure_ascii=False,
                )
            )
            try:
                msg2 = await asyncio.wait_for(ws.recv(), timeout=1.5)
                print("  SEND->", msg2[:220])
            except asyncio.TimeoutError:
                print("  SEND-> (sin respuesta)")


async def main():
    for key in KEYS:
        for room in ROOMS:
            payload = {"tipo": "unirse", "nombre": "PC_Test", key: room}
            try:
                await try_payload(payload)
            except Exception as e:
                print(payload, "FAIL", e)
            await asyncio.sleep(0.05)


if __name__ == "__main__":
    asyncio.run(main())
