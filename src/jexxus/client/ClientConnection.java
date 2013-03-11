package jexxus.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import jexxus.common.Connection;
import jexxus.common.ConnectionListener;
import jexxus.common.Delivery;

/**
 * Used to establish a connection to a server.
 * 
 * @author Jason
 * 
 */
public class ClientConnection extends Connection {

	private Socket tcpSocket;
	private DatagramSocket udpSocket;
	protected final String serverAddress;
	protected final int tcpPort, udpPort;
	private DatagramPacket packet;
	private boolean connected = false;
	private InputStream tcpInput;
	private OutputStream tcpOutput;
	private final boolean useSSL;

	/**
	 * Creates a new connection to a server. The connection is not ready for use until <code>connect()</code> is called.
	 * 
	 * @param listener
	 *            The responder to special events such as receiving data.
	 * @param serverAddress
	 *            The IP address of the server to connect to.
	 * @param tcpPort
	 *            The port to connect to the server on.
	 */
	public ClientConnection(ConnectionListener listener, String serverAddress, int tcpPort, boolean useSSL) {
		this(listener, serverAddress, tcpPort, -1, useSSL);
	}

	/**
	 * Creates a new connection to a server. The connection is not ready for use until <code>connect()</code> is called.
	 * 
	 * @param listener
	 *            The responder to special events such as receiving data.
	 * @param serverAddress
	 *            The IP address of the server to connect to.
	 * @param tcpPort
	 *            The port to send data using the TCP protocol.
	 * @param udpPort
	 *            The port to send data using the UDP protocol.
	 */
	public ClientConnection(ConnectionListener listener, String serverAddress, int tcpPort, int udpPort, boolean useSSL) {
		super(listener);

		this.listener = listener;
		this.serverAddress = serverAddress;
		this.tcpPort = tcpPort;
		this.udpPort = udpPort;
		this.useSSL = useSSL;

		if (udpPort != -1) {
			try {
				packet = new DatagramPacket(new byte[0], 0, new InetSocketAddress(serverAddress, udpPort));
				udpSocket = new DatagramSocket();
			} catch (IOException e) {
				System.err.println("Problem initializing UDP on port " + udpPort);
				System.err.println(e.toString());
			}
		}
	}

	public synchronized void connect() throws IOException {
		connect(0);
	}

	/**
	 * Tries to open a connection to the server.
	 * 
	 * @return true if the connection was successful, false otherwise.
	 */
	public synchronized void connect(int timeout) throws IOException {
		if (connected) {
			throw new IllegalStateException("Tried to connect after already connected!");
		}

		SocketFactory socketFactory = useSSL ? SSLSocketFactory.getDefault() : SocketFactory.getDefault();
		tcpSocket = socketFactory.createSocket();

		final String[] enabledCipherSuites = { "SSL_DH_anon_WITH_RC4_128_MD5" };
		((SSLSocket) tcpSocket).setEnabledCipherSuites(enabledCipherSuites);

		tcpSocket.connect(new InetSocketAddress(serverAddress, tcpPort), timeout);
		tcpInput = new BufferedInputStream(tcpSocket.getInputStream());
		tcpOutput = new BufferedOutputStream(tcpSocket.getOutputStream());

		startTCPListener();
		connected = true;
		if (udpPort != -1) {
			startUDPListener();
			send(new byte[0], Delivery.UNRELIABLE);
		}

	}

	@Override
	public synchronized void send(byte[] data, Delivery deliveryType) {
		if (connected == false) {
			System.err.println("Cannot send message when not connected!");
			return;
		}

		if (deliveryType == Delivery.RELIABLE) {
			// send with TCP
			try {
				super.sendTCP(data);
			} catch (IOException e) {
				System.err.println("Error writing TCP data.");
				System.err.println(e.toString());
			}
		} else if (deliveryType == Delivery.UNRELIABLE) {
			if (udpPort == -1) {
				System.err.println("Cannot send Unreliable data unless a UDP port is specified.");
				return;
			}
			packet.setData(data);
			try {
				udpSocket.send(packet);
			} catch (IOException e) {
				System.err.println("Error writing UDP data.");
				System.err.println(e.toString());
			}
		}
	}

	private void startTCPListener() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (true) {
					byte[] ret;
					try {
						ret = readTCP();
					} catch (IOException e) {
						if (connected) {
							connected = false;
							listener.connectionBroken(ClientConnection.this, false);
						} else {
							listener.connectionBroken(ClientConnection.this, true);
						}
						if (udpSocket != null) {
							udpSocket.close();
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
							listener.connectionBroken(ClientConnection.this, false);
						} else {
							listener.connectionBroken(ClientConnection.this, true);
						}
						break;
					}
					try {
						listener.receive(ret, ClientConnection.this);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		t.setName("Jexxus-TCPSocketListener");
		t.start();
	}

	private void startUDPListener() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				final int BUF_SIZE = 2048;
				final DatagramPacket inputPacket = new DatagramPacket(new byte[BUF_SIZE], BUF_SIZE);
				while (true) {
					try {
						udpSocket.receive(inputPacket);
						byte[] ret = Arrays.copyOf(inputPacket.getData(), inputPacket.getLength());
						listener.receive(ret, ClientConnection.this);
					} catch (IOException e) {
						if (connected) {
							connected = false;
						}
						break;
					}
				}
			}
		});
		t.start();
	}

	@Override
	public void close() {
		if (!connected) {
			System.err.println("Cannot close the connection when it is not connected.");
		} else {
			try {
				tcpSocket.close();
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
	protected InputStream getTCPInputStream() {
		return tcpInput;
	}

	@Override
	protected OutputStream getTCPOutputStream() {
		return tcpOutput;
	}
}
