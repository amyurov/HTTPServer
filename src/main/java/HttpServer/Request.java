package HttpServer;

import org.apache.hc.core5.http.NameValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Request {

    private final String method;
    private final String path;
    private final List<String> headers;
    private final String body;
    private final List<NameValuePair> query;
    private final List<NameValuePair> post;

    public Request(String method, String path, List<String> headers, String body, List<NameValuePair> query, List<NameValuePair> post) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.query = query;
        this.post = post;
    }

    public Request(String method, String path, List<String> headers, List<NameValuePair> query, List<NameValuePair> post) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.post = post;
        this.body = null;
        this.query = query;
    }

    public Request(String method, String path, List<String> headers, List<NameValuePair> query) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.post = new ArrayList<>();
        this.body = null;
        this.query = query;
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

    public List<NameValuePair> getQueryParam(String name) {
        return query.stream()
                .filter(q -> q.getName().equals(name))
                .collect(Collectors.toList());
    }

    public List<NameValuePair> getQueryParams() {
        return query;
    }

    public List<NameValuePair> getPostParam(String name) {
        return post.stream()
                .filter(p -> p.getName().equals(name))
                .collect(Collectors.toList());
    }

    public List<NameValuePair> getPostParams() {
        return post;
    }

    @Override
    public String toString() {
        return String.format("Метод запроса: %s%n" +
                "Путь: %s%n" +
                "Query: %s%n" +
                "Заголовки: %s%n" +
                "Тело запроса: %s%n" +
                "POST parameters: %s%n", method, path, query, headers.toString(), body, post);
    }
}
