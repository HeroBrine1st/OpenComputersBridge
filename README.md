# OpenComputersBridge
Kotlin/Java library and OpenComputers client for integrating into your application.

P.s. [JSON library](https://pastebin.com/ji28sbxU), put it into ``/lib/JSON.lua`` on OpenComputers.
# Features

* Execution of existing functions on remote OpenComputers device (without compilation, fastest way)
* Execution of arbitrary lua code on remote OpenComputers device (with compilation, slow)
* Events receiving with(out) filter (by default client ignores almost all user input events)
* Ability to send any messages over protocol.
* If your client has freezed or crashed, server will disconnect from it in about 10 seconds

# Future plans

* Python library
* Declare OpenComputers methods in library for fast developing

# How it works


Device can connect to host over TCP protocol (without encryption), then select a service and authorize. Service can be created on the host side.
If device is disconnected it can connect again, but two devices cannot connect to the one service at the same time.

When host executing a request, device is receiving a "call stack" that it will execute one-by-one and save results until request is fully executed. 
You can use any of previous stack entries results for use in next.

Example request:

```
{
  "type":"EXECUTE",
  "hash":"500",
  "call_stack":[{
      "function":["computer","beep"],
      "args":[2000,0.5],
      "type":"FUNCTION"
    },{
      "code":"computer.beep(2000, 0.5)",
      "type":"CODE"
    }
  ]
 }
```
Performs ``computer.beep(2000, 0.5)`` twice.
