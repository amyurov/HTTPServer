package HttpServer;

import java.util.List;

public class Request {

    private final String method;
    private final String path;
    private final List<String> headers;
    private final String body;

    public Request(String method, String path, List<String> headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
    }

    public Request(String method, String path, List<String> headers) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = null;
    }

    public String getMethod() {
        return method;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return String.format("Метод запроса: %s%n" +
                "Путь: %s%n" +
                "Заголовки: %s%n" +
                "Тело запроса: %s%n", method, path, headers.toString(), body);
    }
}
