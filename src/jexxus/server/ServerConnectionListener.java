package jexxus.server;

/**
 * A class which implements this interface are able to control the events given
 * off by the server.
 * 
 * @author Jason
 * 
 */
public interface ServerConnectionListener {

	/**
	 * Every time data is sent to the server, this method is called.
	 * 
	 * @param data
	 *            The data which was sent by the server.
	 * @param sender
	 *            The client who sent the data.
	 */
	public void receive(byte[] ret, ServerConnection sender);

	/**
	 * Called when a client connects to the server.
	 * 
	 * @param connected
	 *            The client who connected.
	 */
	public void clientConnected(ServerConnection connected);

	/**
	 * Called whenever a client gets disconnected from the server.
	 * 
	 * @param disconnected
	 *            The client who got disconnected.
	 * @param forced
	 *            True if the server forcefully disconnected the client.
	 */
	public void clientDisconnected(ServerConnection disconnected, boolean forced);

}
