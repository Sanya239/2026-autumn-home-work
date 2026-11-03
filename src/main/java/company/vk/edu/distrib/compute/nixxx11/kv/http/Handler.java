package company.vk.edu.distrib.compute.nixxx11.kv.http;

import java.io.IOException;
import java.util.function.Function;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

public class Handler implements HttpHandler {
  private static final Response BAD_METHOD_RESPONSE = new Response.Empty(HTTP_BAD_METHOD);
  private static final Response INTERNAL_ERROR_RESPONSE = new Response.Empty(HTTP_INTERNAL_ERROR);
  private static final SimpleHandler DEFAULT_HANDLER = (ignored1, ignored2) -> BAD_METHOD_RESPONSE;

  private SimpleHandler getHandler = DEFAULT_HANDLER;
  private SimpleHandler postHandler = DEFAULT_HANDLER;
  private SimpleHandler putHandler = DEFAULT_HANDLER;
  private SimpleHandler deleteHandler = DEFAULT_HANDLER;

  @Override
  public void handle(final HttpExchange exchange) throws IOException {
    try (exchange) {
      Response response;
      try {
        final byte[] body = exchange.getRequestBody().readAllBytes();
        response = switch (exchange.getRequestMethod()) {
          case "GET" -> getHandler.handle(exchange, body);
          case "POST" -> postHandler.handle(exchange, body);
          case "PUT" -> putHandler.handle(exchange, body);
          case "DELETE" -> deleteHandler.handle(exchange, body);
          default -> BAD_METHOD_RESPONSE;
        };
      } catch (final Exception e) {
        response = INTERNAL_ERROR_RESPONSE;
      }
      response.handle(exchange);
    }
  }

  public Handler setGetHandler(final SimpleHandler handler) {
    getHandler = handler;
    return this;
  }

  public Handler setPostHandler(final SimpleHandler handler) {
    postHandler = handler;
    return this;
  }

  public Handler setPutHandler(final SimpleHandler handler) {
    putHandler = handler;
    return this;
  }

  public Handler setDeleteHandler(final SimpleHandler handler) {
    deleteHandler = handler;
    return this;
  }

  public Handler wrap(final Function<SimpleHandler, SimpleHandler> wrapper) {
    getHandler = wrapper.apply(getHandler);
    postHandler = wrapper.apply(postHandler);
    putHandler = wrapper.apply(putHandler);
    deleteHandler = wrapper.apply(deleteHandler);
    return this;
  }
}
