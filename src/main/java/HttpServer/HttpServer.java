package HttpServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class HttpServer implements Server {

    private final static List<String> validPath = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css",
            "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js", "/messages.html");

    private final static List<String> allowedMethods = List.of("GET", "POST");

    private final static RequestParser requestParser = new RequestParser(validPath, allowedMethods, 4096);

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

    // Метод для тредпула (обработка подключения)
    private void connectionHandle(Socket socket) {
        try (socket;
             var in = new BufferedInputStream(socket.getInputStream());
             var out = new BufferedOutputStream(socket.getOutputStream())) {

            if (!validateRequest(in, out)) {
                return;
            }

            var request = requestParser.parse(in);
            System.out.println(request);

            var handlersMap = allHandlers.get(request.getMethod());
            var handler = handlersMap.get(request.getPath());

            if (handler == null) {
                ok(out);
                System.out.println(Thread.currentThread().getName() + " Ответил на " + request.getMethod() + request.getPath());
            } else {
                handler.handle(request, out);
            }

            System.out.println(Thread.currentThread().getName() + " Закончил обработку " + request.getMethod() + request.getPath() + "\n");
            Thread.currentThread().interrupt();
        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
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

    // Проверка requestLine
    private static boolean validateRequest(BufferedInputStream in, BufferedOutputStream out) {
        try {
            final var limit = 2048;

            in.mark(limit);//Устанавливаем метку вначале потока и ограничиваем кол-во байтов на чтение (пока марка валидна)
            final var buffer = new byte[limit]; // Инициализируем массив байт в котором будем хранить пришедшие данные (буфер)
            final var readed = in.read(buffer);

            // Ищем индекс где заканчивается requestLine
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = RequestParser.indexOf(buffer, requestLineDelimiter, 0, readed);
            if (requestLineEnd == -1) {
                badRequest(out);
                return false;
            }

            // Читаем requestLine
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split("\\s+");
            if (requestLine.length != 3) {
                badRequest(out);
                return false;
            }

            final var method = requestLine[0];
            if (!allowedMethods.contains(method)) {
                badRequest(out);
                return false;
            }

            final var fullPath = requestLine[1];
            final var path = fullPath.substring(0, fullPath.contains("?") ? fullPath.indexOf('?') : fullPath.indexOf('.'));
            if (!path.startsWith("/")) {
                notFound(out);
                return false;
            }
            in.reset();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return true;
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

    private static void ok(BufferedOutputStream out) throws IOException {
        try (out) {
            out.write(("HTTP/1.1 200 OK\r\n" +
                    "Content-type: " + 0 + "\r\n" +
                    "Content-Length: " + 0 + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n").getBytes());
            out.flush();
        }
    }

}
