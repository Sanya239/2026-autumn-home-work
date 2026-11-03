package company.vk.edu.distrib.compute.nosorozhek.urlshortener.authentication;

import company.vk.edu.distrib.compute.nosorozhek.urlshortener.handlers.Handler;

import java.util.Base64;

public class AuthenticationMiddleware {
    private static final String AUTHORIZATION = "Authorization";
    private final UserService userService;

    public AuthenticationMiddleware(UserService userService) {
        this.userService = userService;
    }

    private static Credentials parseBasicCredentials(String header)
            throws InvalidCredentialsException, UnauthorizedException {
        if (header == null) {
            throw new UnauthorizedException();
        }

        int separator = header.indexOf(' ');
        if (separator < 0 || !"Basic".equalsIgnoreCase(header.substring(0, separator))) {
            throw new UnauthorizedException();
        }

        String token = header.substring(separator + 1).trim();

        try {
            return parseColonSeparatedCredentials(new String(Base64.getDecoder().decode(token)));
        } catch (IllegalArgumentException | InvalidCredentialsException e) {
            throw new UnauthorizedException(e);
        }
    }

    public static Credentials parseColonSeparatedCredentials(String value) throws InvalidCredentialsException {
        int colon = value.indexOf(':');
        if (colon <= 0) {
            throw new InvalidCredentialsException("Credentials must follow the <username>:<password> format");
        }

        return new Credentials(
                value.substring(0, colon),
                value.substring(colon + 1)
        );
    }

    public Handler wrap(Handler handler) throws InvalidCredentialsException, UnauthorizedException {
        return (exchange, parameters) -> {
            var headers = exchange.getRequestHeaders();
            Credentials credentials = parseBasicCredentials(headers.getFirst(AUTHORIZATION));
            userService.authenticate(credentials);

            handler.handle(exchange, parameters);
        };
    }
}
