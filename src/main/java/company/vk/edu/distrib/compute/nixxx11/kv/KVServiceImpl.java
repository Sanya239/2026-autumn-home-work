package company.vk.edu.distrib.compute.nixxx11.kv;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Request;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.nixxx11.kv.http.AbstractService;
import company.vk.edu.distrib.compute.nixxx11.kv.http.Handler;
import company.vk.edu.distrib.compute.nixxx11.kv.http.LogWrapper;
import company.vk.edu.distrib.compute.nixxx11.kv.http.Response;
import org.jspecify.annotations.Nullable;

import static java.net.HttpURLConnection.*;

public class KVServiceImpl extends AbstractService implements KVService {
  private static final Response NO_ID = new Response.Basic(HTTP_BAD_REQUEST, "No id specified");

  private final Dao<byte[]> dao;

  public KVServiceImpl(
      final int port,
      final Dao<byte[]> dao
  ) throws IOException {
    super(port);
    this.dao = dao;
    init();
  }

  @Override
  protected Map<String, ? extends HttpHandler> getHandlers() {
    return Map.of(
        "/v0/status", new Handler()
            .setGetHandler(this::status)
            .wrap(LogWrapper::new),
        "/v0/entity", new Handler()
            .setGetHandler(this::get)
            .setPutHandler(this::put)
            .setDeleteHandler(this::delete)
            .wrap(LogWrapper::new)
    );
  }

  private Response status(final Request ignored1, final byte[] ignored2) {
    return new Response.Basic(HTTP_OK, "OK");
  }

  private Response get(final Request request, final byte[] ignored) throws IOException {
    final String id = getId(request);
    if (id == null) {
      return NO_ID;
    }

    final byte[] value;
    try {
      value = dao.get(id);
    } catch (final NoSuchElementException e) {
      return idNotFound(id);
    }

    return new Response.Binary(HTTP_OK, value);
  }

  private Response put(final Request request, final byte[] body) throws IOException {
    final String id = getId(request);
    if (id == null) {
      return NO_ID;
    }

    dao.upsert(id, body);

    return new Response.Empty(HTTP_CREATED);
  }

  private Response delete(final Request request, final byte[] ignored) throws IOException {
    final String id = getId(request);
    if (id == null) {
      return NO_ID;
    }

    dao.delete(id);

    return new Response.Empty(HTTP_ACCEPTED);
  }

  @Nullable
  private static String getId(final Request request) {
    final String query = request.getRequestURI().getQuery();
    if (query == null) {
      return null;
    }
    final Map<String, String> params = parseQuery(query);
    final String id = params.get("id");
    return id == null || id.isEmpty() ? null : id;
  }

  private static Map<String, String> parseQuery(final String query) {
    return Arrays.stream(query.split("&"))
        .map(param -> param.split("=", 2))
        .filter(a -> a.length == 2)
        .collect(Collectors.toUnmodifiableMap(a -> a[0], a -> a[1]));
  }

  private static Response idNotFound(final String id) {
    return new Response.Basic(HTTP_NOT_FOUND, "Id not found: " + id);
  }
}
