# Jexxus

Jexxus (Java Nexus) encapsulates the Java Sockets API, which reduces the redundancy of writing network code and makes it simpler to create an online program.

Contains both server and client capabilities. It is extremely easy to set up a server which takes advantage of both the TCP and UDP protocols.

The code below will help you set up your first server/client. They both use the "DebugConnectionListener", but you can replace that with your own custom connectionlistener which handles your application's logic


```java
public class DebugConnectionListener implements ConnectionListener{

  public void connectionBroken(Connection broken, boolean forced){
    System.out.println("Connection lost: "+broken);
  }

  public void receive(byte[] data, Connection from){
    System.out.println("Received message: "+new String(data));
  }

  public void clientConnected(ServerConnection conn){
    System.out.println("Client Connected: "+conn.getIP());
  }
}
```

To following code is all you need to set up a server.

```java
Server server = new Server(new DebugConnectionListener(), 15652);
server.startServer();
To connect a client to the server:

ClientConnection conn = new ClientConnection(clientListener, "localhost", 15652);
To connect a client to the server:

ClientConnection conn = new ClientConnection(new DebugConnectionListener(), "localhost", 15652);
conn.connect();
//send with the TCP Protocol
conn.send("Hello TCP".getBytes(), Delivery.RELIABLE);
```

Try it out! It's really easy to get started.
