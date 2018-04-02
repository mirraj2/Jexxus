package jexxus;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class Server {

  static final int DEFAULT_PORT = 30591;

  private int port = DEFAULT_PORT;
  private boolean ssl = false, compressed = false;
  private ServerSocket tcpSocket;
  private boolean running = false;
  private Consumer<Connection> onNewClient = input -> {
  };

  public Server port(int port) {
    this.port = port;
    return this;
  }

  public Server useSSL() {
    ssl = true;
    return this;
  }

  public Server compress() {
    compressed = true;
    return this;
  }

  public Server onNewClient(Consumer<Connection> callback) {
    onNewClient = callback;
    return this;
  }

  public Server start() {
    if (running) {
      throw new IllegalStateException("Server is already running!");
    }
    running = true;

    try {
      ServerSocketFactory socketFactory =
          ssl ? SSLServerSocketFactory.getDefault() : ServerSocketFactory.getDefault();
      tcpSocket = socketFactory.createServerSocket(port);

      if (ssl) {
        SSLServerSocket sslSocket = (SSLServerSocket) tcpSocket;
        sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
      }

      startTCPListener();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  private void startTCPListener() {
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        while (running) {
          try {
            Socket socket = tcpSocket.accept();
            Connection conn = new Connection(socket, compressed);
            try {
              onNewClient.accept(conn);
            } catch (Throwable e) {
              e.printStackTrace();
            }
            conn.listen();
          } catch (Exception e) {
            e.printStackTrace();
            running = false;
          }
        }
      }
    });
    t.setName("Jexxus-TCPConnectionListener");
    t.start();
  }

}
