json = require "JSON"
internet = require "internet"
component = require "component"
event = require "event"
computer = require "computer"

properties =
    name: ""
    password: ""
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

local conn

json_encode = (tbl) ->
    return "#{json.encode(tbl)}\r\n"

process_method = (root, method, args) ->
    for part in *method
        success, _next = pcall(() -> root[part])
        if not success
            return success, _next
        root = _next
    return pcall root, table.unpack args

optrequire = (...) ->
  success, module = pcall(require, ...)
  if success
    return module

split = (source, delimiters) ->
  [match for match in string.gmatch(source, '([^'..delimiters..']+)')]
  

env = setmetatable({}, { -- Эта метатаблица позволяет отделить окружение исполняемого кода от глобального, сохраняя непосредственную связь
  __index: (_, k) ->
    return _ENV[k] or optrequire(k)
  __pairs: (t) -> -- Это спизжено из /lib/core/lua_shell.lua и не нужно, но пусть будет
    return (_, key) -> 
      k, v = next(t, key)
      if not k and t == env then
        t = _ENV
        k, v = next(t)
      if not k and t == _ENV then
        t = package.loaded
        k, v = next(t)
      return k, v})

env.send_message = (msg) ->
    conn\write json_encode
        type: "MESSAGE"
        message: msg

execute_code = (code) -> 
    res = {load(code, "=OCBridge", "t", env)}
    if not res[1]
        return res
    res2 = {pcall(res[1])}
    if not res2[1]
        res2[2] = debug.traceback(res2[2])
        
    return res2

while true
    if conn == nil
        os.sleep(0.5)
        print("No connection found. Trying to connect...")
        conn = internet.socket(properties.remote_address, properties.remote_port)
        continue
    
    socket_data = conn\read!
    while true
        new_data = conn\read!
        if new_data == nil
            socket_data = nil
            break
        if new_data == ""
            break
        socket_data = socket_data .. new_data

    if socket_data ~= ""
        if not socket_data
            conn = nil
            continue
        for request in *split(socket_data, "\n")
            success, data = pcall(json.decode, request)
            continue if not success
            if data.type == "AUTHORIZATION_REQUIRED"
                conn\write json_encode 
                    type: "AUTHENTICATION"
                    name: properties.name, 
                    password: properties.password
            elseif data.type == "SERVICE_NOT_FOUND"
                print("Wrong service name")
                conn\close!
                return
            elseif data.type == "SERVICE_BUSY"
                print("Service busy")
                conn\close!
                return
            elseif data.type == "WRONG_PASSWORD"
                print("Wrong password")
                conn\close!
                return
            elseif data.type == "PING" -- socket heartbeat
                conn\write json_encode 
                    type: "PONG"
                    hash: data.hash
            elseif data.type == "EXECUTE"
                stack = {}
                local success, result
                env.getResultFromStack = (index) -> stack[index]
                for call in *data.call_stack
                    if call.type == "CODE" -- TODO какая-то хрень при синтаксической ошибке
                        -- Если синтаксическая ошибка, то ничего не отправляется
                        res = execute_code(call.code)
                        success = res[1]
                        result = {table.unpack(res, 2, #res)}
                    elseif call.type == "FUNCTION"
                        args = for arg in *call.args
                            if type(arg) == "string"
                                match, index = string.match arg, "^$(%d+)%[(%d+)%]$"
                                if match
                                    stack[match][index]
                                elseif arg == "$$"
                                    "$"
                                else
                                    arg
                            else
                                arg
                        res = {process_method package.loaded, call.function, args}
                        success = res[1]
                        result = {table.unpack(res, 2, #res)}
                    table.insert stack, result
                    if not success
                        break
                json_success, json_result = pcall json_encode, 
                    type: "RESULT"
                    hash: data.hash
                    result: result
                    success: not not success -- ебучая луа
                if not json_success
                    conn\write json_encode
                        type: "RESULT"
                        hash: data.hash
                        result: {json_result}
                        success: false
                else
                    conn\write json_result
    end_ = computer.uptime() + 0.5
    e = {event.pull(end_ - computer.uptime())}
    events = {}
    while #e > 0
        if not properties.event_blacklist[e[1]]
            table.insert(events, e)
        e = {event.pull(end_ - computer.uptime())}
    if #events > 0
        conn\write json_encode
                events: events
                type: "EVENT"
conn\close!