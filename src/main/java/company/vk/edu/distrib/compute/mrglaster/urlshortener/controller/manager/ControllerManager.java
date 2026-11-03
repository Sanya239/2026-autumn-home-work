package company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.annotation.Route;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.enums.StatusCode;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.controller.network.NetworkInteractable;
import company.vk.edu.distrib.compute.mrglaster.urlshortener.service.AuthorizationService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerManager implements NetworkInteractable {

    private final List<RouteDefinition> routes = new ArrayList<>();
    private final AuthorizationService authService;
    private static final String ROOT_PATH = "/";

    public ControllerManager(AuthorizationService authService) {
        this.authService = authService;
    }

    private static String[] splitPath(String urlPath) {
        String processableUrlPath = urlPath;
        if (processableUrlPath == null || processableUrlPath.isEmpty() || ROOT_PATH.equals(processableUrlPath)) {
            return new String[0];
        }
        if (processableUrlPath.startsWith(ROOT_PATH)) {
            processableUrlPath = processableUrlPath.substring(1);
        }
        if (processableUrlPath.endsWith(ROOT_PATH)) {
            processableUrlPath = processableUrlPath.substring(0, processableUrlPath.length() - 1);
        }
        if (processableUrlPath.isEmpty()) {
            return new String[0];
        }
        return processableUrlPath.split(ROOT_PATH);
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> result = new ConcurrentHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return result;
        }
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            if (eq < 0) {
                result.put(decode(pair), "");
            } else {
                String key = decode(pair.substring(0, eq));
                String value = decode(pair.substring(eq + 1));
                result.put(key, value);
            }
        }
        return result;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    public void addController(Object controller) {
        Class<?> clazz = controller.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Route.class)) {
                Route route = method.getAnnotation(Route.class);
                routes.add(new RouteDefinition(route.method().toUpperCase(Locale.US),
                        route.path(),
                        route.requiresAuthorization(),
                        method, controller));
            }
        }
    }

    public void register(HttpServer server) {
        server.createContext(ROOT_PATH, this::handleExchange);
    }

    private void handleExchange(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String requestMethod = exchange.getRequestMethod().toUpperCase(Locale.US);
        Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getRawQuery());

        RouteMatch match = findMatchingRoute(requestMethod, requestPath, queryParams);
        if (match == null) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        if (!isAuthorized(match.route, exchange)) {
            sendStatusCodeResponse(exchange, StatusCode.HTTP_UNAUTHORIZED);
            return;
        }

        invokeHandler(match, exchange);
    }

    private boolean isAuthorized(RouteDefinition route, HttpExchange exchange) {
        if (!route.requiresAuthorization() || authService == null) {
            return true;
        }
        try {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            return authService.checkBasicAuth(authHeader);
        } catch (Exception e) {
            return false;
        }
    }

    private RouteMatch findMatchingRoute(String method, String path, Map<String, String> queryParams) {
        String[] pathSegments = splitPath(path);
        Map<String, String> allParams = new ConcurrentHashMap<>();
        for (RouteDefinition route : routes) {
            if (!route.httpMethod.equals(method) || route.pathSegments.length != pathSegments.length) {
                continue;
            }
            Optional<Map<String, String>> extractedParams = matchRoute(route, pathSegments);
            if (extractedParams.isPresent()) {
                allParams.clear();
                allParams.putAll(extractedParams.get());
                allParams.putAll(queryParams);
                return new RouteMatch(route, allParams);
            }
        }
        return null;
    }

    private Optional<Map<String, String>> matchRoute(RouteDefinition route, String... pathSegments) {
        Map<String, String> extractedParams = new ConcurrentHashMap<>();
        for (int i = 0; i < route.pathSegments.length; i++) {
            String routeSegment = route.pathSegments[i];
            String pathSegment = pathSegments[i];

            if (isPathParam(routeSegment)) {
                extractedParams.put(extractParamName(routeSegment), decode(pathSegment));
            } else if (!routeSegment.equals(pathSegment)) {
                return Optional.empty();
            }
        }
        return Optional.of(extractedParams);
    }

    private static boolean isPathParam(String segment) {
        return segment.startsWith("{") && segment.endsWith("}");
    }

    private static String extractParamName(String segment) {
        return segment.substring(1, segment.length() - 1);
    }

    private void invokeHandler(RouteMatch match, HttpExchange exchange) throws IOException {
        Method method = match.route.method;
        Object instance = match.route.instance;

        try (HttpExchange ex = exchange) {
            Object[] args = new Object[method.getParameterCount()];
            for (int i = 0; i < method.getParameterCount(); i++) {
                Class<?> paramType = method.getParameterTypes()[i];
                if (paramType == HttpExchange.class) {
                    args[i] = ex;
                } else if (paramType == Map.class) {
                    args[i] = match.pathParams;
                } else {
                    sendStatusCodeResponse(ex, StatusCode.HTTP_INTERNAL_SERVER_ERROR);
                    return;
                }
            }

            try {
                method.invoke(instance, args);
            } catch (Exception e) {
                sendStatusCodeResponse(ex, StatusCode.HTTP_INTERNAL_SERVER_ERROR);
                return;
            }

            if (ex.getResponseCode() == -1) {
                sendStatusCodeResponse(ex, StatusCode.HTTP_OK);
            }
        }
    }

    private record RouteDefinition(String httpMethod, String[] pathSegments, boolean requiresAuthorization,
                                   Method method, Object instance) {
            RouteDefinition(String httpMethod,
                            String path,
                            boolean requiresAuthorization,
                            Method method,
                            Object instance) {
                this(httpMethod, splitPath(path), requiresAuthorization, method, instance);
            }
        }

    private record RouteMatch(RouteDefinition route, Map<String, String> pathParams) {
    }
}
