package nl.arba.integration.utils;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {
    public static String streamToString(InputStream stream) throws IOException {
        return new String(streamToBytes(stream));
    }

    public static byte[] streamToBytes(InputStream stream) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            IOUtils.copy(stream, bos);
            return bos.toByteArray();
        }
        finally {
            stream.close();
        }

    }

    public static InputStream objectToStream(Object source) {
        if (source instanceof byte[])
            return new ByteArrayInputStream((byte[]) source);
        else
            return null;
    }
}
