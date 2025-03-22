# libsev
libsev is an event loop based on liburing for asynchronous I/O. It uses Java's new FFM API to call liburing functions. Also included are FFM handles for 
POSIX socket functions. Thanks to [mitchellh](https://github.com/mitchellh) and [tigerbeetle](https://github.com/tigerbeetle) for the inspiration!

## Example
```java
try(Arena arena = Arena.ofConfined()) {

  int server = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
  setSockOpt(server, SOL_SOCKET, SO_REUSEADDR, 1);
  Inet4Address address = (Inet4Address) InetAddress.getByName("127.0.0.1")
  MemorySegment localhost = InetUtil.sockAddrInet4(address, 8080, arena);
  bind(server, localhost, SOCKADDR_IN_SIZE);
  listen(serverFd, 1);

  UringLoop loop = UringLoop.init();
  Operation acceptOperation = new Operation.Accept();
  acceptOperation.fd(serverFd);
  Callback<UringLoop, UringCompletion> acceptCallback = (context, uringLoop, completion, result) -> {
    System.out.println(result);
    return false;
  }
  UringCompletion acceptCompletion = new UringCompletion();
  acceptCompletion.operation(acceptOperation)
                  .context(null)
                  .callback(acceptCallback);

  loop.enqueue(acceptCompletion);
  loop.runAll();
  loop.deinit();

} finally {}
```

The code first creates a socket and binds it to the address.</br> 

A loop is created:
```java
UringLoop loop = UringLoop.init();
```

We define the operation we want to do. An operation can one of many traditional I/O operations, such as read(), write(), recv(), send(), connect(), etc.,
but can also be operations similar to those offered by epoll()/poll(), or even timeouts.
In this case we want to do an accept operation:</br>
```java
Operation acceptOperation = new Operation.Accept();
```

We then define the callback to be run upon completion of the operation. In this case we just print the result, which will be either the file descriptor of the
accepted socket or the negative errno value. We also return false, indicating that the Completion should not be requeued to the loop.</br>

Completions are what we can enqueue to the loop. A Completion takes an operation, callback, and a "context", which is just an arbitrary object that can be anything
the user wants it to be, akin to a "userdata" object.</br>

We then run `loop.runAll()` to make the loop run until all enqueued Completions have their I/O requests completed (by liburing) and their callbacks run.
