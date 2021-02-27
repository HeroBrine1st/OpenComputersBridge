from selectors import DefaultSelector, EVENT_READ
from socket import socket
from threading import Thread
from service import Service
import logging

logger = logging.getLogger("OCBridge")


#
# while True:
#     events = sel.select()
#     for key, mask in events:
#         callback = key.data
#         callback(key.fileobj, mask)

class BridgeServer(Thread):
    def __init__(self, port: int = None, listen_address="0.0.0.0"):
        super().__init__(name="OpenComputers Bridge Server Thread")
        self.socket = socket()
        self.listen_address = listen_address
        self.port = port
        self.selector = DefaultSelector() # Если там SelectSelector - пизда при высокой нагрузке
        if port is not None:
            self.start()

    def register(self, service: Service):
        pass

    def start(self, port: int = None):
        if port is not None:
            self.port = port
        elif self.port is None:
            raise ValueError("Port is None")
        self.socket.bind((self.listen_address, self.port))
        self.socket.listen()
        self.socket.setblocking(False)
        self.selector.register(self.socket, EVENT_READ, self.accept)

    def accept(self, sock):
        conn, address = sock.accept()
        logger.log(0, f"Peer {address} connected.")
        # conn.getpeername()
        conn.setblocking(False)
        self.selector.register(conn, EVENT_READ, self.read)

    def read(self, conn: socket):
        b_data: bytes = conn.recv(4096)  # Should be ready
        if b_data:
            try:
                data: str = b_data.decode("UTF-8")
            except UnicodeDecodeError:
                logger.log(0, f"Peer {conn.getpeername()} using wrong encoding. Closing connection")
                self.selector.unregister(conn)
                conn.close()
                return

        else:
            logger.log(0, f"Peer {conn.getpeername()} disconnected.")
            self.selector.unregister(conn)
            conn.close()

