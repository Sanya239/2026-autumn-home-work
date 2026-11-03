package company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.route.external;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.annotation.Route;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.enums.StatusCode;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.network.NetworkInteractable;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.service.ShortLinksGeneratorService;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class LinkController implements NetworkInteractable {

    private final ShortLinksGeneratorService shortLinksGeneratorService;
    private final Supplier<Dao<String>> urlDaoSupplier;

    public LinkController(Supplier<Dao<String>> urlDaoSupplier, String baseUrl) {
        this.urlDaoSupplier = urlDaoSupplier;
        this.shortLinksGeneratorService = new ShortLinksGeneratorService(baseUrl);
    }

    @Route(method = "GET", path = "/v0/links/{id}", requiresAuthorization = true)
    public void getFullUrl(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        String id = pathParams.get("id");
        Dao<String> urlDao = urlDaoSupplier.get();
        try {
            if (isInvalidId(id)) {
                sendStatusCodeResponse(exchange, StatusCode.HTTP_UNPROCESSABLE_ENTITY);
                return;
            }
            String longUrl = urlDao.get(id);
            sendStringResponse(exchange, longUrl, StatusCode.HTTP_OK);
        } catch (NoSuchElementException e) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_NOT_FOUND);
        } catch (IllegalArgumentException | IOException e) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    @Route(method = "GET", path = "/{id}", requiresAuthorization = false)
    public void redirectLink(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        String id = pathParams.get("id");
        Dao<String> urlDao = urlDaoSupplier.get();
        try {
            if (isInvalidId(id)) {
                sendStatusCodeResponse(exchange, StatusCode.HTTP_UNPROCESSABLE_ENTITY);
                return;
            }
            String longUrl = urlDao.get(id);
            exchange.getResponseHeaders().set("Location", longUrl);
            exchange.sendResponseHeaders(StatusCode.HTTP_MOVED_PERMANENTLY.getStatusCode(), -1);
            exchange.close();
        } catch (NoSuchElementException e) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_NOT_FOUND);
        } catch (IllegalArgumentException | IOException e) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    @Route(method = "POST", path = "/v0/links", requiresAuthorization = true)
    public void createLink(HttpExchange exchange) throws IOException, NoSuchAlgorithmException {
        String longUrl = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (longUrl.isEmpty() || isInvalidUrl(longUrl)) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_UNPROCESSABLE_ENTITY);
            return;
        }
        String shortId = shortLinksGeneratorService.generateLinkID(longUrl);
        String shortUrl = shortLinksGeneratorService.generateShortURL(shortId);
        Dao<String> urlDao = urlDaoSupplier.get();
        urlDao.upsert(shortId, longUrl);

        sendStringResponse(exchange, shortUrl, StatusCode.HTTP_CREATED);
    }

    @Route(method = "PUT", path = "/v0/links/{id}", requiresAuthorization = true)
    public void updateLink(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        String id = pathParams.get("id");
        try {
            String newLongUrl = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (isInvalidUrl(newLongUrl) || isInvalidId(id)) {
                sendStatusCodeResponse(exchange, StatusCode.HTTP_UNPROCESSABLE_ENTITY);
                return;
            }
            Dao<String> urlDao = urlDaoSupplier.get();
            urlDao.get(id);
            urlDao.upsert(id, newLongUrl);
            this.sendStringResponse(exchange, "OK", StatusCode.HTTP_OK);
        } catch (NoSuchElementException e) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_NOT_FOUND);
        } catch (IllegalArgumentException | IOException e) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    @Route(method = "DELETE", path = "/v0/links/{id}", requiresAuthorization = true)
    public void deleteLink(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        String id = pathParams.get("id");
        if (isInvalidId(id)) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_UNPROCESSABLE_ENTITY);
            return;
        }
        Dao<String> urlDao = urlDaoSupplier.get();
        urlDao.delete(id);
        sendStatusCodeResponse(exchange, StatusCode.HTTP_ACCEPTED);
    }

    public boolean isInvalidUrl(String urlString) {
        try {
            String url = URI.create(urlString).toURL().toString();
            return url.isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isInvalidId(String id) {
        return id == null || !id.matches("[0-9a-zA-Z]{10}");
    }
}
