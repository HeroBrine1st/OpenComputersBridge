import json

from pydantic import BaseModel


def json_to_bytes(json_data):
    return bytes(json.dumps(json_data) + "\r\n", encoding="UTF-8")

def model_to_bytes(model: BaseModel):
    return bytes(model.json() + "\r\n", encoding="UTF-8")