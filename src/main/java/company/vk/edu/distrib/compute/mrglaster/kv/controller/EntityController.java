package company.vk.edu.distrib.compute.mrglaster.kv.controller;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.annotation.Route;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.enums.StatusCode;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.network.NetworkInteractable;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

public class EntityController implements NetworkInteractable {

    private final Dao<byte[]> dao;

    public EntityController(Dao<byte[]> dao) {
        this.dao = dao;
    }

    @Route(method = "GET", path = "/v0/entity", requiresAuthorization = false)
    public void getEntity(HttpExchange exchange, Map<String, String> pathParams) throws IOException {

        String id = pathParams.get("id");
        if (isInvalidId(id)) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_BAD_REQUEST);
            return;
        }
        try {
            byte[] value = dao.get(id);
            sendBytesResponse(exchange, value, StatusCode.HTTP_OK);
        } catch (NoSuchElementException e) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_NOT_FOUND);
        } catch (IllegalArgumentException | IOException e) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    @Route(method = "PUT", path = "/v0/entity", requiresAuthorization = false)
    public void upsertEntity(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        String id = pathParams.get("id");
        if (isInvalidId(id)) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_BAD_REQUEST);
            return;
        }
        byte[] body = exchange.getRequestBody().readAllBytes();
        try {
            dao.upsert(id, body);
            sendStatusCodeResponse(exchange, StatusCode.HTTP_CREATED);
        } catch (IllegalArgumentException | IOException e) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    @Route(method = "DELETE", path = "/v0/entity", requiresAuthorization = false)
    public void deleteEntity(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        String id = pathParams.get("id");
        if (isInvalidId(id)) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_BAD_REQUEST);
            return;
        }
        try {
            dao.delete(id);
            sendStatusCodeResponse(exchange, StatusCode.HTTP_ACCEPTED);
        } catch (IllegalArgumentException | IOException e) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isInvalidId(String id) {
        return id == null || id.isEmpty();
    }

}
