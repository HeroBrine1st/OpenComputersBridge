import abc
import logging

from ocbridge.utils import model_to_bytes
from socket import socket
from typing import Optional
from ocbridge.structure import *
logger = logging.getLogger("OCBridge")

class Service(abc.ABC):
    connection: Optional[socket]
    name: str
    password: str
    pending_remove: bool
    pending: list
    callbacks: dict
    last_ping_timestamp: int

    def __init__(self, name: str, password: str):
        self.connection = None
        self.name = name
        self.password = password
        self.last_ping_timestamp = 0

    @property
    def is_ready(self):
        return self.connection is not None

    def bind(self, connection):
        self.connection = connection
        self.on_connect()

    def unbind(self):
        if self.connection:
            self.connection.close()
        self.connection = None
        self.on_disconnect()

    @abc.abstractmethod
    def on_connect(self):
        pass

    @abc.abstractmethod
    def on_disconnect(self):
        pass

    @abc.abstractmethod
    def on_message(self, message: str):
        pass

    @abc.abstractmethod
    def on_event(self, event: tuple):
        pass

    def heartbeat(self):
        if not self.is_ready: return
        if time.time() - self.last_ping_timestamp > 5:
            if next(filter(lambda x: x.type == RequestStructure.Type.PING,self.pending), default=False):
                self.unbind()
                logger.info(f"{self.name} disconnected cause of no response")
        else:
            req = PingStructure()
            while next(filter(lambda x: x.hash == req.hash, self.pending)): # Очень редкий случай, что-то вроде 1/65536
                req = PingStructure()
            self.pending.append(req)
            self.connection.send(model_to_bytes(req))
            self.last_ping_timestamp = req.timestamp