package nl.arba.integration.utils;

import jakarta.servlet.http.HttpServletResponse;
import nl.arba.integration.model.HttpResponse;

import java.io.IOException;
import java.io.OutputStream;

public class HttpUtils {
    public static void sendHtml(byte[] html, HttpServletResponse response) throws IOException {
        response.setStatus(200);
        response.setContentType("text/html");
        response.setContentLength(html.length);
        sendBytes(html, response);
    }

    public static void sendHtml(String html, HttpServletResponse response) throws IOException {
        sendHtml(html.getBytes(), response);
    }

    private static void sendBytes(byte[] bytes, HttpServletResponse response) throws IOException {
        OutputStream os = response.getOutputStream();
        os.write(bytes);
        os.flush();
        os.close();
    }

    public static void sendData(String data, HttpServletResponse response) throws IOException {
        response.setStatus(200);
        response.setContentLength(data.length());
        sendBytes(data.getBytes(), response);
    }

    public static void notFound(HttpServletResponse response) throws IOException {
        response.setStatus(404);
        response.setContentLength(0);
        sendBytes(new byte[0], response);
    }

    public static void forbidden(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentLength(0);
        sendBytes(new byte[0], response);
    }

    public static void failure(HttpServletResponse response) throws IOException {
        response.setStatus(500);
        response.setContentLength(0);
        sendBytes(new byte[0], response);
    }

    public static void sendResponse(HttpResponse response, HttpServletResponse httpresponse) throws IOException {
        httpresponse.setStatus(response.getCode());
        httpresponse.setContentLength(response.getContent().length);
        httpresponse.setContentType(response.getContentType());
        sendBytes(response.getContent(), httpresponse);
    }

}
