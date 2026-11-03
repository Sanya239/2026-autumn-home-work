package company.vk.edu.distrib.compute.nixxx11.urlshortener;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Request;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.nixxx11.urlshortener.http.AbstractService;
import company.vk.edu.distrib.compute.nixxx11.urlshortener.http.Handler;
import company.vk.edu.distrib.compute.nixxx11.urlshortener.http.LogWrapper;
import company.vk.edu.distrib.compute.nixxx11.urlshortener.http.Response;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

public class UrlShortenerServiceImpl extends AbstractService implements UrlShortenerService {
  private static final String ALLOWED_ID_CHARS;

  static {
    final StringBuilder sb = new StringBuilder();
    for (char c = 'a'; c <= 'z'; c++) {
      sb.append(c);
    }
    for (char c = 'A'; c <= 'Z'; c++) {
      sb.append(c);
    }
    for (char c = '0'; c <= '9'; c++) {
      sb.append(c);
    }
    ALLOWED_ID_CHARS = sb.toString();
  }

  private static final int ID_LENGTH = 10;
  private static final Pattern ID_PATTERN = Pattern.compile("[" + ALLOWED_ID_CHARS + "]{" + ID_LENGTH + "}");

  private static final Set<String> ALLOWED_SCHEMAS = Set.of("http", "https");

  private static final int HTTP_UNPROCESSABLE_CONTENT = 422;

  private static final String LINKS_BASE_PATH = "/v0/links/";
  private static final String REDIRECT_BASE_PATH = "/";

  private Dao<String> linksDao;
  private final UrlShortenerAuthSystem authSystem;
  private final String host;
  private final Random random = new Random();

  public UrlShortenerServiceImpl(
      final int port,
      final Dao<String> linksDao,
      final Dao<String> usersDao
  ) throws IOException {
    super(port);
    this.linksDao = linksDao;
    this.authSystem = new UrlShortenerAuthSystem(usersDao);
    this.host = "http://localhost:" + port;
    init();
  }

  @Override
  public void setLinksDao(final Dao<String> dao) {
    this.linksDao = dao;
  }

  @Override
  protected Map<String, HttpHandler> getHandlers() {
    return Map.of(
        "/v0/status", new Handler()
            .setGetHandler(this::status)
            .wrap(LogWrapper::new),
        "/v0/links", new Handler()
            .setPostHandler(this::createLink)
            .wrap(authSystem::wrap)
            .wrap(LogWrapper::new),
        LINKS_BASE_PATH, new Handler()
            .setGetHandler(this::getLink)
            .setPutHandler(this::updateLink)
            .setDeleteHandler(this::deleteLink)
            .wrap(authSystem::wrap)
            .wrap(LogWrapper::new),
        REDIRECT_BASE_PATH, new Handler()
            .setGetHandler(this::redirect)
            .wrap(LogWrapper::new),
        "/internal/users", new Handler()
            .setPostHandler(this::createUser)
            .wrap(LogWrapper::new)
    );
  }

  private Response status(final Request ignored1, final String ignored2) {
    return new Response.Basic(HTTP_OK, "OK");
  }

  private Response createLink(final Request ignored, final String content) throws IOException {
    if (!isValidLink(content)) {
      return invalidLink(content);
    }

    final String id = randomId();
    linksDao.upsert(id, content);

    return new Response.Basic(HTTP_CREATED, host + '/' + id);
  }

  private Response getLink(final Request request, final String ignored) throws IOException {
    final String id = getId(request, LINKS_BASE_PATH);
    if (!isValidId(id)) {
      return invalidId(id);
    }

    final String value;
    try {
      value = linksDao.get(id);
    } catch (final NoSuchElementException e) {
      return idNotFound(id);
    }

    return new Response.Basic(HTTP_OK, value);
  }

  private Response updateLink(final Request request, final String content) throws IOException {
    final String id = getId(request, LINKS_BASE_PATH);
    if (!isValidId(id)) {
      return invalidId(id);
    }

    if (!isValidLink(content)) {
      return invalidLink(content);
    }

    final String value;
    try {
      value = linksDao.get(id);
    } catch (final NoSuchElementException e) {
      return idNotFound(id);
    }
    if (!content.equals(value)) {
      linksDao.upsert(id, content);
    }

    return new Response.Empty(HTTP_OK);
  }

  private Response deleteLink(final Request request, final String ignored) throws IOException {
    final String id = getId(request, LINKS_BASE_PATH);
    if (!isValidId(id)) {
      return invalidId(id);
    }

    linksDao.delete(id);

    return new Response.Empty(HTTP_ACCEPTED);
  }

  private Response redirect(final Request request, final String ignored) throws IOException {
    final String id = getId(request, "/");
    if (!isValidId(id)) {
      return invalidId(id);
    }

    final String value;
    try {
      value = linksDao.get(id);
    } catch (final NoSuchElementException e) {
      return idNotFound(id);
    }

    return new Response.Redirect(value);
  }

  private Response createUser(final Request ignored, final String content) throws IOException {
    final int i = content.indexOf(':');
    if (i < 0) {
      return new Response.Basic(HTTP_UNPROCESSABLE_CONTENT, "Invalid user credentials: " + content);
    }

    final String login = content.substring(0, i);
    final String password = content.substring(i + 1);
    authSystem.saveUser(login, password);

    return new Response.Empty(HTTP_OK);
  }

  private String randomId() {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ID_LENGTH; i++) {
      final int pos = random.nextInt(ALLOWED_ID_CHARS.length());
      final char c = ALLOWED_ID_CHARS.charAt(pos);
      sb.append(c);
    }
    return sb.toString();
  }

  private static String getId(final Request request, final String basePath) {
    final String path = request.getRequestURI().getPath();
    return path.substring(basePath.length());
  }

  private static boolean isValidId(final String id) {
    return ID_PATTERN.matcher(id).matches();
  }

  private static boolean isValidLink(final String link) {
    try {
      final URI uri = new URI(link);
      return uri.isAbsolute() && uri.getScheme() != null && ALLOWED_SCHEMAS.contains(uri.getScheme());
    } catch (final URISyntaxException e) {
      return false;
    }
  }

  private static Response invalidId(final String id) {
    return new Response.Basic(HTTP_UNPROCESSABLE_CONTENT, "Invalid id: " + id);
  }

  private static Response invalidLink(final String link) {
    return new Response.Basic(HTTP_UNPROCESSABLE_CONTENT, "Invalid link: " + link);
  }

  private static Response idNotFound(final String id) {
    return new Response.Basic(HTTP_NOT_FOUND, "Id not found: " + id);
  }
}
