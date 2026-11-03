package company.vk.edu.distrib.compute.tomovalex.urlshortener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.charset.StandardCharsets;

final class RequestBodyUtils {
    private static final int MAX_SIZE = 8 * 1024;

    private RequestBodyUtils() {
    }

    static String read(InputStream input) throws IOException {
        byte[] body = input.readNBytes(MAX_SIZE + 1);
        if (body.length > MAX_SIZE) {
            throw new BufferOverflowException();
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}
