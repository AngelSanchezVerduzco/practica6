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

CANDIDATES = [
    {"tipo": "mensaje", "from": "PC", "text": "hola"},
    {"tipo": "mensaje", "nombre": "PC", "texto": "hola"},
    {"tipo": "chat", "from": "PC", "text": "hola"},
    {"tipo": "message", "from": "PC", "text": "hola"},
    {"tipo": "enviar", "usuario": "PC", "mensaje": "hola"},
    {"tipo": "join", "nombre": "PC"},
    {"tipo": "conectar", "nombre": "PC"},
    {"tipo": "registro", "nombre": "PC"},
    {"tipo": "login", "nombre": "PC"},
    {"tipo": "hola", "nombre": "PC"},
]


async def try_one(payload: dict):
    try:
        async with websockets.connect(URI, open_timeout=4) as ws:
            await ws.send(json.dumps(payload, ensure_ascii=False))
            try:
                msg = await asyncio.wait_for(ws.recv(), timeout=2)
                print("PAYLOAD", payload)
                print("  ->", msg[:300])
            except asyncio.TimeoutError:
                print("PAYLOAD", payload)
                print("  -> (sin respuesta)")
    except Exception as e:
        print("PAYLOAD", payload)
        print("  FAIL", e)


async def main():
    for p in CANDIDATES:
        await try_one(p)
        await asyncio.sleep(0.2)


if __name__ == "__main__":
    asyncio.run(main())
