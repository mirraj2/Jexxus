package jexxus.client;

import jexxus.common.ConnectionListener;
import jexxus.server.ServerConnection;

/**
 * A class which implements this interface is able to use a client connection.
 */
public abstract class ClientConnectionListener implements ConnectionListener {

  public void clientConnected(ServerConnection conn) {}

}
