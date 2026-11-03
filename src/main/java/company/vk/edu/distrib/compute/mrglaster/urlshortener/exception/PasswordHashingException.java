package company.vk.edu.distrib.compute.mrglaster.urlshortener.exception;

public class PasswordHashingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PasswordHashingException(String message) {
        super(message);
    }

    public PasswordHashingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PasswordHashingException(Throwable cause) {
        super(cause);
    }
}
