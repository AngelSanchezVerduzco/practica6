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

JOIN_TYPES = [
    "unirse",
    "unir",
    "join_room",
    "entrar",
    "iniciar",
    "start",
    "hello",
    "ready",
    "register",
    "auth",
    "nombre",
    "set_nombre",
    "identificarse",
    "identify",
    "handshake",
]


async def try_join(tipo: str):
    payloads = [
        {"tipo": tipo, "nombre": "PC_Test"},
        {"tipo": tipo, "usuario": "PC_Test"},
        {"tipo": tipo, "name": "PC_Test"},
        {"tipo": tipo, "from": "PC_Test"},
    ]
    for payload in payloads:
        try:
            async with websockets.connect(URI, open_timeout=3) as ws:
                await ws.send(json.dumps(payload, ensure_ascii=False))
                try:
                    msg = await asyncio.wait_for(ws.recv(), timeout=1.5)
                except asyncio.TimeoutError:
                    msg = "(sin respuesta)"
                print(f"{payload} -> {msg[:250]}")
                if "desconocido" not in str(msg) and "error" not in str(msg).lower():
                    # try send message after
                    await ws.send(json.dumps({"tipo": "enviar", "usuario": "PC_Test", "mensaje": "hola2"}, ensure_ascii=False))
                    try:
                        msg2 = await asyncio.wait_for(ws.recv(), timeout=1.5)
                        print("  AFTER SEND:", msg2[:250])
                    except asyncio.TimeoutError:
                        print("  AFTER SEND: (sin respuesta)")
        except Exception as e:
            print(payload, "FAIL", e)


async def main():
    for t in JOIN_TYPES:
        await try_join(t)


if __name__ == "__main__":
    asyncio.run(main())
