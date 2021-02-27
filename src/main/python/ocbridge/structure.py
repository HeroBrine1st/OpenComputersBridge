import time
from typing import List, Union

from pydantic import BaseModel, root_validator, Field
from enum import Enum
from pydantic.typing import NoneType
from uuid import UUID, uuid4

## REQUEST ##

AuthorizationRequired = {
    "type": "AUTHORIZATION_REQUIRED"
}

ServiceBusy = {
    "type": "SERVICE_BUSY"
}

NotFound = {
    "type": "SERVICE_NOT_FOUND"
}

WrongPassword = {
    "type": "WRONG_PASSWORD"
}

class CallStackEntry(BaseModel):
    class Type(Enum):
        FUNCTION = "FUNCTION"
        CODE = "CODE"
    type: Type

class CodeEntry(CallStackEntry):
    type = CallStackEntry.Type.CODE
    code: str

class FunctionEntry(CallStackEntry):
    type = CallStackEntry.Type.FUNCTION
    function: List[str]
    args: List[Union[bool, str, int, NoneType, float]]

class RequestStructure(BaseModel):
    class Type(Enum):
        PING = "PING"
        EXECUTE = "EXECUTE"
    hash: UUID = Field(default_factory=uuid4)
    type: Type
    call_stack: list

    #Transient
    _timestamp: float = Field(default_factory=time.time)
    @property
    def timestamp(self):
        return self._timestamp

class PingStructure(RequestStructure):
    call_stack = None
    type = RequestStructure.Type.PING

## RESPONSE ##

# data class AuthenticationData(val type: String?, val name: String?, val password: String?)

class AuthenticationData(BaseModel):
    type = Field("AUTHENTICATION", const=True)
    name: str
    password: str

class ResponseStructure(BaseModel):
    class Type(Enum):
        PONG = "PONG"
        RESULT = "RESULT"
        MESSAGE = "MESSAGE"
        EVENT = "EVENT"
    type: Type
    hash: UUID
    result: list = None
    success: bool = None
    message: str = None
    events: list  = None

    @root_validator(skip_on_failure=True)
    def validator(cls, values):
        if values["type"] == ResponseStructure.Type.RESULT:
            assert "result" in values and "success" in values
        elif values["type"] == ResponseStructure.Type.MESSAGE:
            assert "message" in values
        elif values["type"] == ResponseStructure.Type.EVENT:
            assert "events" in values
        return values


