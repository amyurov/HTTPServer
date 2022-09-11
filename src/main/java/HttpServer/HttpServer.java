package HttpServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.*;

public class HttpServer implements Server {

//    // Тут не используется, но, в частном случае, думаю, должно
//    private final static List<String> validPath = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css",
//            "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js", "/messages.html");

    private final static List<String> allowedMethods = List.of("GET", "POST");

    private final static RequestParser requestParser = new RequestParser(allowedMethods, 4096);

    // Хранение хендлеров
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> allHandlers = new ConcurrentHashMap<>();

    private final ExecutorService threadPool;

    public HttpServer(int threadsCount) {
        threadPool = Executors.newFixedThreadPool(threadsCount);
    }

    @Override
    public void listen(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                System.out.println("Server is listening");
                Socket socket = serverSocket.accept();
                threadPool.execute(() -> connectionHandle(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Метод для тредпула
    private void connectionHandle(Socket socket) {
        try (socket;
             var in = new BufferedInputStream(socket.getInputStream());
             var out = new BufferedOutputStream(socket.getOutputStream())) {

            var request = requestParser.parse(in);
            if (request.getMethod() == null) {
                badRequest(out);
                return;
            }
            // Добавил использование геттеров query
            System.out.println(request.getQueryParams());
            System.out.println(request.getQueryParam("title"));

            System.out.println(request);

            var handlersMap = allHandlers.get(request.getMethod());
            var handler = handlersMap.get(request.getPath());
            if (handler == null) {
                out.write(("HTTP/1.1 200 OK\r\n" +
                        "Content-type: " + 0 + "\r\n" +
                        "Content-Length: " + 0 + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n").getBytes());
                out.flush();
                System.out.println(Thread.currentThread().getName() + " Ответил на " + request.getMethod() + request.getPath());
            } else {
                handler.handle(request, out);
            }

            System.out.println(Thread.currentThread().getName() + " Закончил обработку " + request.getMethod() + request.getPath());
            Thread.currentThread().interrupt();
        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace();
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

    private static void badRequest(BufferedOutputStream out) throws IOException {
        try (out) {
            out.write(("HTTP/1.1 400 Bad Request\r\n" + // Формируем response status line
                    "Content-lenght: 0\r\n" + // Заголовки. Длина тела 0, статус подключения закрыто
                    "Connection: close\r\n" +
                    "\r\n").getBytes());
            out.flush();
        }
    }

    private static void notFound(BufferedOutputStream out) throws IOException {
        try (out) {
            out.write(("HTTP/1.1 404 Not Found\r\n" + // Формируем response status line
                    "Content-lenght: 0\r\n" + // Заголовки. Длина тела 0, статус подключения закрыто
                    "Connection: close\r\n" +
                    "\r\n").getBytes());
            out.flush();
        }
    }

}
