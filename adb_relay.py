import socket
import threading
import sys

LOCAL_HOST = '127.0.0.1'
LOCAL_PORT = 5555
REMOTE_HOST = '192.168.0.142'
REMOTE_PORT = 5555

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
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((LOCAL_HOST, LOCAL_PORT))
    server.listen(5)
    print(f"Proxy listening on {LOCAL_HOST}:{LOCAL_PORT} -> {REMOTE_HOST}:{REMOTE_PORT}")
    sys.stdout.flush()

    while True:
        client_sock, addr = server.accept()
        try:
            remote_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            remote_sock.connect((REMOTE_HOST, REMOTE_PORT))
            t1 = threading.Thread(target=forward, args=(client_sock, remote_sock), daemon=True)
            t2 = threading.Thread(target=forward, args=(remote_sock, client_sock), daemon=True)
            t1.start()
            t2.start()
        except Exception as e:
            print(f"Failed to connect to remote: {e}")
            client_sock.close()

if __name__ == '__main__':
    main()
