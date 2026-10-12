package company.vk.edu.distrib.compute.test.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.HttpService;
import company.vk.edu.distrib.compute.kv.ClusterDaoFactoryTest;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.kv.KVServiceTest;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junitpioneer.jupiter.RetryingTest;

import java.io.IOException;
import java.net.BindException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;

import static company.vk.edu.distrib.compute.test.AbstractArgumentsProvider.findAnnotatedFactories;
import static company.vk.edu.distrib.compute.test.TestUtils.*;
import static company.vk.edu.distrib.compute.test.urlshortener.LinksApiTest.createLink;
import static company.vk.edu.distrib.compute.test.urlshortener.LinksApiTest.getLinks;
import static company.vk.edu.distrib.compute.test.urlshortener.RemoteDaoLinksTest.groupByPackageName;
import static org.junit.jupiter.api.Assertions.*;

@ParameterizedClass(allowZeroInvocations = true)
@MethodSource("createServiceDaoPairs")
@EnabledIfEnvironmentVariable(named = "CURRENT_DATE", matches = "2026-(10-07|10-08|10-09|10-10|10-11|10-12|10-13|10-14)")
public class ShardingTest {
    private static final int CLUSTER_SIZE = 2;
    private static final int KEYS_COUNT = 50;
    private static final int HTTP_OK = 200;
    public static final String TEST_LINK_ID = "10db3750xY";
    public static final String TEST_LONG_LINK = "https://ya.ru/search/?text=test";
    public static final String TEST_LONG_LINK_2 = "https://ya.ru/search/?text=test2";
    private static final String ENTITY_PATH = "/v0/entity";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Parameter(0)
    AbstractHttpServiceFactory<? extends UrlShortenerService> serviceFactory;

    @Parameter(1)
    AbstractHttpServiceFactory<? extends KVService> kvServiceFactory;

    @Parameter(2)
    RemoteDaoFactory<String> remoteDaoFactory;

    int port;

    int[] remotePorts = new int[CLUSTER_SIZE];

    UrlShortenerService service;

    Dao<String> remoteDao;

    KVService[] clusterNodes = new KVService[CLUSTER_SIZE];

    @BeforeEach
    void setup() throws IOException {

        for (int i = 0; i < CLUSTER_SIZE; i++) {
            this.remotePorts[i] = randomPort();
            this.clusterNodes[i] = kvServiceFactory.create(this.remotePorts[i]);
        }

        this.port = randomPort(remotePorts);
        this.service = serviceFactory.create(port);
        this.remoteDao = remoteDaoFactory.create(remotePorts);
        service.setLinksDao(remoteDao);
    }

    @AfterEach
    void tearDown() {
        httpClient.close();
    }

    @RetryingTest(onExceptions = {BindException.class}, maxAttempts = 10, suspendForMs = 500)
    void fullFlowShouldWork() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            try {
                startCluster();
                service.start();
                runHttpCtx(httpClient, port, () -> {
                    tryCreateTestUser();

                    String longLink = TEST_LONG_LINK;
                    HttpResponse<String> createResponse = createLink(longLink);
                    assertEquals(201, createResponse.statusCode());
                    assertEquals(CONTENT_TYPE_TEXT, header(createResponse, "Content-Type"));

                    String id = extractId(port, createResponse.body());
                    assertDoesNotThrow(() -> remoteDao.get(id));

                    HttpResponse<String> getResponse = getLinks(id);
                    assertEquals(200, getResponse.statusCode());
                    assertEquals(CONTENT_TYPE_TEXT, header(getResponse, "Content-Type"));
                    assertEquals(longLink, getResponse.body());

                    List<Integer> nodesOwningId = findNodesOwningId(id);
                    assertEquals(1, nodesOwningId.size());
                });
            } finally {
                service.stop();
                stopCluster();
            }
        });
    }

    @RetryingTest(onExceptions = {BindException.class}, maxAttempts = 10, suspendForMs = 500)
    void shouldFollowSameNodeWithSameId() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            try {
                startCluster();
                remoteDao.delete(TEST_LINK_ID);

                assertDoesNotThrow(() -> remoteDao.upsert(TEST_LINK_ID, TEST_LONG_LINK));
                assertEquals(TEST_LONG_LINK, remoteDao.get(TEST_LINK_ID));

                List<Integer> nodesOwningId = findNodesOwningId(TEST_LINK_ID);
                assertEquals(1, nodesOwningId.size());

                assertDoesNotThrow(() -> remoteDao.upsert(TEST_LINK_ID, TEST_LONG_LINK_2));
                assertEquals(TEST_LONG_LINK_2, remoteDao.get(TEST_LINK_ID));

                List<Integer> upsertedNodesOwningId = findNodesOwningId(TEST_LINK_ID);
                assertEquals(1, upsertedNodesOwningId.size());
                assertEquals(nodesOwningId.getFirst(), upsertedNodesOwningId.getFirst());

                assertDoesNotThrow(() -> remoteDao.delete(TEST_LINK_ID));
                assertTrue(findNodesOwningId(TEST_LINK_ID).isEmpty());
            } finally {
                stopCluster();
            }
        });
    }

    @RetryingTest(onExceptions = {BindException.class}, maxAttempts = 10, suspendForMs = 500)
    void shouldSpreadKeysAcrossNodes() {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            try {
                startCluster();
                Set<Integer> usedNodes = new HashSet<>();
                for (int i = 0; i < KEYS_COUNT; i++) {
                    String id = i + "db3750xY";
                    assertDoesNotThrow(() -> remoteDao.upsert(id, TEST_LONG_LINK));

                    List<Integer> nodesOwningId = findNodesOwningId(id);
                    assertEquals(1, nodesOwningId.size());
                    usedNodes.addAll(nodesOwningId);
                }
                assertEquals(CLUSTER_SIZE, usedNodes.size());
            } finally {
                stopCluster();
            }
        });
    }

    private void startCluster() {
        Arrays.stream(clusterNodes).forEach(HttpService::start);
    }

    private void stopCluster() {
        Arrays.stream(clusterNodes).forEach(HttpService::stop);
    }

    private List<Integer> findNodesOwningId(String id) {
        List<Integer> idNodes = new ArrayList<>();
        for (int remotePort : remotePorts) {
            runHttpCtx(httpClient, remotePort, () -> {
                HttpResponse<String> stringHttpResponse = get(ENTITY_PATH + "?id=" + id);
                if (stringHttpResponse.statusCode() == HTTP_OK) {
                    idNodes.add(remotePort);
                }
            });
        }
        return idNodes;
    }

    static Stream<Arguments> createServiceDaoPairs() {
        var urlShortenerServiceFactories = groupByPackageName(findAnnotatedFactories(UrlShortenerTest.class));
        var kvServiceFactories = groupByPackageName(findAnnotatedFactories(KVServiceTest.class));
        var remoteDaoFactories = groupByPackageName(findAnnotatedFactories(ClusterDaoFactoryTest.class));
        return urlShortenerServiceFactories.entrySet().stream()
                .filter(it -> kvServiceFactories.containsKey(it.getKey()) && remoteDaoFactories.containsKey(it.getKey()))
                .map(it -> Arguments.of(
                        ReflectionUtils.newInstance(it.getValue()),
                        ReflectionUtils.newInstance(kvServiceFactories.get(it.getKey())),
                        ReflectionUtils.newInstance(remoteDaoFactories.get(it.getKey()))));
    }
}
