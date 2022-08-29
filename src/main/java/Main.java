import HttpServer.HttpServer;
import HttpServer.Server;

public class Main {
    public static void main(String[] args) {

        int serverPort = 8000;
        int threads = 64;
        Server server = new HttpServer(serverPort, threads);

        server.start();
    }
}
