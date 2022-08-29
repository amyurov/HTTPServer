package HttpServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer implements Server {

    private static List<String> validPath = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css",
            "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private static ExecutorService threadPool = Executors.newFixedThreadPool(64);

    private int port;

    public HttpServer(int port) {
        this.port = port;
    }

    @Override
    public void start() {

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                System.out.println("Server is listening");
                Socket socket = serverSocket.accept();
                threadPool.execute(new Thread(() -> handle(socket)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handle(Socket socket) {
        try (var out = new BufferedOutputStream(socket.getOutputStream());
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            final var requestLine = in.readLine(); // Получаем входящую строку
            final var parts = requestLine.split("\\s+"); // Делим ее по пробелам

            System.out.printf("Поток %s обрабатывает запрос %n, %s%n", Thread.currentThread().getName(), requestLine);
            Thread.sleep(5000);

            if (parts.length != 3) {
                //just close socket
            }

            final var path = parts[1]; // Проверяем 2й элемент (URL) существует ли у нас такой ресурс, если нет то 404
            if (!validPath.contains(path)) {
                out.write(("HTTP/1.1 404 Not Found\r\n" + // Формируем response status line
                        "Content-lenght: 0\r\n" + // Заголовки. Длина тела 0, статус подключения закрыто
                        "Connection: close\r\n" +
                        "\r\n").getBytes());
                out.flush();
            }

            // Если данный ресурс существует:
            final var filePath = Path.of(".", "public", path); // Получаем путь до файла на винте .public + /path
            final var mimeType = Files.probeContentType(filePath); // Определяем тип файла

            // special case for classic или добавим интерактива
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath); // Читаем файл в виде строки
                final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes(); // Заменяем в нем якорь {time} на то что нам нужно и переводим в байты
                out.write(("HTTP/1.1 200 OK\r\n" +
                        "Content-type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n").getBytes());
                out.write(content);
                out.flush();
            }

            final var length = Files.size(filePath); // Получаем размер файла в байтах, для передачи в Content-Length
            out.write(("HTTP/1.1 200 OK\r\n" +
                    "Content-type: " + mimeType + "\r\n" +
                    "Content-Length: " + length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n").getBytes());
            Files.copy(filePath, out); // Копируем файл в выходной поток (отправляем клиенту)
            out.flush();
            System.out.printf("Поток %s отправил ответ %n", Thread.currentThread().getName());
            socket.close();
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

}
