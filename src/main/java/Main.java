import HttpServer.*;

import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        int port = 9999;
        int threadCount = 64;
        final var server = new HttpServer(threadCount);

        // добавление handler'ов (обработчиков)
        server.addHandler("GET", "/messages", (request, out) -> {
            try {
                HttpServer.ok(out);
                System.out.println(Thread.currentThread().getName() + " Ответил на " + request.getMethod() + request.getPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        server.addHandler("POST", "/messages", (request, out) -> {
            try {
                HttpServer.ok(out);
                System.out.println(Thread.currentThread().getName() + " Ответил на " + request.getMethod() + request.getPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        server.addHandler("GET", "/favicon", (request, out) -> {
            try {
                HttpServer.ok(out);
                System.out.println(Thread.currentThread().getName() + " Ответил на " + request.getMethod() + request.getPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // Запускаем сервер
        server.listen(port);
    }
}


