package company.vk.edu.distrib.compute.artyompeshkov.urlshortener;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import company.vk.edu.distrib.compute.Dao;

import static company.vk.edu.distrib.compute.artyompeshkov.urlshortener.HttpUtils.sendMethodNotAllowed;
import static company.vk.edu.distrib.compute.artyompeshkov.urlshortener.HttpUtils.sendRedirect;

class RedirectHandler extends BaseHandler {
    static final String PATH = "/";
    private final Dao<String> links;

    RedirectHandler(Dao<String> links) {
        super();
        this.links = links;
    }

    @Override
    protected void doHandle(HttpExchange exchange) throws IOException {
        if (!GET.equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange, GET);
            return;
        }
        LinkId id = new LinkId(exchange.getRequestURI().getPath().substring(1));
        sendRedirect(exchange, links.get(id.value()));
    }
}
