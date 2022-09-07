package HttpServer;

import org.apache.hc.client5.http.utils.URIUtils;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RequestParser {

    private final List<String> validPath;
    private final List<String> allowedMethods;
    private final int bufferLimit;

    public RequestParser(List<String> validPath, List<String> allowedMethods, int bufferLimit) {
        this.validPath = validPath;
        this.allowedMethods = allowedMethods;
        this.bufferLimit = bufferLimit;
    }

    public Request parse(BufferedInputStream in) throws IOException, URISyntaxException {

        final var limit = bufferLimit;

        in.mark(limit);//Устанавливаем метку вначале потока и ограничиваем кол-во байтов на чтение (пока марка валидна)
        final var buffer = new byte[limit]; // Инициализируем массив байт в котором будем хранить пришедшие данные (буфер)
        final var readed = in.read(buffer);

        // Ищем индекс где заканчивается requestLine
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, readed);
        if (requestLineEnd == -1) {
            return null;
        }

        // Читаем requestLine
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split("\\s+");
        if (requestLine.length != 3) {
            return null;
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            return null;
        }

        final var fullPath = requestLine[1];
        final var path = fullPath.substring(0, fullPath.contains("?") ? fullPath.indexOf('?') : fullPath.indexOf('.'));
        if (!path.startsWith("/")) {
            return null;
        }

        // Парсим query
        var uriBuilder = new URIBuilder(fullPath, Charset.defaultCharset());
        var query = uriBuilder.getQueryParams();

        // Ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, readed);

        in.reset();
        in.skip(headersStart);

        final var headersByte = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersByte).split("\r\n"));

        // body
        // no body for GET
        if (method.equals("GET")) {
            return new Request(method, path, headers, query);
        }

        // Если метод не GET, то метод начинает работу с телом запроса
        in.skip(headersDelimiter.length);
        // Высчитываем content-length, чтобы прочитать body
        final var contentLength = extractHeader(headers, "Content-Length");
        final var contentType = extractHeader(headers, "Content-Type");

        if (contentLength.isEmpty() && contentType.isEmpty()) {
            return new Request(method, path, headers, query);
        }

        final var length = Integer.parseInt(contentLength.get());
        final var bodyBytes = in.readNBytes(length);
        final var body = URLDecoder.decode(new String(bodyBytes), Charset.defaultCharset());
        final var postParams = parseParams(body);
        return new Request(method, path, headers, body, query, postParams);
    }

    // String to List<NameValuePair>
    private static List<NameValuePair> parseParams(String string) {
        var paramList = new ArrayList<NameValuePair>();
        var array = string.split("&");

        for (String param : array) {
            var keyVal = param.split("=");
            paramList.add(new BasicNameValuePair(keyVal[0], keyVal[1]));
        }
        return paramList;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i <= max - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

}
