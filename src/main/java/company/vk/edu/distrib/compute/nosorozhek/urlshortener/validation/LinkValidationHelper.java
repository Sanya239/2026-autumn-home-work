package company.vk.edu.distrib.compute.nosorozhek.urlshortener.validation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public final class LinkValidationHelper {
    private LinkValidationHelper() {
    }

    public static void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidLinkException(value);
        }

        try {
            URI uri = new URI(value);

            if (!uri.isAbsolute()
                    || uri.getHost() == null
                    || !Set.of("http", "https").contains(uri.getScheme())) {
                throw new InvalidLinkException(value);
            }
        } catch (URISyntaxException e) {
            throw new InvalidLinkException(value, e);
        }
    }
}
