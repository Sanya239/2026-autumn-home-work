package company.vk.edu.distrib.compute.nosorozhek.urlshortener.authentication;

public class UnauthorizedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedException() {
        super();
    }

    public UnauthorizedException(Exception e) {
        super(e);
    }
}
