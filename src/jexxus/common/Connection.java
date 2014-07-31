package jexxus.common;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents a connection between two computers.
 * 
 * @author Jason
 * 
 */
public abstract class Connection {

  private static final int MAGIC_NUMBER = 1304231989;

  private long bytesSent = 0;

  protected ConnectionListener listener;

  /**
   * Checks to see whether the current connection is open.
   * 
   * @return True if the connection is established.
   */
  public abstract boolean isConnected();

  /**
   * Sends the given data over this connection.
   * 
   * @param data
   *            The data to send to the other computer.
   * @param deliveryType
   *            The requirements for the delivery of this data.
   */
  public abstract void send(byte[] data, Delivery deliveryType);

  protected abstract ReusableInputStream getTCPInputStream();

  protected abstract ResuableOutputStream getTCPOutputStream();

  /**
   * Closes the connection. Further data may not be transfered across this link.
   */
  public abstract void close();

  private final byte[] headerInput = new byte[8];
  private final byte[] headerOutput = new byte[8];

  public Connection(ConnectionListener listener) {
    if (listener == null) {
      throw new RuntimeException("You must supply a connection listener.");
    }
    this.listener = listener;
  }

  protected byte[] readTCP() throws IOException {
    ReusableInputStream tcpInput = getTCPInputStream();

    try {
      tcpInput.getReady();
    } catch (NullPointerException e) {
      return null;
    }

    if (tcpInput.read(headerInput) == -1) {
      return null;
    }
    int magicNumber = ByteBuffer.wrap(headerInput).getInt();
    if (magicNumber != MAGIC_NUMBER) {
      throw new InvalidProtocolException("Bad magic number: " + magicNumber);
    }
    int len = ByteBuffer.wrap(headerInput).getInt(4);
    byte[] data = new byte[len];
    int count = 0;
    while (count < len) {
      count += tcpInput.read(data, count, len - count);
    }

    return data;
  }

  protected void sendTCP(byte[] data) throws IOException {
    ResuableOutputStream tcpOutput = getTCPOutputStream();

    ByteBuffer.wrap(headerOutput).putInt(MAGIC_NUMBER);
    ByteBuffer.wrap(headerOutput).putInt(4, data.length);
    tcpOutput.write(headerOutput);
    tcpOutput.write(data);
    tcpOutput.finish();

    bytesSent += data.length;
  }

  public void setConnectionListener(ConnectionListener listener) {
    this.listener = listener;
  }

  public long getBytesSent() {
    return bytesSent;
  }

}
