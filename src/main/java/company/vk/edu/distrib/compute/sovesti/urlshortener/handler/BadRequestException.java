package company.vk.edu.distrib.compute.sovesti.urlshortener.handler;

public final class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1235942363641479104L;

    public BadRequestException(String reason) {
        super(reason);
    }
}
