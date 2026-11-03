package company.vk.edu.distrib.compute.mrglaster.kv.controller;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.annotation.Route;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.enums.StatusCode;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.network.NetworkInteractable;

import java.io.IOException;
import java.util.Map;

public class KVStatusController implements NetworkInteractable {

    @Route(method = "GET", path = "/v0/status", requiresAuthorization = false)
    public void getStatus(HttpExchange exchange, Map<String, String> queryParams) throws IOException {
        sendStatusCodeResponse(exchange, StatusCode.HTTP_OK);
    }
}
