import HttpServer.HttpServer;
import HttpServer.Server;

public class Main {
    public static void main(String[] args) {

        int serverPort = 8000;
        Server server = new HttpServer(serverPort);

        server.start();
    }
}
