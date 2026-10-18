#!/usr/bin/env python3
"""Deterministic clock/release tests; no subprocess, sockets, sleeps or rig."""
import importlib.util, json, os, pathlib, socket, subprocess, sys, tempfile, threading, time, unittest
ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("proxy", pathlib.Path(__file__).with_name("tcp-stall-proxy.py"))
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)

class Validation(unittest.TestCase):
    def test_upstream_must_be_numeric_ipv4_port(self):
        self.assertEqual(m.parse_upstream("172.26.0.2:25565"), ("172.26.0.2", 25565))
        for bad in ("localhost:25565", "meshac-red:25565", "1.2.3.256:25565", "1.2.3.4:0", "1.2.3.4", "1.2.3.4:99999", "1.2.3.4:25565;rm -rf /"):
            with self.assertRaises(ValueError, msg=bad): m.parse_upstream(bad)
    def test_listen_is_loopback_only(self):
        p = m.Proxy(25566, ("127.0.0.1", 25565), "/tmp/x.sock", "/tmp")
        self.assertEqual(p.listen_addr[0], "127.0.0.1")
    def test_control_path_refuses_non_socket(self):
        with tempfile.TemporaryDirectory() as d:
            f = pathlib.Path(d) / "not-a-socket"; f.write_text("x")
            p = m.Proxy(25567, ("127.0.0.1", 25565), str(f), d)
            with self.assertRaises(SystemExit): p.serve_control()

class FakeClock:
    def __init__(self): self.now=100.0
    def __call__(self): return self.now
    def sleep(self, seconds): self.now += seconds

class FakeTimer:
    def __init__(self,*args): self.cancelled=False
    def start(self): pass
    def cancel(self): self.cancelled=True

class DeterministicRelease(unittest.TestCase):
    def test_first_flush_at_gate_open_is_counted(self):
        clock=FakeClock()
        with tempfile.TemporaryDirectory() as tmp:
            p=m.Proxy(25566,("127.0.0.1",25565),tmp+"/ctl",tmp,clock,clock.sleep,FakeTimer)
            class Gate:
                def set(self): p.meter.sent(500) # pump scheduled immediately on release
            p.gate=Gate()
            path=p.hold(400,tmp+"/receipt.json")
            r=json.loads(pathlib.Path(path).read_text())
            self.assertEqual(r["flushed_c2s_bytes_250ms_after_release"],500)
            self.assertAlmostEqual(r["hold_ms_actual"],400)
    def test_exact_window_and_no_wall_clock(self):
        clock=FakeClock();meter=m.ReleaseMeter(clock)
        meter.sent(123);self.assertEqual(meter.snapshot(),0)
        meter.begin();meter.sent(500)
        clock.now+=.249;meter.sent(20)
        clock.now+=.001;meter.sent(999)
        self.assertEqual(meter.snapshot(),520)
        meter.begin();self.assertEqual(meter.snapshot(),0)
    def test_concurrent_counter_no_lost_updates(self):
        clock=FakeClock();meter=m.ReleaseMeter(clock);meter.begin()
        threads=[threading.Thread(target=lambda: [meter.sent(1) for _ in range(1000)]) for _ in range(4)]
        for t in threads:t.start()
        for t in threads:t.join()
        self.assertEqual(meter.snapshot(),4000)

class RealSocketSmoke(unittest.TestCase):
    def test_tcp_unix_control_meter_and_disconnect_cleanup(self):
        # Real sockets and pumps; events control the hold, fake monotonic clock
        # avoids asking the scheduler to forward within a250ms wall deadline.
        with tempfile.TemporaryDirectory() as tmp:
            upstream=socket.socket();upstream.bind(("127.0.0.1",0));upstream.listen(1);upstream.settimeout(5)
            errors=[]
            def echo():
                try:
                    conn,_=upstream.accept()
                    with conn:
                        conn.settimeout(5)
                        while True:
                            data=conn.recv(65536)
                            if not data:break
                            conn.sendall(data)
                except Exception as e: errors.append(e)
                finally:upstream.close()
            et=threading.Thread(target=echo);et.start()
            held=threading.Event();release=threading.Event();flushed=threading.Event();finished=threading.Event()
            def controlled_sleep(seconds):
                if seconds==.4:
                    held.set()
                    if not release.wait(5):raise AssertionError("test release missing")
                elif not flushed.wait(5):raise AssertionError("flush observation missing")
            p=m.Proxy(0,("127.0.0.1",upstream.getsockname()[1]),tmp+"/ctl",tmp,FakeClock(),controlled_sleep,FakeTimer)
            original_sent=p.meter.sent
            def observed_sent(size):original_sent(size);flushed.set()
            p.meter.sent=observed_sent
            original_hold=p.hold
            def observed_hold(ms,path):
                try:return original_hold(ms,path)
                finally:finished.set()
            p.hold=observed_hold
            def run():
                try:p.run()
                except Exception as e:errors.append(e)
            pt=threading.Thread(target=run);pt.start();client=None
            try:
                self.assertTrue(p.listen_ready.wait(5));self.assertTrue(p.control_ready.wait(5))
                client=socket.create_connection(p.listener.getsockname(),timeout=5)
                client.sendall(b"HELLO");self.assertEqual(client.recv(5),b"HELLO")
                with socket.socket(socket.AF_UNIX,socket.SOCK_STREAM) as control:
                    control.settimeout(5);control.connect(p.control_path);control.sendall(b"HOLD 400\n")
                    response=control.recv(4096);self.assertTrue(response.startswith(b"OK "))
                self.assertTrue(held.wait(5));self.assertFalse(p.gate.is_set())
                flushed.clear();client.sendall(b"STALL"*100);release.set()
                data=b""
                while len(data)<500:data+=client.recv(500-len(data))
                self.assertEqual(data,b"STALL"*100)
                self.assertTrue(finished.wait(5))
                path=response.decode().split()[1]
                self.assertEqual(json.loads(pathlib.Path(path).read_text())["flushed_c2s_bytes_250ms_after_release"],500)
                with socket.socket(socket.AF_UNIX,socket.SOCK_STREAM) as control:
                    control.settimeout(5);control.connect(p.control_path);control.sendall(b"INVALID\n")
                    self.assertTrue(control.recv(256).startswith(b"ERR"))
                client.close();client=None;pt.join(5);et.join(5)
                self.assertFalse(pt.is_alive());self.assertFalse(et.is_alive());self.assertFalse(os.path.exists(p.control_path))
                self.assertEqual(p.meter.snapshot(),500);self.assertEqual(errors,[])
            finally:
                release.set()
                if client:client.close()
                p.dead.set();p.gate.set()
                for sock in (p.client,p.server,p.listener,p.ctl):
                    if sock:
                        try:sock.shutdown(socket.SHUT_RDWR)
                        except OSError:pass
                        sock.close()
                pt.join(5);et.join(5)


if __name__ == "__main__":
    unittest.main()
