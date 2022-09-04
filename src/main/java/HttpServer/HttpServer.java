package HttpServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
               threadPool.execute(() -> connectionHandle(socket));
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
        var pathWithFormat = requestLine[1];
        var path = pathWithFormat.substring(0, pathWithFormat.indexOf("."));

        var headers = new ArrayList<String>();
        var line = "";
        while (!(line = in.readLine()).isEmpty()) {
            headers.add(line);
        }

        if (in.ready()) {
            var body = in.lines().collect(Collectors.joining());
            return new Request(method, path, headers, body);
        }

        return new Request(method, path, headers);
    }

    // Метод для тредпула
    private void connectionHandle(Socket socket) {
        try (socket;
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var out = new BufferedOutputStream(socket.getOutputStream())) {

            if(!in.ready()) {
                badRequest(out);
                return;
            }

            var request = requestParse(in);
            System.out.println(Thread.currentThread().getName() + " Начал обработку " + request.getMethod() + request.getPath());

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
            System.out.println(Thread.currentThread().getName() + " Закончил обработку " + request.getMethod() + request.getPath());
            Thread.currentThread().interrupt();
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
        }
    }

    private void notFound(BufferedOutputStream out) throws IOException {
        try (out) {
            out.write(("HTTP/1.1 404 Not Found\r\n" + // Формируем response status line
                    "Content-lenght: 0\r\n" + // Заголовки. Длина тела 0, статус подключения закрыто
                    "Connection: close\r\n" +
                    "\r\n").getBytes());
            out.flush();
        }
    }

}
