package company.vk.edu.distrib.compute.nixxx11.kv.http;

import java.io.IOException;
import java.util.Random;

import com.sun.net.httpserver.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogWrapper implements SimpleHandler {
  private static final Logger LOG = LoggerFactory.getLogger(LogWrapper.class);
  private static final Random RANDOM = new Random();

  private final SimpleHandler handler;

  public LogWrapper(final SimpleHandler handler) {
    this.handler = handler;
  }

  @Override
  public Response handle(final Request request, final byte[] content) throws IOException {
    final int requestId = RANDOM.nextInt();
    final String method = request.getRequestMethod();
    final String path = request.getRequestURI().getPath();
    LOG.info("Got request '{} {}' (id={})", method, path, requestId);
    final Response response;
    try {
      response = handler.handle(request, content);
    } catch (final Exception e) {
      LOG.error("Error while handling request", e);
      throw e;
    }
    final int status = response.status();
    LOG.info("Finished request with status {} (id={})", status, requestId);
    return response;
  }
}
