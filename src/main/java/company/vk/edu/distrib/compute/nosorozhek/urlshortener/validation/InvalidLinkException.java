package company.vk.edu.distrib.compute.nosorozhek.urlshortener.validation;

public final class InvalidLinkException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public InvalidLinkException(String link, Exception e) {
        super("Invalid link: " + link, e);
    }

    public InvalidLinkException(String link) {
        super("Invalid link: " + link);
    }
}
