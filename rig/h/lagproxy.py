import asyncio, os, time
STALL = float(os.environ.get("STALL_MS", "800")) / 1000
EVERY = float(os.environ.get("EVERY_S", "4"))
T0 = time.time()
def stalled():
    return (time.time() - T0) % EVERY < STALL and (time.time() - T0) > 20
async def pipe(r, w):
    try:
        while True:
            d = await r.read(65536)
            if not d: break
            while stalled(): await asyncio.sleep(0.01)
            w.write(d); await w.drain()
    except Exception: pass
    finally:
        try: w.close()
        except Exception: pass
async def handle(cr, cw):
    sr, sw = await asyncio.open_connection("meshac-rig", 25565)
    await asyncio.gather(pipe(cr, sw), pipe(sr, cw))
async def main():
    s = await asyncio.start_server(handle, "0.0.0.0", 25565)
    async with s: await s.serve_forever()
asyncio.run(main())
