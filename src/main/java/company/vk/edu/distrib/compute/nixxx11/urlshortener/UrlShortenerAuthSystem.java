package company.vk.edu.distrib.compute.nixxx11.urlshortener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.Request;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.nixxx11.urlshortener.http.Response;
import company.vk.edu.distrib.compute.nixxx11.urlshortener.http.SimpleHandler;

import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

public class UrlShortenerAuthSystem {
  private static final Response UNAUTHORIZED_RESPONSE = new Response.Empty(HTTP_UNAUTHORIZED);
  private static final String BASIC_PREFIX = "Basic ";

  private final Dao<String> userDao;

  public UrlShortenerAuthSystem(final Dao<String> userDao) {
    this.userDao = userDao;
  }

  public SimpleHandler wrap(final SimpleHandler handler) {
    return new Wrapper(handler);
  }

  private class Wrapper implements SimpleHandler {
    private final SimpleHandler handler;

    public Wrapper(final SimpleHandler handler) {
      this.handler = handler;
    }

    @Override
    public Response handle(final Request request, final String content) throws IOException {
      if (checkAuth(request)) {
        return handler.handle(request, content);
      }
      return UNAUTHORIZED_RESPONSE;
    }
  }

  public void saveUser(final String username, final String password) throws IOException {
    userDao.upsert(username, password);
  }

  private boolean checkAuth(final Request request) throws IOException {
    final List<String> authHeaders = request.getRequestHeaders().get("authorization");
    if (authHeaders == null) {
      return false;
    }
    for (final String authHeader : authHeaders) {
      if (checkAuth(authHeader)) {
        return true;
      }
    }
    return false;
  }

  private boolean checkAuth(final String authHeader) throws IOException {
    if (!authHeader.startsWith(BASIC_PREFIX)) {
      return false;
    }
    final byte[] raw = authHeader.substring(BASIC_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
    final byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(raw);
    } catch (final IllegalArgumentException e) {
      return false;
    }
    return checkDecodedAuth(new String(decoded, StandardCharsets.UTF_8));
  }

  private boolean checkDecodedAuth(final String decoded) throws IOException {
    final int i = decoded.indexOf(':');
    if (i < 0) {
      return false;
    }
    final String username = decoded.substring(0, i);
    final String password = decoded.substring(i + 1);
    final String storedPassword;
    try {
      storedPassword = userDao.get(username);
    } catch (final NoSuchElementException e) {
      return false;
    }
    return password.equals(storedPassword);
  }
}
