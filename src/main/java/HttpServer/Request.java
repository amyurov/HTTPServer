package HttpServer;

import org.apache.commons.fileupload.FileItem;
import org.apache.hc.core5.http.NameValuePair;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class Request {

    private final String method;
    private final String path;
    private final List<String> headers;
    private final List<NameValuePair> query;
    private final String body;
    private final List<NameValuePair> postParams;
    private final List<FileItem> parts;

    public Request(String method, String path, List<String> headers, List<NameValuePair> query) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.query = query;
        this.body = null;
        this.postParams = null;
        this.parts = null;
    }

    public Request(String method, String path, List<String> headers, List<NameValuePair> query, List<NameValuePair> postParams, List<FileItem> parts) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.query = query;
        this.body = "Body parsed in postParams";
        this.postParams = postParams;
        this.parts = parts;
    }

    public Request(String method, String path, List<String> headers, List<NameValuePair> query, List<FileItem> parts) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.query = query;
        this.body = "Body parsed in parts";
        this.postParams = null;
        this.parts = parts;
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

    // Геттеры для параметров в url. getQueryParam возвращает лист параметров с указанным именем поля
    public List<NameValuePair> getQueryParam(String name) {
        return query.stream()
                .filter(q -> q.getName().equals(name))
                .collect(Collectors.toList());
    }
    public List<NameValuePair> getQueryParams() {
        return query;
    }

    // Геттеры для параметров в теле запроса. getPostParam возвращает лист параметров с указанным именем поля
    public List<NameValuePair> getPostParam(String name) {
        return postParams.stream()
                .filter(p -> p.getName().equals(name))
                .collect(Collectors.toList());
    }
    public List<NameValuePair> getPostParams() {
        return postParams;
    }

    // Геттеры для параметров в теле запроса отправленных в multipart/form-data
    public List<FileItem> getParts() {
        return parts;
    }
    // getPart возвращает массив байт либо с файлом, либо со строкой типа имя=значение.
    // Эту строку можно распарсить статическим методом RequestParser.stringToNameValue
    public byte[] getPart(String name) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (FileItem part : parts) {
            if (part.getFieldName().equals(name)) {

                if (!part.isFormField()) {
                    return part.get(); // Returns the contents of the file item as an array of bytes.
                }

                if (part.isFormField()) {
                    sb.append(name + "=" + part.getString(Charset.defaultCharset().name()));
                    sb.append("&");
                }
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }


    @Override
    public String toString() {
        return String.format("Метод запроса: %s%n" +
                "Путь: %s%n" +
                "Query: %s%n" +
                "Заголовки: %s%n" +
                "Тело запроса: %s%n" +
                "POST parameters: %s%n" +
                "POST parts %s%n", method, path, query, headers, body, postParams, parts);
    }
}
