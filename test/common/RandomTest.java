package common;

import jexxus.client.ClientConnection;
import jexxus.common.Connection;
import jexxus.common.ConnectionListener;
import jexxus.server.Server;
import jexxus.server.ServerConnection;

public class RandomTest implements ConnectionListener {

	private RandomTest() throws Exception {
		Server server = new Server(this, 35221, true);
		server.startServer();
		ClientConnection connection = new ClientConnection(this, "localhost", 35221, true);
		connection.connect();
	}

	public static void main(String[] args) throws Exception {
		new RandomTest();
	}

	@Override
	public void clientConnected(ServerConnection conn) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectionBroken(Connection broken, boolean forced) {
		// TODO Auto-generated method stub

	}

	@Override
	public void receive(byte[] data, Connection from) {
		// TODO Auto-generated method stub

	}

}
