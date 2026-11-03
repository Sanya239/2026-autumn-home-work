package company.vk.edu.distrib.compute.mperikov.urlshortener;

import java.net.URI;
import java.net.URISyntaxException;

final class RequestChecks {
    static final String GET = "GET";
    static final String POST = "POST";
    static final String STATUS_PATH = "/v0/status";
    static final String USERS_PATH = "/internal/users";
    static final String LINKS_PATH = "/v0/links";
    static final String LINKS_PREFIX = "/v0/links/";

    private RequestChecks() {
    }

    static boolean isPublic(String method, String path) {
        return (GET.equals(method) && STATUS_PATH.equals(path))
            || (POST.equals(method) && USERS_PATH.equals(path))
            || isRedirectPath(method, path);
    }

    static boolean isCredentialLine(String body, int separator) {
        return separator > 0
            && separator < body.length() - 1
            && body.indexOf('\n') < 0
            && body.indexOf('\r') < 0;
    }

    static boolean isValidLink(String link) {
        try {
            URI uri = new URI(link);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!uri.isAbsolute() || scheme == null || host == null || host.isEmpty()) {
                return false;
            }
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (URISyntaxException expected) {
            return false;
        }
    }

    static boolean isRedirectPath(String method, String path) {
        return GET.equals(method) && path.startsWith("/") && path.length() > 1 && path.indexOf('/', 1) < 0;
    }
}
