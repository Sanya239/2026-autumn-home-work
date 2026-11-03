package company.vk.edu.distrib.compute.nosorozhek.urlshortener.validation;

public final class InvalidLinkIdException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public InvalidLinkIdException(String id) {
        super("Invalid link ID: " + id);
    }
}
