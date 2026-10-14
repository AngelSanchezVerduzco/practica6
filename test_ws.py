import asyncio
import sys

try:
    import websockets
except ImportError:
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "websockets", "-q"])
    import websockets


async def main():
    uri = "ws://10.13.8.252:8080/ws"
    try:
        async with websockets.connect(uri, open_timeout=5) as ws:
            print("OPEN_OK")
            await ws.send('{"from":"PC","text":"ping-test"}')
            print("SENT_JSON")
            try:
                msg = await asyncio.wait_for(ws.recv(), timeout=3)
                print("RECV:", repr(msg)[:300])
            except asyncio.TimeoutError:
                print("NO_ECHO_TIMEOUT")
    except Exception as e:
        print("FAIL:", type(e).__name__, e)


if __name__ == "__main__":
    asyncio.run(main())
