#!/usr/bin/env python3
"""User-space TCP hold/release stall fixture v2 for the packet-bunched A15 control.

A plain TCP proxy on the box, in front of the rig server only. The rig client
connects to the proxy; the proxy forwards to meshac-red. On a HOLD command it
stops reading BOTH directions for N ms: bytes queue in the kernel socket
buffers of both peers (a real transport stall), then pumping resumes and the
queued bytes flush as a burst. No firewall, no docker, no privileges, no shell.

Hard rules (reviewer v2):
- listen is always numeric 127.0.0.1; --connect must be a numeric IPv4:port
  (the rig server). No DNS, no other bind addresses, no broad scope.
- control is a chmod-600 unix socket; an existing path is unlinked only if it
  really is a socket; newline-framed commands, 5 s control timeout.
- hold length is a hard deadline: an independent watchdog force-releases the
  gate, and pumps gate.wait() with a timeout as backstop.
- during a hold the pumps do not touch the sockets at all (gate checked before
  recv), so queued bytes stay in kernel buffers; bytes-held is measured with
  FIONREAD, not by consuming.
- lifecycle: one client connection at a time; on disconnect the proxy cleans
  up and exits; the control socket file is removed on any exit.
- receipts are unique (ns timestamp) and include post-release flush byte
  counts for 250 ms, proving the burst happened.

Trigger: printf "HOLD 1200\n" | socat - UNIX-CONNECT:<control.sock>
Static-review gate: do not run against the rig until reviewer sign-off + GO.
"""
import argparse, array, fcntl, json, os, re, socket, stat, struct, threading, time, termios

MAX_HOLD_MS = 5000
BUF = 65536
IPPORT = re.compile(r"^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}):(\d{1,5})$")

def parse_upstream(s):
    m = IPPORT.match(s)
    if not m: raise ValueError("upstream must be numeric IPv4:port")
    octets = [int(x) for x in m.groups()[:4]]
    if any(o > 255 for o in octets): raise ValueError("bad IPv4 octet")
    port = int(m.group(5))
    if not (1 <= port <= 65535): raise ValueError("bad port")
    return ".".join(str(o) for o in octets), port

def queued_bytes(sock):
    b = array.array("i", [0])
    fcntl.ioctl(sock.fileno(), termios.FIONREAD, b, True)
    return b[0]


class ReleaseMeter:
    """Thread-safe monotonic release window; initialized BEFORE opening the gate."""
    def __init__(self, clock=time.monotonic):
        self.clock = clock; self.lock = threading.Lock(); self.until = None; self.count = 0
    def begin(self):
        with self.lock: self.count = 0; self.until = self.clock() + 0.25
    def sent(self, size):
        with self.lock:
            if self.until is not None and self.clock() < self.until: self.count += size
    def snapshot(self):
        with self.lock: return self.count

class Proxy:
    def __init__(self, listen_port, upstream, control_path, receipt_dir, clock=time.monotonic, sleep=time.sleep, timer=threading.Timer):
        self.listen_addr = ("127.0.0.1", listen_port)
        self.upstream = upstream
        self.control_path = control_path
        self.receipt_dir = receipt_dir
        self.gate = threading.Event(); self.gate.set()
        self.clock = clock; self.sleep = sleep; self.timer = timer
        self.meter = ReleaseMeter(clock)
        self.client = None; self.server = None; self.listener = None; self.ctl = None
        self.dead = threading.Event()
        self.listen_ready = threading.Event(); self.control_ready = threading.Event()

    def pump(self, src, dst, tag):
        try:
            while not self.dead.is_set():
                self.gate.wait(1.0)  # before recv: during a hold we do not touch the socket
                if self.dead.is_set(): break
                try: data = src.recv(BUF)
                except socket.timeout: continue
                if not data: break
                while not self.gate.wait(1.0):  # hold started while we were in recv: park the bytes, forward nothing
                    if self.dead.is_set(): return
                dst.sendall(data)
                if tag == "c2s": self.meter.sent(len(data))
        except OSError:
            pass
        finally:
            self.dead.set()

    def hold_async(self, ms):
        ms = min(ms, MAX_HOLD_MS)
        path = os.path.join(self.receipt_dir, f"stall-receipt-{time.time_ns()}.json")
        self.gate.clear()  # synchronous: OK means the hold is already in effect
        threading.Thread(target=self.hold, args=(ms, path), daemon=True).start()
        return path

    def hold(self, ms, path):
        started = time.time(); started_mono = self.clock()
        watchdog = self.timer(ms / 1000 + 0.5, self.gate.set); watchdog.start()  # hard deadline, independent of this thread
        self.sleep(ms / 1000)
        # Snapshot held bytes and arm meter BEFORE gate.set lets the pumps flush.
        c2s_q = queued_bytes(self.client) if self.client else -1
        s2c_q = queued_bytes(self.server) if self.server else -1
        self.meter.begin()
        self.gate.set()
        watchdog.cancel()
        released = time.time(); released_mono = self.clock()
        self.sleep(0.3)
        receipt = {
            "kind": "user-space-tcp-hold-release-v2",
            "listen": f"{self.listen_addr[0]}:{self.listen_addr[1]}",
            "upstream": f"{self.upstream[0]}:{self.upstream[1]}",
            "hold_ms_requested": ms,
            "held_at_epoch": started, "released_at_epoch": released,
            "hold_ms_actual": round((released_mono - started_mono) * 1000, 1),
            "kernel_queued_c2s_at_release": c2s_q, "kernel_queued_s2c_at_release": s2c_q,
            "flushed_c2s_bytes_250ms_after_release": self.meter.snapshot(),
            "mechanism": "proxy pumps stopped before recv; kernel buffers queue; resume flushes a burst. Scope: this one proxied connection.",
        }
        with open(path, "w") as f: json.dump(receipt, f, indent=2)
        return path

    def serve_control(self):
        if os.path.lexists(self.control_path):
            st = os.stat(self.control_path)
            if not stat.S_ISSOCK(st.st_mode):
                raise SystemExit(f"control path {self.control_path} exists and is not a socket; refusing to touch it")
            os.unlink(self.control_path)
        self.ctl = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self.ctl.bind(self.control_path); os.chmod(self.control_path, 0o600); self.ctl.listen(2); self.ctl.settimeout(0.25); self.control_ready.set()
        while not self.dead.is_set():
            try: c, _ = self.ctl.accept()
            except socket.timeout: continue
            except OSError: break
            try:
                c.settimeout(5.0)
                data = b""
                while b"\n" not in data and len(data) < 64:
                    chunk = c.recv(64)
                    if not chunk: break
                    data += chunk
                parts = data.decode(errors="replace").strip().split()
                if len(parts) == 2 and parts[0] == "HOLD" and parts[1].isdigit():
                    c.sendall(b"OK " + self.hold_async(int(parts[1])).encode() + b"\n")
                else:
                    c.sendall(b"ERR expected: HOLD <ms>\n")
            except socket.timeout:
                pass  # a stalled control client costs one connection, not the loop
            except OSError:
                if self.dead.is_set(): break
            finally:
                c.close()

    def run(self):
        threading.Thread(target=self.serve_control, daemon=True).start()
        self.listener = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.listener.bind(self.listen_addr); self.listener.listen(1); self.listen_ready.set()
        print(f"proxy on {self.listen_addr[0]}:{self.listen_addr[1]} -> {self.upstream[0]}:{self.upstream[1]}", flush=True)
        try:
            self.client, peer = self.listener.accept()
            self.client.settimeout(1.0)
            self.server = socket.create_connection(self.upstream, timeout=5.0)
            self.server.settimeout(1.0)
            print(f"client {peer} forwarding", flush=True)
            t1 = threading.Thread(target=self.pump, args=(self.client, self.server, "c2s")); t1.start()
            t2 = threading.Thread(target=self.pump, args=(self.server, self.client, "s2c")); t2.start()
            t1.join(); t2.join()
        finally:
            self.dead.set(); self.gate.set()
            for s in (self.client, self.server, self.listener, self.ctl):
                if s:
                    try: s.close()
                    except OSError: pass
            try:
                if os.path.lexists(self.control_path): os.unlink(self.control_path)
            except OSError: pass

def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--listen-port", type=int, default=25566, help="loopback listen port (127.0.0.1 only)")
    ap.add_argument("--connect", required=True, help="rig server numeric IPv4:port, e.g. 172.26.0.2:25565")
    ap.add_argument("--control", required=True, help="unix socket path for HOLD commands")
    ap.add_argument("--receipt-dir", default=".")
    a = ap.parse_args()
    if not (1024 <= a.listen_port <= 65535): raise SystemExit("bad listen port")
    try: upstream = parse_upstream(a.connect)
    except ValueError as e: raise SystemExit(str(e))
    Proxy(a.listen_port, upstream, a.control, a.receipt_dir).run()

if __name__ == "__main__":
    main()
