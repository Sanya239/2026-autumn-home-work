package company.vk.edu.distrib.compute.sovesti.urlshortener.auth;

import java.io.IOException;
import java.util.Optional;

import com.sun.net.httpserver.Request;

public interface AuthenticationScheme {

    Optional<Credentials> parse(Request request);

    String challenge();

    @FunctionalInterface
    interface Credentials {

        boolean check() throws IOException;

    }

    final class Transient implements AuthenticationScheme {

        @Override
        public Optional<Credentials> parse(Request request) {
            return Optional.of(() -> true);
        }

        @Override
        public String challenge() {
            return "";
        }

    }
}
