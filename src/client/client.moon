json = require "JSON"
internet = require "internet"
component = require "component"
event = require "event"

properties =
    name: "ship"
    password: "12345678"
    remote_address: "0.0.0.0"
    remote_port: 0
    event_blacklist: 
        internet_ready: true
        touch: true
        drag: true
        drop: true
        scroll: true
        redstone_changed: true
        key_up: true
        key_down: true
        clipboard: true
        chat_message: true

json_encode = (tbl) ->
    return "#{json.encode(tbl)}\r\n"

process_method = (root, method, args) ->
    for part in *method
        success, _next = pcall(() -> root[part])
        if not success
            return success, _next
        root = _next
    return pcall root, table.unpack args


conn = internet.socket(properties.remote_address, properties.remote_port)
while true
    socket_data = conn\read!
    continue if data == ""
    if not data
        conn = internet.socket(properties.remote_address, properties.remote_port)
    else
        local success, data
        while not success
            success, data = pcall(json.decode, socket_data)
            new_data = conn\read!
            data += new_data
            if new_data == ""
                break
        continue if not success
        if data.type == "AUTHORIZATION_REQUIRED"
            conn\write json_encode 
                name: properties.name, 
                password: name.password
        elseif data.type == "SERVICE_NOT_FOUND"
            print("Wrong service name")
            break
        elseif data.type == "SERVICE_BUSY"
            print("Service busy")
            break
        elseif data.type == "WRONG_PASSWORD"
            print("Wrong password")
            break
        elseif data.type == "PING"
            conn\write json_encode 
                type: "PONG"
                hash: data.hash
        elseif data.type == "EXECUTE"
            local stack = {}
            local success, result
            _ENV.getResultFromStack = (index) -> stack[index]
            for call in *data.call_stack
                if call.type == "CODE"
                    success, result = load(call.code)
                    if not success:
                        break
                    success, result = pcall(result)
                    if not success:
                        break
                elseif call.type == "FUNCTION"
                    args = for arg in *call.args
                        if type(arg) == "string"
                            match = string.match(arg, "^$(%d+)$")
                            if match
                                stack[match]
                            else
                                arg
                    success, result = process_method(package.loaded, call.function, args)
                    if not success:
                        break
            _ENV.getResultFromStack = nil
            conn\write json_encode
                type: "RESULT"
                hash: data.hash
                result: result
                success: success



conn\close!