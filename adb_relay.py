import socket
import threading
import sys

LOCAL_HOST = '127.0.0.1'
REMOTE_HOST = '192.168.0.142'

def forward(src, dst):
    try:
        while True:
            data = src.recv(4096)
            if not data:
                break
            dst.sendall(data)
    except Exception:
        pass
    finally:
        try:
            src.close()
        except Exception:
            pass
        try:
            dst.close()
        except Exception:
            pass

def main():
    remote_port = int(sys.argv[1]) if len(sys.argv) > 1 else 5555
    local_port = int(sys.argv[2]) if len(sys.argv) > 2 else remote_port

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((LOCAL_HOST, local_port))
    server.listen(5)
    print(f"Proxy listening on {LOCAL_HOST}:{local_port} -> {REMOTE_HOST}:{remote_port}")
    sys.stdout.flush()

    while True:
        client_sock, addr = server.accept()
        try:
            remote_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            remote_sock.connect((REMOTE_HOST, remote_port))
            t1 = threading.Thread(target=forward, args=(client_sock, remote_sock), daemon=True)
            t2 = threading.Thread(target=forward, args=(remote_sock, client_sock), daemon=True)
            t1.start()
            t2.start()
        except Exception as e:
            print(f"Failed to connect to remote: {e}")
            client_sock.close()

if __name__ == '__main__':
    main()
