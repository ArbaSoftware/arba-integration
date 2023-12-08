package nl.arba.integration.model;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private HashMap <String, String> uriParams = new HashMap<>();
    private HashMap <String, String> headers = new HashMap<>();
    private String url;
    private HttpMethod method;
    private byte[] postBody;
    private String contentType;

    public void setUriParam(String name, String value) {
        uriParams.put(name, value);
    }

    public boolean hasUriParam(String name) {
        return uriParams.containsKey(name);
    }

    public Map<String, String> getUriParams() {
        return uriParams;
    }

    public String getUriParam(String name) {
        return uriParams.get(name);
    }

    public void setUrl(String value) {
        url = value;
    }

    public String getUrl() {
        return url;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public static HttpRequest from(HttpServletRequest request) {
        HttpRequest result = new HttpRequest();
        result.setUrl(request.getRequestURI() + (request.getQueryString() == null || request.getQueryString().isEmpty() ? "" : "?"+ request.getQueryString()));
        result.setMethod(HttpMethod.fromString(request.getMethod()));
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            result.setHeader(header, request.getHeader(header));
        }
        return result;
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setPostBody(byte[] content) {
        postBody = content;
    }

    public byte[] getPostBody() {
        return postBody;
    }

    public void setContentType(String type) {
        contentType = type;
    }

    public String getContentType() {
        return contentType;
    }
}
