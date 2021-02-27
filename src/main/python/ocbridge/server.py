import json
import logging
from selectors import DefaultSelector, EVENT_READ, EVENT_WRITE
from socket import socket
from threading import Thread
from pydantic import ValidationError

from ocbridge.structure import *
from ocbridge.utils import json_to_bytes
from service import Service

logger = logging.getLogger("OCBridge")


class BridgeServer(Thread):
    services: List[Service]
    listen_socket: socket
    listen_address: str
    sockets: list
    port: int
    selector: DefaultSelector

    def __init__(self, port: int = None, listen_address="0.0.0.0"):
        super().__init__(name="OpenComputers Bridge Server Thread", daemon=True)
        self.services = []
        self.listen_socket = socket()
        self.listen_address = listen_address
        self.port = port
        self.selector = DefaultSelector()  # Если там SelectSelector - пизда при высокой нагрузке
        self._pending_stop = False
        self.sockets = [self.listen_socket]
        if port is not None:
            self.start()

    def register(self, service: Service):
        self.services.append(service)


    # Проверяй блять сам, запускал ты его уже или нет, вызов этого метода конкретного экземпляра должен содержаться в твоем коде всего один раз
    # Иначе это говнокод
    def start(self, port: int = None):
        if port is not None:
            self.port = port
        elif self.port is None:
            raise ValueError("Port is None")
        self.listen_socket.bind((self.listen_address, self.port))
        self.listen_socket.listen()
        self.listen_socket.setblocking(False)
        self.selector.register(self.listen_socket, EVENT_READ, None)
        super(BridgeServer, self).start()

    def stop(self):
        """
        Останавливает сервер за ~5 секунд.
        Внимание: инвалидирует поток к херам, еще раз запустить нельзя
        :return: None
        """
        self._pending_stop = True

    # Касаемо селектора тут все говнокод полный, потому что сам селектор говнокод полный
    # Пиздец блять, это как нахуй можно было написать такой код, который даже IDE не может понять
    # и приходится юзать ебаные noinspection аннотации
    # Пиздец блять
    # ***
    # Вы бля знаете че они сделали?
    # https://github.com/python/cpython/blob/a64de63248267acec87a3ffb6de66ee7008977b2/Lib/selectors.py#L328
    # ЕБАТЬ СМЕШАЕМ ДВА МАССИВА ЧТОБЫ ПРОВЕРЯТЬ КАКОЙ ЭЛЕМЕНТ ИЗ КАКОГО МАССИВА
    # А ЧЕ ЗАТО КОДА МЕНЬШЕ
    def run(self):
        while True:
            events = self.selector.select(timeout=5)
            for key, mask in events:
                # FIXME 27.02.2021 тип всегда socket, но идея считает иначе
                # noinspection PyTypeChecker
                sock: socket = key.fileobj
                if key.fileobj == self.listen_socket:
                    conn, address = sock.accept()
                    logger.log(0, f"Peer {address} connected.")
                    # conn.getpeername()
                    conn.setblocking(False)
                    self.selector.register(conn, EVENT_READ, data=None)
                    conn.send(json_to_bytes(AuthorizationRequired))
                else:
                    conn: socket = sock
                    data = ""
                    try:
                        # На яве это более лаконично выглядит
                        while True:  # Считываем даже самые длинные ответы длиной в пару иоттабайт, если достаточно оперативки
                            b_data: bytes = conn.recv(4096)
                            if b_data is None:
                                read = -1
                                break
                            data += b_data.decode("UTF-8")
                            read = len(b_data)
                            if read == 0:
                                break
                    except UnicodeDecodeError:
                        logger.log(0, f"Peer {conn.getpeername()} is using wrong encoding. Closing connection")
                        self.selector.unregister(conn)
                        conn.close()
                        return
                    service = next(filter(lambda x: x.connection == conn, self.services))
                    if service:
                        if read == -1:
                            self.selector.unregister(conn)
                            service.unbind()
                            logger.log(0, f"Peer {conn.getpeername()} disconnected.")
                        # TODO
                    else:
                        if read == -1:
                            logger.log(0, f"Peer {conn.getpeername()} disconnected.")
                            self.selector.unregister(conn)
                            conn.close()
                        try:
                            model = AuthenticationData(**json.loads(data))
                        except ValidationError:
                            logger.log(0, f"Peer {conn.getpeername()} is disconnected.")
                            self.selector.unregister(conn)
                            conn.close()
                            return
                        service = next(filter(lambda x: not x.pending_remove and x.name == model.name, self.services),
                                       default=False)
                        if not service:
                            conn.send(json_to_bytes(NotFound))
                        elif not service.is_ready:
                            conn.send(json_to_bytes(ServiceBusy))
                        elif service.password != model.password:
                            conn.send(json_to_bytes(WrongPassword))
                        else:
                            service.bind(conn)
            self.selector.get_map()
            if self._pending_stop:
                break
        # TODO блять, а как закрыть сокеты-то???
        self.selector.close()
        self.listen_socket.close()
