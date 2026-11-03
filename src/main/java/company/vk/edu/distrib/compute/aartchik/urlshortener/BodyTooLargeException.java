package company.vk.edu.distrib.compute.aartchik.urlshortener;

import java.io.IOException;

public final class BodyTooLargeException extends IOException {
    private static final long serialVersionUID = 1L;

    public BodyTooLargeException() {
        super("Request body exceeds 64 KiB");
    }
}
