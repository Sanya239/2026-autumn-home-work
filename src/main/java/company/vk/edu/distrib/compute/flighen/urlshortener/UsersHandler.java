package company.vk.edu.distrib.compute.flighen.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class UsersHandler implements HttpHandler {
    private static final int CREDENTIAL_PARTS_COUNT = 2;

    private static final Logger log =
            LoggerFactory.getLogger(UsersHandler.class);

    private static final int HTTP_OK = 200;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_UNPROCESSABLE_ENTITY = 422;

    private final Dao<String> usersDao;

    public UsersHandler(Dao<String> dao) {
        usersDao = dao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final String method = exchange.getRequestMethod();

        try (exchange) {
            if (Objects.equals(method, "POST")) {
                try (InputStream input = exchange.getRequestBody()) {
                    String data = new String(input.readAllBytes(), StandardCharsets.UTF_8);

                    if (parseData(data)) {
                        exchange.sendResponseHeaders(HTTP_OK, 0);
                    } else {
                        exchange.sendResponseHeaders(HTTP_UNPROCESSABLE_ENTITY, 0);
                    }
                }
            } else {
                exchange.sendResponseHeaders(HTTP_FORBIDDEN, 0);
            }
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error(
                        "I/O error while handling {} {}",
                        method,
                        exchange.getRequestURI(),
                        e
                );
            }
            throw e;
        }

        exchange.close();
    }

    private boolean parseData(String data) throws IOException {
        String stripData = data.strip();

        if (!stripData.contains(":")) {
            return false;
        }

        String[] userInfo = stripData.split(":", 2);

        if (userInfo.length != CREDENTIAL_PARTS_COUNT) {
            return false;
        }

        String login = userInfo[0];
        String password = userInfo[1];

        if (login.isBlank() || password.isBlank()) {
            return false;
        }

        usersDao.upsert(login, password);

        return true;
    }
}
