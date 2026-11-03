package company.vk.edu.distrib.compute.mrglaster.urlshortener.exception;

public class DataSavingException extends RuntimeException {
    private static final long serialVersionUID = 2L;

    public DataSavingException(String message) {
        super(message);
    }

    public DataSavingException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataSavingException(Throwable cause) {
        super(cause);
    }
}
