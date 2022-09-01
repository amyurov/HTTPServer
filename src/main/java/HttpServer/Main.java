package HttpServer;

import HttpServer.HttpServer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        int port = 9999;
        int threadCount = 64;

        final var server = new HttpServer(threadCount);
        // код инициализации сервера (из вашего предыдущего ДЗ)

        // добавление handler'ов (обработчиков)
        server.addHandler("GET", "/messages", new Handler() {
            @Override
            public void handle( Request request, BufferedOutputStream responseStream) {
                try {
                    responseStream.write(("HTTP/1.1 200 OK\r\n" +
                            "Content-type: " + 0 + "\r\n" +
                            "Content-Length: " + 0 + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n").getBytes());
                    responseStream.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        server.addHandler("POST", "/messages", (request, responseStream) -> {
            try {
                responseStream.write(("HTTP/1.1 202 OK\r\n" +
                        "Content-type: " + 0 + "\r\n" +
                        "Content-Length: " + 0 + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n").getBytes());
                responseStream.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        server.listen(port);
    }
}


