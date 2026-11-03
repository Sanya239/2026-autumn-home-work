package company.vk.edu.distrib.compute.sanya239.kv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("PMD.GodClass")
public class KVService implements company.vk.edu.distrib.compute.kv.KVService {
    private static final String METHOD_GET = "GET";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_DELETE = "DELETE";
    private static final String STATUS_PATH = "/v0/status";
    private static final String ENTITY_PATH = "/v0/entity";
    private static final String ID_PARAMETER = "id";
    private static final String ACK_PARAMETER = "ack";
    private static final String CLUSTER_PROPERTY = "CLUSTER";
    private static final String REPLICATION_FACTOR_PROPERTY = "REPLICATION_FACTOR";
    private static final String INTERNAL_HEADER = "X-Internal-Request";
    private static final String INTERNAL_HEADER_VALUE = "true";
    private static final String LOCALHOST = "http://localhost:";

    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_NOT_FOUND = 404;
    private static final int STATUS_METHOD_NOT_ALLOWED = 405;
    private static final int STATUS_INTERNAL_ERROR = 500;
    private static final int STATUS_UNAVAILABLE = 503;
    private static final int DEFAULT_ACK = 1;
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65_535;
    private static final long RESPONSE_WAIT_SECONDS = 2;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(1);
    private static final byte[] EMPTY_BODY = new byte[0];
    private static final ReplicaResponse UNAVAILABLE_RESPONSE = new ReplicaResponse(STATUS_UNAVAILABLE, EMPTY_BODY);

    private final int port;
    private final FileDao dao;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private HttpServer server;

    public KVService(int port) throws IOException {
        this.port = port;
        Path file = Path.of(System.getProperty("java.io.tmpdir"), "sanya239-kv", port + ".db");
        dao = new FileDao(file.toString());
    }

    @Override
    public void start() {
        if (server != null) {
            throw new IllegalStateException("Service is already started");
        }
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext("/", this::handle);
            server.setExecutor(executor);
            server.start();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            handleRequest(exchange);
        } catch (RuntimeException exception) {
            respond(exchange, STATUS_INTERNAL_ERROR, null);
        }
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (STATUS_PATH.equals(path)) {
            respond(exchange, METHOD_GET.equals(method) ? STATUS_OK : STATUS_METHOD_NOT_ALLOWED, null);
            return;
        }
        if (!ENTITY_PATH.equals(path) && !path.startsWith(ENTITY_PATH + "/")) {
            respond(exchange, STATUS_NOT_FOUND, null);
            return;
        }

        String key;
        try {
            key = extractKey(exchange, path);
        } catch (IllegalArgumentException exception) {
            respond(exchange, STATUS_BAD_REQUEST, null);
            return;
        }
        if (key.isEmpty()) {
            respond(exchange, STATUS_BAD_REQUEST, null);
            return;
        }

        serve(exchange, method, key);
    }

    private static boolean isInternal(HttpExchange exchange) {
        return INTERNAL_HEADER_VALUE.equals(exchange.getRequestHeaders().getFirst(INTERNAL_HEADER));
    }

    private CompletableFuture<ReplicaResponse> sendInternalRequest(
            String method,
            String key,
            byte[] body,
            int targetPort
    ) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        URI endpoint = URI.create(LOCALHOST + targetPort + ENTITY_PATH + "?" + ID_PARAMETER + "=" + encodedKey);
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .timeout(REQUEST_TIMEOUT)
                .header(INTERNAL_HEADER, INTERNAL_HEADER_VALUE);

        HttpRequest request = switch (method) {
            case METHOD_GET -> builder.GET().build();
            case METHOD_PUT -> builder.PUT(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            case METHOD_DELETE -> builder.DELETE().build();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .handle((response, exception) -> exception == null
                        ? new ReplicaResponse(response.statusCode(), response.body())
                        : UNAVAILABLE_RESPONSE);
    }

    private void serveInternal(HttpExchange exchange, String method, String key) throws IOException {
        byte[] body = METHOD_PUT.equals(method) ? exchange.getRequestBody().readAllBytes() : EMPTY_BODY;
        ReplicaResponse response = executeLocally(method, key, body);
        respond(exchange, response.status(), response.body());
    }

    private ReplicaResponse executeLocally(String method, String key, byte[] body) {
        return switch (method) {
            case METHOD_GET -> {
                byte[] value = dao.get(key);
                yield value == null
                        ? new ReplicaResponse(STATUS_NOT_FOUND, EMPTY_BODY)
                        : new ReplicaResponse(STATUS_OK, value);
            }
            case METHOD_PUT -> {
                dao.upsert(key, body);
                yield new ReplicaResponse(STATUS_CREATED, EMPTY_BODY);
            }
            case METHOD_DELETE -> {
                dao.delete(key);
                yield new ReplicaResponse(STATUS_ACCEPTED, EMPTY_BODY);
            }
            default -> new ReplicaResponse(STATUS_METHOD_NOT_ALLOWED, EMPTY_BODY);
        };
    }

    private void serve(HttpExchange exchange, String method, String key) throws IOException {
        if (isInternal(exchange)) {
            serveInternal(exchange, method, key);
            return;
        }
        if (!isEntityMethod(method)) {
            respond(exchange, STATUS_METHOD_NOT_ALLOWED, null);
            return;
        }

        String cluster = System.getProperty(CLUSTER_PROPERTY);
        if (cluster == null || cluster.isBlank()) {
            serveInternal(exchange, method, key);
            return;
        }

        int[] ports;
        int replicationFactor;
        try {
            ports = parsePorts(cluster);
            replicationFactor = replicationFactor(ports.length);
        } catch (IllegalArgumentException exception) {
            respond(exchange, STATUS_INTERNAL_ERROR, null);
            return;
        }

        int ack;
        try {
            ack = extractAck(exchange);
        } catch (IllegalArgumentException exception) {
            respond(exchange, STATUS_BAD_REQUEST, null);
            return;
        }
        if (ack < DEFAULT_ACK || ack > replicationFactor) {
            respond(exchange, STATUS_BAD_REQUEST, null);
            return;
        }

        byte[] body = METHOD_PUT.equals(method) ? exchange.getRequestBody().readAllBytes() : EMPTY_BODY;
        BlockingQueue<ReplicaResponse> responses = new LinkedBlockingQueue<>();
        int startShard = selectShard(key, ports);
        for (int attempt = 0; attempt < replicationFactor; attempt++) {
            int targetPort = ports[(startShard + attempt) % ports.length];
            CompletableFuture<ReplicaResponse> request = targetPort == port
                    ? CompletableFuture.completedFuture(executeLocally(method, key, body))
                    : sendInternalRequest(method, key, body, targetPort);
            request.thenAccept(responses::add);
        }

        ReplicaResponse response = new ResponseCollector(method, ack, replicationFactor, responses).collect();
        respond(exchange, response.status(), response.body());
    }

    private static boolean isEntityMethod(String method) {
        return METHOD_GET.equals(method) || METHOD_PUT.equals(method) || METHOD_DELETE.equals(method);
    }

    private static int extractAck(HttpExchange exchange) {
        String ack = extractParameter(exchange, ACK_PARAMETER);
        return ack == null ? DEFAULT_ACK : Integer.parseInt(ack);
    }

    private static int[] parsePorts(String cluster) {
        int[] ports = Arrays.stream(cluster.split(","))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
        if (ports.length == 0 || Arrays.stream(ports).anyMatch(value -> value < MIN_PORT || value > MAX_PORT)) {
            throw new IllegalArgumentException("Invalid cluster configuration");
        }
        return ports;
    }

    private static int replicationFactor(int clusterSize) {
        String configuredFactor = System.getProperty(REPLICATION_FACTOR_PROPERTY);
        int factor = configuredFactor == null || configuredFactor.isBlank()
                ? clusterSize
                : Integer.parseInt(configuredFactor);
        if (factor < DEFAULT_ACK || factor > clusterSize) {
            throw new IllegalArgumentException("Invalid replication factor");
        }
        return factor;
    }

    private static int selectShard(String key, int... shards) {
        int shardNumber = 0;
        int maxHash = Integer.MIN_VALUE;

        for (int i = 0; i < shards.length; i++) {
            int hash = String.format("%d : %s", i, key).hashCode();
            if (hash > maxHash) {
                maxHash = hash;
                shardNumber = i;
            }
        }
        return shardNumber;
    }

    private static String extractKey(HttpExchange exchange, String path) {
        if (path.startsWith(ENTITY_PATH + "/")) {
            return path.substring(ENTITY_PATH.length() + 1);
        }
        String key = extractParameter(exchange, ID_PARAMETER);
        return key == null ? "" : key;
    }

    private static String extractParameter(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String parameter : query.split("&")) {
            int separator = parameter.indexOf('=');
            if (separator > 0 && name.equals(parameter.substring(0, separator))) {
                return URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        byte[] responseBody = body == null ? EMPTY_BODY : body;
        exchange.sendResponseHeaders(status, responseBody.length == 0 ? -1 : responseBody.length);
        if (responseBody.length > 0) {
            exchange.getResponseBody().write(responseBody);
        }
        exchange.close();
    }

    @Override
    public void stop() {
        server.stop(0);
        executor.shutdownNow();
        client.close();
        try {
            dao.close();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private record ReplicaResponse(int status, byte[] body) {
    }

    private record ResponseCollector(
            String method,
            int ack,
            int replicas,
            BlockingQueue<ReplicaResponse> responses
    ) {
        private ReplicaResponse collect() {
            int completed = 0;
            int confirmed = 0;
            byte[] value = null;
            try {
                while (completed < replicas && confirmed < ack) {
                    ReplicaResponse response = responses.poll(RESPONSE_WAIT_SECONDS, TimeUnit.SECONDS);
                    if (response == null) {
                        break;
                    }
                    completed++;
                    if (isConfirmation(response.status())) {
                        confirmed++;
                        value = selectValue(value, response);
                    }
                    if (confirmed + replicas - completed < ack) {
                        break;
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return UNAVAILABLE_RESPONSE;
            }

            return createResponse(confirmed, value);
        }

        private boolean isConfirmation(int status) {
            return switch (method) {
                case METHOD_GET -> status == STATUS_OK || status == STATUS_NOT_FOUND;
                case METHOD_PUT -> status == STATUS_CREATED;
                case METHOD_DELETE -> status == STATUS_ACCEPTED;
                default -> false;
            };
        }

        private byte[] selectValue(byte[] currentValue, ReplicaResponse response) {
            return METHOD_GET.equals(method) && response.status() == STATUS_OK ? response.body() : currentValue;
        }

        private ReplicaResponse createResponse(int confirmed, byte[] value) {
            if (confirmed < ack) {
                return UNAVAILABLE_RESPONSE;
            }
            return switch (method) {
                case METHOD_GET -> value == null
                        ? new ReplicaResponse(STATUS_NOT_FOUND, EMPTY_BODY)
                        : new ReplicaResponse(STATUS_OK, value);
                case METHOD_PUT -> new ReplicaResponse(STATUS_CREATED, EMPTY_BODY);
                case METHOD_DELETE -> new ReplicaResponse(STATUS_OK, EMPTY_BODY);
                default -> new ReplicaResponse(STATUS_METHOD_NOT_ALLOWED, EMPTY_BODY);
            };
        }
    }
}
