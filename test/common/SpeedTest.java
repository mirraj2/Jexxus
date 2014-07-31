package common;

import jexxus.client.ClientConnection;
import jexxus.common.Connection;
import jexxus.common.ConnectionListener;
import jexxus.common.Delivery;
import jexxus.server.Server;
import jexxus.server.ServerConnection;

public class SpeedTest implements ConnectionListener {

  private static final int PORT = 32521;

  int DATA_SIZE = 1000 * 1000 * 50;
  Server s = new Server(this, PORT, false);
  long start;

  public void runTest() throws Exception {
    s.startServer();
    ClientConnection conn = new ClientConnection(this, "localhost", PORT, false);
    conn.connect(1000);
    byte[] data = new byte[DATA_SIZE];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i % 100);
    }
    start = System.nanoTime();
    conn.send(data, Delivery.RELIABLE);
    synchronized (this) {
      wait();
    }
    s.shutdown(true);
  }

  public static void main(String[] args) throws Exception {
    for (int i = 0; i < 10; i++) {
      new SpeedTest().runTest();
    }
  }

  @Override
  public void connectionBroken(Connection broken, boolean forced) {
  }

  @Override
  public void receive(byte[] data, Connection from) {
    long now = System.nanoTime();
    int elapsed = (int) (now - start);
    System.out.println(elapsed / 1000000.0 + " ms");
    synchronized (this) {
      notifyAll();
    }
  }

  @Override
  public void clientConnected(ServerConnection conn) {
  }

}
