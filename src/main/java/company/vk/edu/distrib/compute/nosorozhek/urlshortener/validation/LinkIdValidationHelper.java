package company.vk.edu.distrib.compute.nosorozhek.urlshortener.validation;

import java.util.regex.Pattern;

public final class LinkIdValidationHelper {
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9]{10}");

    private LinkIdValidationHelper() {
    }

    public static void validate(String id) throws InvalidLinkException {
        if (id == null || !VALID_ID.matcher(id).matches()) {
            throw new InvalidLinkIdException(id);
        }
    }
}
