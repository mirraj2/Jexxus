package jexxus.server;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import jexxus.common.ConnectionListener;

/**
 * Acts as a server for incoming client connections. The server can send and receive data from all clients who connect to this server.
 * 
 * @author Jason
 * 
 */
public class Server {

	private final ConnectionListener listener;
	private ServerSocket tcpSocket;
	private DatagramSocket udpSocket;
	private boolean running = false;
	protected final int tcpPort, udpPort;
	private final HashMap<String, ServerConnection> clients = new HashMap<String, ServerConnection>();
	private final HashMap<String, ServerConnection> udpClients = new HashMap<String, ServerConnection>();

	private final DatagramPacket outgoingPacket = new DatagramPacket(new byte[0], 0);

	/**
	 * Creates a new server.
	 * 
	 * @param listener
	 *            The responder to special events such as receiving data.
	 * @param port
	 *            The port to listen for client connections on. [TCP]
	 */
	public Server(ConnectionListener listener, int port, boolean useSSL) {
		this(listener, port, -1, useSSL);
	}

	/**
	 * Creates a new server.<br>
	 * <br>
	 * Note: The server will not begin listening for connections until <code>startServer()</code> is called.
	 * 
	 * @param listener
	 *            The responder to special events such as receiving data.
	 * @param tcpPort
	 *            The port to listen for TCP client connections on.
	 * @param udpPort
	 *            The port to listen for UDP client connections on. Use -1 if you don't want to use any UDP.
	 */
	public Server(ConnectionListener listener, int tcpPort, int udpPort, boolean useSSL) {
		this.listener = listener;

		this.tcpPort = tcpPort;
		this.udpPort = udpPort;
		try {
			ServerSocketFactory socketFactory = useSSL ? SSLServerSocketFactory.getDefault() : ServerSocketFactory.getDefault();
			tcpSocket = socketFactory.createServerSocket(tcpPort);

			final String[] enabledCipherSuites = { "SSL_DH_anon_WITH_RC4_128_MD5" };
			((SSLServerSocket) tcpSocket).setEnabledCipherSuites(enabledCipherSuites);

		} catch (BindException e) {
			System.err.println("There is already a server bound to port " + tcpPort + " on this computer.");
			throw new RuntimeException(e);
		} catch (IOException e) {
			if (e.toString().contains("JVM_Bind")) {
				System.err.println("There is already a server bound to port " + tcpPort + " on this computer.");
			}
			throw new RuntimeException(e);
		}
		if (udpPort != -1) {
			try {
				udpSocket = new DatagramSocket(udpPort);
			} catch (SocketException e) {
				System.err.println("There was a problem starting the server's UDP socket on port " + udpPort);
				System.err.println(e.toString());
			}
		}

	}

	/**
	 * After the server has started, it is open for accepting new client connections.
	 */
	public synchronized void startServer() {
		if (running) {
			System.err.println("Cannot start server when already running!");
			return;
		}
		running = true;
		startTCPConnectionListener();
		if (udpPort != -1) {
			startUDPListener();
		}
	}

	private void startTCPConnectionListener() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (running) {
					try {
						Socket sock = tcpSocket.accept();
						ServerConnection sc = new ServerConnection(Server.this, listener, sock);
						clients.put(sc.getIP(), sc);
						listener.clientConnected(sc);
					} catch (IOException e) {
						if (running) {
							e.printStackTrace();
						}
						break;
					}
				}
			}
		});
		t.setName("Jexxus-TCPConnectionListener");
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
						String senderIP = inputPacket.getAddress().getHostAddress();
						ServerConnection conn = udpClients.get(senderIP + inputPacket.getPort());
						if (conn == null) {
							conn = clients.get(senderIP);
						}
						if (conn == null) {
							System.err.println("Received UDP Packet from unknown source: " + senderIP);
						} else {
							if (ret.length == 0) {
								System.out.println("Set UDP Port: " + inputPacket.getPort());
								if (conn.getUDPPort() != -1) {
									// see if there is another connection without a UDP port set
									for (ServerConnection sc : clients.values()) {
										if (sc.getUDPPort() == -1) {
											conn = sc;
											break;
										}
									}
								}
								conn.setUDPPort(inputPacket.getPort());
								udpClients.put(senderIP + inputPacket.getPort(), conn);
							} else {
								listener.receive(ret, conn);
							}
						}
					} catch (IOException e) {
						if (running) {
							System.err.println("UDP Socket failed!");
							running = false;
						}
						break;
					}
				}
			}
		});
		t.start();
	}

	void connectionDied(ServerConnection conn, boolean forced) {
		synchronized (clients) {
			clients.remove(conn.getIP());
		}
		synchronized (udpClients) {
			clients.remove(conn.getIP() + conn.getUDPPort());
		}
		listener.connectionBroken(conn, forced);

	}

	void sendUDP(byte[] data, ServerConnection serverConnection) {
		synchronized (outgoingPacket) {
			outgoingPacket.setData(data);
			outgoingPacket.setAddress(serverConnection.getAddress());
			outgoingPacket.setPort(serverConnection.getUDPPort());
			try {
				udpSocket.send(outgoingPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * After the server has shut down, no new client connections can be established.
	 * 
	 * @param closeAllConnections
	 *            If this is true, all previously opened client connections will be closed.
	 */
	public void shutdown(boolean closeAllConnections) {
		running = false;
		try {
			tcpSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (udpSocket != null) {
			udpSocket.close();
		}
		synchronized (clients) {
			LinkedList<String> ips = new LinkedList<String>();
			for (String ip : clients.keySet()) {
				ips.add(ip);
			}
			for (String ip : ips) {
				ServerConnection sc = clients.get(ip);
				sc.exit();
			}
		}
	}

}
