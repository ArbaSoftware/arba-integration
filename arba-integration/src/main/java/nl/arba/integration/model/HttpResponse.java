package nl.arba.integration.model;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.Header;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class HttpResponse {
    private byte[] content;
    private String contentType;
    private int responseCode;
    private HashMap<String,String> headers;

    public HttpResponse(Integer responsecode) {
        responseCode= responsecode;
        headers = new HashMap<>();
    }

    public int getCode() {
        return responseCode;
    }

    public static HttpResponse create(int code) {
        return new HttpResponse(code);
    }

    public void setContentType(String type) {
        contentType = type;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setContent(InputStream is) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            IOUtils.copy(is, bos);
            setContent(bos.toByteArray());
        }
        catch (Exception err) {
            setContent(new byte[0]);
        }
        finally {
            try {
                is.close();
            }
            catch (Exception err) {}
        }
    }

    public byte[] getContent() {
        return content;
    }

    public static HttpResponse from(CloseableHttpResponse input) throws IOException {
        HttpResponse result = HttpResponse.create(input.getCode());
        if (input.getEntity() != null) {
            result.setContentType(input.getEntity().getContentType());
            result.setContent(input.getEntity().getContent());
        }
        if (input.getHeaders() != null) {
            for (Header header: input.getHeaders()) {
                result.addHeader(header.getName(), header.getValue());
            }
        }
        return result;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public String[] getHeaderNames() {
        return headers.keySet().toArray(new String[0]);
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

}
