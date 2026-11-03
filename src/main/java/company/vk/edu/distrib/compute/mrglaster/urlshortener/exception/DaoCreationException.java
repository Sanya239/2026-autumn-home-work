package company.vk.edu.distrib.compute.mrglaster.urlshortener.exception;

public class DaoCreationException extends RuntimeException {

    private static final long serialVersionUID = 3L;

    public DaoCreationException(String message) {
        super(message);
    }

    public DaoCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DaoCreationException(Throwable cause) {
        super(cause);
    }
}
