package jexxus;


class ServerClientTest {

  private ServerClientTest() throws Exception {
    Server server = new Server().compress().useSSL();
    server.onNewClient(client -> {
      client.onMessage(data -> {
        System.out.println("Server received: " + new String(data));
        System.out.println("Test passed!");
        System.exit(0);
      });
      client.send("Server says hi!".getBytes());
    });
    server.start();

    Client client = new Client("localhost").compress().useSSL();
    client.onMessage(data -> {
      System.out.println("Client received: " + new String(data));
      client.send("Client says hi!".getBytes());
    });
    client.connect();
  }

  public static void main(String[] args) throws Exception {
    new ServerClientTest();
  }

}
