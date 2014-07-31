package jexxus.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import jexxus.common.Connection;
import jexxus.common.ConnectionListener;
import jexxus.common.Delivery;
import jexxus.common.ResuableOutputStream;
import jexxus.common.ReusableInputStream;

/**
 * Represents a server's connection to a client.
 */
public class ServerConnection extends Connection {

  private final Server controller;
  private final Socket socket;
  private final ResuableOutputStream tcpOutput;
  private final ReusableInputStream tcpInput;
  private boolean connected = true;
  private final String ip;
  private int udpPort = -1;

  ServerConnection(Server controller, ConnectionListener listener, Socket socket)
      throws IOException {
    super(listener);

    this.controller = controller;
    this.socket = socket;
    this.ip = socket.getInetAddress().getHostAddress();
    tcpOutput = new ResuableOutputStream(socket.getOutputStream());
    tcpInput = new ReusableInputStream(socket.getInputStream());

    startTCPListener();
  }

  private void startTCPListener() {
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          byte[] ret;
          try {
            ret = readTCP();
          } catch (SocketException e) {
            if (connected) {
              connected = false;
              controller.connectionDied(ServerConnection.this, false);
              listener.connectionBroken(ServerConnection.this, false);
            } else {
              controller.connectionDied(ServerConnection.this, true);
              listener.connectionBroken(ServerConnection.this, true);
            }
            break;
          } catch (Exception e) {
            e.printStackTrace();
            break;
          }
          if (ret == null) {
            // the stream has ended
            if (connected) {
              connected = false;
              controller.connectionDied(ServerConnection.this, false);
              listener.connectionBroken(ServerConnection.this, false);
            } else {
              controller.connectionDied(ServerConnection.this, true);
              listener.connectionBroken(ServerConnection.this, true);
            }
            break;
          }
          try {
            listener.receive(ret, ServerConnection.this);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    });
    t.setName("Jexxus-TCPSocketListener");
    t.start();
  }

  @Override
  public synchronized void send(byte[] data, Delivery deliveryType) {
    if (connected == false) {
      throw new RuntimeException("Cannot send message when not connected!");
    }
    if (deliveryType == Delivery.RELIABLE) {
      // send with TCP
      try {
        sendTCP(data);
      } catch (IOException e) {
        System.err.println("Error writing TCP data.");
        e.printStackTrace();
      }
    } else if (deliveryType == Delivery.UNRELIABLE) {
      controller.sendUDP(data, this);
    }
  }

  /**
   * @return The IP of this client.
   */
  public String getIP() {
    return ip;
  }

  /**
   * Closes this connection to the client.
   */
  public void exit() {
    connected = false;
    try {
      tcpInput.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      tcpOutput.close();
    } catch (IOException e) {}
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  InetAddress getAddress() {
    return socket.getInetAddress();
  }

  int getUDPPort() {
    return udpPort;
  }

  void setUDPPort(int port) {
    this.udpPort = port;
  }

  @Override
  public void close() {
    if (!connected) {
      throw new RuntimeException("Cannot close the connection when it is not connected.");
    } else {
      try {
        socket.close();
        tcpInput.close();
        tcpOutput.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      connected = false;
    }
  }

  @Override
  public boolean isConnected() {
    return connected;
  }

  @Override
  protected ReusableInputStream getTCPInputStream() {
    return tcpInput;
  }

  @Override
  protected ResuableOutputStream getTCPOutputStream() {
    return tcpOutput;
  }

  @Override
  public String toString() {
    return ip;
  }
}
