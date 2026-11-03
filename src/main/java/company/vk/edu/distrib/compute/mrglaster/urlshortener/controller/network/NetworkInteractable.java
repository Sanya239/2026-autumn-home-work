package company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.network;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.enums.StatusCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public interface NetworkInteractable {

    default void sendStringResponse(HttpExchange exchange, String message, StatusCode statusCode) throws IOException {
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(statusCode.getStatusCode(), responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }

    default void sendStatusCodeResponse(HttpExchange exchange, StatusCode statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(statusCode.getStatusCode(), -1);
    }

    default void sendBytesResponse(HttpExchange exchange, byte[] body, StatusCode statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(statusCode.getStatusCode(), body.length);
        exchange.getResponseBody().write(body);
    }
}
