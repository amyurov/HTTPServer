package HttpServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class HttpServer implements Server {

    private final static List<String> validPath = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css",
            "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private final static List<String> allowedMethods = List.of("GET", "POST");
    // Хранение хендлеров
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> allHandlers = new ConcurrentHashMap<>();

    private final int threadsCount;
    private final ExecutorService threadPool;


    public HttpServer(int threadsCount) {
        this.threadsCount = threadsCount;
        threadPool = Executors.newFixedThreadPool(threadsCount);
    }

    @Override
    public void listen(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                System.out.println("Server is listening");
                Socket socket = serverSocket.accept();
                threadPool.execute(() -> newHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Метод добавления хендлера
    public void addHandler(String method, String path, Handler handler) {
        if (!allHandlers.containsKey(method)) {
            var methodMap = new ConcurrentHashMap<String, Handler>();
            allHandlers.put(method, methodMap);
        }
        allHandlers.get(method).put(path, handler);
    }

    // Парсинг запроса в объект Request
    private Request requestParse(BufferedReader in) throws IOException {
        var requestLine = in.readLine().split("\\s+");
        var method = requestLine[0];
        var path = requestLine[1];

        var headers = new ArrayList<String>();
        var line = "";
        while (!(line = in.readLine()).isEmpty()) {
            headers.add(line);
        }

        var body = in.lines().collect(Collectors.joining());
        if (body.isEmpty()) {
            return new Request(method, path, headers);
        }
        return new Request(method, path, headers, body);
    }

    // Метод для тредпула
    private void newHandler(Socket socket) {
        try {
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var request = requestParse(in);
            System.out.println(Thread.currentThread().getName() + "Начал обработку " + request.getMethod() + request.getPath());
            var out = new BufferedOutputStream(socket.getOutputStream());
            if (request.getMethod() == null) {
                badRequest(out);
                return;
            }

            if (request.getPath() == null) {
                notFound(out);
                return;
            }

            var handlersMap = allHandlers.get(request.getMethod());
            var handler = handlersMap.get(request.getPath());
            handler.handle(request, out);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void badRequest(BufferedOutputStream out) throws IOException {
        try (out) {
            out.write(("HTTP/1.1 400 Bad Request\r\n" + // Формируем response status line
                    "Content-lenght: 0\r\n" + // Заголовки. Длина тела 0, статус подключения закрыто
                    "Connection: close\r\n" +
                    "\r\n").getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notFound(BufferedOutputStream out) {
        try (out) {
            out.write(("HTTP/1.1 404 Not Found\r\n" + // Формируем response status line
                    "Content-lenght: 0\r\n" + // Заголовки. Длина тела 0, статус подключения закрыто
                    "Connection: close\r\n" +
                    "\r\n").getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void oldHandler(Socket socket) {
        try (var out = new BufferedOutputStream(socket.getOutputStream());
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             socket) {

            final var requestLine = in.readLine(); // Получаем входящую строку
            final var parts = requestLine.split("\\s+"); // Делим ее по пробелам

            System.out.printf("Поток %s обрабатывает запрос %n, %s%n", Thread.currentThread().getName(), requestLine);

            if (parts.length != 3) {
                return;
            }

            final var path = parts[1]; // Проверяем 2й элемент (URL) существует ли у нас такой ресурс, если нет то 404
            if (!validPath.contains(path)) {
                out.write(("HTTP/1.1 404 Not Found\r\n" + // Формируем response status line
                        "Content-lenght: 0\r\n" + // Заголовки. Длина тела 0, статус подключения закрыто
                        "Connection: close\r\n" +
                        "\r\n").getBytes());
                out.flush();
                System.out.printf("Поток %s отправил ответ %n", Thread.currentThread().getName());
                return;
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
                System.out.printf("Поток %s отправил ответ %n", Thread.currentThread().getName());
                return;
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
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
