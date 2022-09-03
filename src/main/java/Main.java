import HttpServer.*;

import java.io.BufferedOutputStream;
import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        int port = 9999;
        int threadCount = 64;
        final var server = new HttpServer(threadCount);

        // добавление handler'ов (обработчиков)
        server.addHandler("GET", "/messages.html", ((request, out) -> System.out.println("ОБРАБОТКА ЕПТА")));

        server.addHandler("POST", "/messages", ((request, out) -> {
            try {
                out.write(("HTTP/1.1 202 OK\r\n" +
                        "Content-type: " + 0 + "\r\n" +
                        "Content-Length: " + 0 + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n").getBytes());
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }));

        server.listen(port);
    }
}


