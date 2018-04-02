package jexxus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Connection {

  private final byte[] headerInput = new byte[4], headerOutput = new byte[4];
  private final byte[] decompressionBuffer = new byte[1024];

  private final Socket socket;
  private boolean compressed;

  public final String ip;
  private final InputStream input;
  private final OutputStream output;
  private boolean connected = false;
  private Consumer<byte[]> messageCallback = (byte[] data) -> {
  };
  private Runnable disconnectCallback = () -> {
  };

  Connection(Socket socket, boolean compressed) throws Exception {
    this.socket = socket;
    this.compressed = compressed;

    ip = socket.getInetAddress().getHostAddress();
    input = new BufferedInputStream(socket.getInputStream());
    output = new BufferedOutputStream(socket.getOutputStream());
  }

  Connection listen() {
    new Thread(listener, "Jexxus-TCPSocketListener").start();
    return this;
  }

  public boolean isConnected() {
    return connected;
  }

  public Connection onMessage(Consumer<byte[]> callback) {
    messageCallback = callback;
    return this;
  }

  public Connection onDisconnect(Runnable callback) {
    disconnectCallback = callback;
    return this;
  }

  private Runnable listener = () -> {
    connected = true;
    while (true) {
      try {
        byte[] data = readTCP();
        if (data == null) {
          break;
        }
        try {
          messageCallback.accept(data);
        } catch (Throwable t) {
          t.printStackTrace();
        }
      } catch (Exception e) {
        e.printStackTrace();
        break;
      }
    }
    connected = false;
    disconnectCallback.run();
  };

  private byte[] readTCP() throws Exception {
    if (input.read(headerInput) == -1) {
      return null;
    }
    int len = ByteBuffer.wrap(headerInput).getInt();
    if (len > 1_000_000_000) {
      throw new IllegalStateException("Someone is trying to send us a " + len + " byte message!");
    }
    byte[] data = new byte[len];
    int count = 0;
    while (count < len) {
      count += input.read(data, count, len - count);
    }

    if (compressed) {
      data = decompress(data);
    }
    return data;
  }

  public void send(byte[] data) {
    try {
      if (compressed) {
        data = compress(data);
      }
      synchronized (output) {
        ByteBuffer.wrap(headerOutput).putInt(data.length);
        output.write(headerOutput);
        output.write(data);
        output.flush();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected byte[] compress(byte[] data) {
    Deflater compressor = new Deflater();
    compressor.setInput(data);
    compressor.finish();

    ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
    byte[] buf = new byte[1024];
    while (!compressor.finished()) {
      int count = compressor.deflate(buf);
      bos.write(buf, 0, count);
    }

    return bos.toByteArray();
  }

  private byte[] decompress(byte[] data) {
    Inflater decompressor = new Inflater();
    decompressor.setInput(data);

    ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);

    byte[] buf = decompressionBuffer;
    while (!decompressor.finished()) {
      try {
        int count = decompressor.inflate(buf);
        bos.write(buf, 0, count);
      } catch (DataFormatException e) {
        throw new RuntimeException(e);
      }
    }

    return bos.toByteArray();
  }

  public void close() {
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
