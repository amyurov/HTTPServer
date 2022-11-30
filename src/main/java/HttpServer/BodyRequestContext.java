package HttpServer;

import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.UploadContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class BodyRequestContext implements RequestContext {

    private final byte[] bodyBytes;
    private final String contentType;
    private final int contentLength;

    public BodyRequestContext(byte[] bodyBytes, String contentType, int contentLength) {
        this.bodyBytes = bodyBytes;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

//    @Override
//    public long contentLength() {
//        return contentLength;
//    }

    @Override
    public String getCharacterEncoding() {
        return Charset.defaultCharset().name();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Deprecated
    @Override
    public int getContentLength() {
        return contentLength;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(bodyBytes);
    }
}
