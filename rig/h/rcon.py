import socket, struct, sys
def rcon(host, port, pw, cmd):
    s = socket.create_connection((host, port), timeout=10)
    def pkt(i, t, b):
        d = struct.pack('<ii', i, t) + b.encode() + b'\x00\x00'
        return struct.pack('<i', len(d)) + d
    s.sendall(pkt(1, 3, pw))
    r = s.recv(4096)
    rid = struct.unpack('<i', r[4:8])[0]
    if rid == -1: raise SystemExit('AUTH FAIL')
    s.sendall(pkt(2, 2, cmd))
    buf = b''
    while True:
        try: chunk = s.recv(4096)
        except socket.timeout: break
        if not chunk: break
        buf += chunk
        if len(buf) >= 12 and len(buf) >= 4 + struct.unpack('<i', buf[:4])[0]: break
    ln = struct.unpack('<i', buf[:4])[0]
    body = buf[12:4+ln-2]
    print(body.decode(errors='replace'))
    s.close()
if __name__ == '__main__':
    rcon(sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4])
