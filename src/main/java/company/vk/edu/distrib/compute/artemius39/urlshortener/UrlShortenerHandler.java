package company.vk.edu.distrib.compute.artemius39.urlshortener;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class UrlShortenerHandler {
    private static final String ALPHABET = IntStream.concat(
            IntStream.concat(
                IntStream.rangeClosed('0', '9'),
                IntStream.rangeClosed('a', 'z')
            ),
            IntStream.rangeClosed('A', 'Z')
        ).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
    private static final int SHORT_URL_LENGTH = 10;
    private static final String ROOT_URI = "/";

    private final Dao<String> linkDao;
    private final Dao<String> authDao;
    private final SecureRandom random;

    public UrlShortenerHandler(Dao<String> linkDao, Dao<String> authDao, SecureRandom random) {
        this.linkDao = linkDao;
        this.authDao = authDao;
        this.random = random;
    }

    public void registerUser(HttpExchange exchange) throws IOException {
        String requestString = HttpUtils.getRequestBodyAsString(exchange);
        String[] split = requestString.split(":");
        if (split.length != HttpUtils.AUTH_FIELD_COUNT) {
            HttpUtils.unprocessableEntity(exchange);
            return;
        }
        String username = split[0];
        String password = split[1];
        authDao.upsert(username, password);
        exchange.sendResponseHeaders(200, -1);
    }

    public void getStatus(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(200, -1);
    }

    public void getLink(HttpExchange httpExchange) throws IOException {
        if (!checkAuth(httpExchange)) {
            return;
        }
        String id = HttpUtils.getPathParam(httpExchange, UrlShortenerController.LINKS_URI);
        if (!isValidShortLink(httpExchange, id)) {
            return;
        }
        try {
            String longLink = linkDao.get(id);
            HttpUtils.sendResponse(httpExchange, 200, longLink);
        } catch (NoSuchElementException e) {
            HttpUtils.notFound(httpExchange);
        } catch (IllegalArgumentException e) {
            HttpUtils.unprocessableEntity(httpExchange);
        }
    }

    public void createLink(HttpExchange exchange) throws IOException {
        if (!checkAuth(exchange)) {
            return;
        }
        String longLink = HttpUtils.getRequestBodyAsString(exchange);
        if (!isValidLongLink(exchange, longLink)) {
            return;
        }
        String shortLink = generateShortUrl();
        linkDao.upsert(shortLink, longLink);
        String fullLink = getFullLink(exchange, shortLink);
        HttpUtils.sendResponse(exchange, 201, fullLink);
    }

    public void changeLink(HttpExchange exchange) throws IOException {
        if (!checkAuth(exchange)) {
            return;
        }
        String linkToChange = HttpUtils.getPathParam(exchange, UrlShortenerController.LINKS_URI);
        if (!isValidShortLink(exchange, linkToChange)) {
            return;
        }
        String newLink = HttpUtils.getRequestBodyAsString(exchange);
        if (!isValidLongLink(exchange, newLink)) {
            return;
        }
        try {
            linkDao.get(linkToChange);
            linkDao.upsert(linkToChange, newLink);
            exchange.sendResponseHeaders(200, -1);
        } catch (NoSuchElementException e) {
            HttpUtils.notFound(exchange);
        } catch (IllegalArgumentException e) {
            HttpUtils.unprocessableEntity(exchange);
        }
    }

    public void deleteLink(HttpExchange exchange) throws IOException {
        if (!checkAuth(exchange)) {
            return;
        }
        String linkToDelete = HttpUtils.getPathParam(exchange, UrlShortenerController.LINKS_URI);
        if (!isValidShortLink(exchange, linkToDelete)) {
            return;
        }
        linkDao.delete(linkToDelete);
        exchange.sendResponseHeaders(202, -1);
    }

    public void redirectLink(HttpExchange exchange) throws IOException {
        String shortLink = HttpUtils.getPathParam(exchange, ROOT_URI);
        if (!isValidShortLink(exchange, shortLink)) {
            return;
        }
        try {
            String longLink = linkDao.get(shortLink);
            exchange.getResponseHeaders().set("Location", longLink);
            exchange.sendResponseHeaders(301, -1);
        } catch (NoSuchElementException e) {
            HttpUtils.notFound(exchange);
        } catch (IllegalArgumentException e) {
            HttpUtils.unprocessableEntity(exchange);
        }
    }

    private String getFullLink(HttpExchange exchange, String shortLink) {
        return "http://%s:%d/%s".formatted(
            exchange.getLocalAddress().getHostString(),
            exchange.getLocalAddress().getPort(),
            shortLink
        );
    }

    private boolean checkAuth(HttpExchange httpExchange) throws IOException {
        Credentials credentials = HttpUtils.parseAuth(httpExchange);
        if (credentials != null) {
            try {
                String expectedPassword = authDao.get(credentials.username());
                if (credentials.password().equals(expectedPassword)) {
                    return true;
                }
            } catch (NoSuchElementException | IllegalArgumentException | IOException e) {
                HttpUtils.unauthorized(httpExchange);
                return false;
            }
        }
        HttpUtils.unauthorized(httpExchange);
        return false;
    }

    private String generateShortUrl() throws IOException {
        StringBuilder sb = new StringBuilder(SHORT_URL_LENGTH);
        while (true) {
            for (int i = 0; i < SHORT_URL_LENGTH; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            String result = sb.toString();
            try {
                linkDao.get(result);
            } catch (NoSuchElementException e) {
                return result;
            }
            sb.delete(0, sb.length());
        }
    }

    private boolean isValidShortLink(HttpExchange exchange, String shortLink) throws IOException {
        if (shortLink.codePoints().anyMatch(ch -> ALPHABET.indexOf(ch) == -1)) {
            exchange.sendResponseHeaders(422, -1);
            return false;
        }
        return true;
    }

    private boolean isValidLongLink(HttpExchange exchange, String longLink) throws IOException {
        try {
            URI uri = URI.create(longLink);
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            HttpUtils.unprocessableEntity(exchange);
            return false;
        }
        HttpUtils.unprocessableEntity(exchange);
        return false;
    }

    public void close() throws IOException {
        linkDao.close();
        authDao.close();
    }
}
