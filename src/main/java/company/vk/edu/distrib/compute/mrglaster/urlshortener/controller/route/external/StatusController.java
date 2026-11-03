package company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.route.external;

import company.vk.edu.distrib.compute.mrglaster.urlshortener.annotation.Route;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.network.NetworkInteractable;

public class StatusController implements NetworkInteractable {
    @Route(method = "GET", path = "/v0/status", requiresAuthorization = false)
    public String getStatus() {
        return "UP";
    }
}
