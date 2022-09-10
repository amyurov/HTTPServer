package HttpServer;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;

public class RequestParser {


    public RequestParser() {
    }

    public Request parse(BufferedInputStream in, int bufferLimit) throws IOException, URISyntaxException {

        in.mark(bufferLimit);//Устанавливаем метку вначале потока и ограничиваем кол-во байтов на чтение (пока марка валидна)
        final var buffer = new byte[bufferLimit]; // Инициализируем массив байт в котором будем хранить пришедшие данные (буфер)
        final var readed = in.read(buffer);

        // Ищем индекс где заканчивается requestLine
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, readed);

        // Читаем requestLine
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split("\\s+");
        final var method = requestLine[0];
        final var fullPath = requestLine[1];
        final var path = fullPath.substring(0, fullPath.contains("?") ? fullPath.indexOf('?') : fullPath.indexOf('.'));

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

        // Если метод не GET, то начинаем работу с телом запроса
        in.skip(headersDelimiter.length);

        // Высчитываем content-length и content-type чтобы прочитать body
        final var contentLength = extractHeader(headers, "Content-Length");
        final var length = Integer.parseInt(contentLength.get());
        final var bodyBytes = in.readNBytes(length);

        // Content-Type может придти с boundary, нужно их отделить для switch - case
        final var contentTypeHeader = extractHeader(headers, "Content-Type");
        final var contentTypeArr = contentTypeHeader.get().split(";");
        final var contentType = contentTypeArr[0];

        switch (contentType) {
            case "application/x-www-form-urlencoded":
                final var body = URLDecoder.decode(new String(bodyBytes), Charset.defaultCharset());
                final var postParams = new ArrayList<>(stringToNameValue(body));
                return new Request(method, path, headers, query, postParams, null);

            case "multipart/form-data":
                DiskFileItemFactory factory = new DiskFileItemFactory();
                FileUpload upload = new FileUpload(factory);
                upload.setSizeMax(1_048_576); // Устанавливаем максимальный размер для передаваемых файлов
                try {

                    //Чтобы не заморачиваться с servlets создаем класс BodyRequestContext реализующий интерфейс RequestContext
                    // второй аргумент это заголовок "Content-Type" полностью с boundary
                    List<FileItem> parts = upload.parseRequest(new BodyRequestContext(bodyBytes, contentTypeHeader.get(), length));

                    for (FileItem item : parts) {
                        if (!item.isFormField()) { // Если не поле формы, значит файл. Записываем в корневую папку
                            if (item.getName().isEmpty()) continue;
                            File uploadFile = new File(item.getName());
                            item.write(uploadFile);
                        }
                    }

                    return new Request(method, path, headers, query, parts);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            default:
                break;
        }
        return new Request(method, path, headers, query);
    }

    // String to List<NameValuePair>
    public static List<NameValuePair> stringToNameValue(String string) {
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

    public static int indexOf(byte[] array, byte[] target, int start, int max) {
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
