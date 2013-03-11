package jexxus.client;

/**
 * A class which implements this interface is able to use a client connection.
 * 
 * @author Jason
 * 
 */
public interface ClientConnectionListener {

	/**
	 * Every time data is sent to this client from the server, this method is
	 * called.
	 * 
	 * @param data
	 *            The data which was sent by the server.
	 */
	public void receive(byte[] data);

	/**
	 * Called when the connection is broken.
	 * 
	 * @param forced
	 *            True if this connection was intentionally broken by this
	 *            client.
	 */
	public void connectionBroken(boolean forced);

}
