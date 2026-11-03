package company.vk.edu.distrib.compute.nixxx11.urlshortener.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.HttpService;

public abstract class AbstractService implements HttpService {
  private final HttpServer httpServer;

  public AbstractService(final int port) throws IOException {
    this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
  }

  protected void init() {
    final Map<String, ? extends HttpHandler> handlers = getHandlers();
    handlers.forEach(httpServer::createContext);
  }

  protected abstract Map<String, ? extends HttpHandler> getHandlers();

  @Override
  public void start() {
    httpServer.start();
  }

  @Override
  public void stop() {
    httpServer.stop(1);
  }
}
