package cl.duoc.farmaexpress.bff;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BffIntegrationTests {
    private static final Upstream PRESCRIPTIONS = new Upstream();
    private static final Upstream CATALOG = new Upstream();
    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("services.prescriptions.url", PRESCRIPTIONS::url);
        registry.add("services.catalog.url", CATALOG::url);
        registry.add("services.connect-timeout", () -> "1s");
        registry.add("services.read-timeout", () -> "1s");
        registry.add("frontend.origin", () -> "http://frontend.test");
        registry.add("logging.level.root", () -> "WARN");
    }

    @BeforeEach
    void reset() { PRESCRIPTIONS.reset(); CATALOG.reset(); }

    @AfterAll
    static void close() { PRESCRIPTIONS.close(); CATALOG.close(); }

    @Test
    void prescriptionListForwardsFiltersAndOptionalAuthorization() throws Exception {
        PRESCRIPTIONS.body = "[{\"id\":7,\"status\":\"EN_PREPARACION\"}]";
        mvc.perform(get("/api/bff/prescriptions")
                .param("status", "EN_PREPARACION").param("from", "2026-09-01T00:00:00")
                .param("to", "2026-09-30T23:59:59").header("Authorization", "Bearer test-only"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("EN_PREPARACION"));
        Call call = PRESCRIPTIONS.calls.remove();
        assertEquals("GET", call.method());
        assertEquals("/api/prescriptions", call.path());
        assertTrue(call.query().contains("status=EN_PREPARACION"));
        assertTrue(call.query().contains("from=2026-09-01T00:00:00"));
        assertTrue(call.query().contains("to=2026-09-30T23:59:59"));
        assertEquals("Bearer test-only", call.authorization());
        assertTrue(CATALOG.calls.isEmpty());
    }

    @Test
    void createsPrescriptionPreserving201AndJson() throws Exception {
        PRESCRIPTIONS.status = 201;
        PRESCRIPTIONS.body = "{\"id\":1,\"status\":\"INGRESADA\"}";
        String payload = "{\"patientId\":\"p1\",\"pharmacyId\":\"f1\"}";
        mvc.perform(post("/api/bff/prescriptions").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("INGRESADA"));
        Call call = PRESCRIPTIONS.calls.remove();
        assertEquals("POST", call.method());
        assertEquals("/api/prescriptions", call.path());
        assertEquals(payload, call.body());
        assertNull(call.authorization());
    }

    @Test
    void getAndUpdatePrescriptionUseInternalRoutes() throws Exception {
        mvc.perform(get("/api/bff/prescriptions/7")).andExpect(status().isOk());
        assertEquals("/api/prescriptions/7", PRESCRIPTIONS.calls.remove().path());
        String payload = "{\"status\":\"VALIDADA\"}";
        mvc.perform(put("/api/bff/prescriptions/7/status").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
        Call call = PRESCRIPTIONS.calls.remove();
        assertEquals("PUT", call.method());
        assertEquals("/api/prescriptions/7/status", call.path());
        assertEquals(payload, call.body());
    }

    @Test
    void catalogListAndAliasUseCatalogService() throws Exception {
        CATALOG.body = "[{\"id\":1,\"sku\":\"MED-001\"}]";
        for (String route : new String[] {"/api/bff/catalog", "/api/bff/catalog/medicamentos"}) {
            mvc.perform(get(route)).andExpect(status().isOk()).andExpect(jsonPath("$[0].sku").value("MED-001"));
            assertEquals("/api/catalog/medicamentos", CATALOG.calls.remove().path());
        }
        assertTrue(PRESCRIPTIONS.calls.isEmpty());
    }

    @Test
    void catalogCrudForwardsMethodsBodiesAndStatuses() throws Exception {
        String payload = "{\"sku\":\"MED-001\",\"nombre\":\"Paracetamol\",\"precio\":1990,\"stock\":5}";
        CATALOG.status = 201;
        mvc.perform(post("/api/bff/catalog/medicamentos").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        Call created = CATALOG.calls.remove();
        assertEquals("POST", created.method());
        assertEquals("/api/catalog/medicamentos", created.path());
        assertEquals(payload, created.body());
        CATALOG.status = 200;
        mvc.perform(get("/api/bff/catalog/medicamentos/1")).andExpect(status().isOk());
        assertEquals("/api/catalog/medicamentos/1", CATALOG.calls.remove().path());
        mvc.perform(put("/api/bff/catalog/medicamentos/1").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
        Call updated = CATALOG.calls.remove();
        assertEquals("PUT", updated.method());
        assertEquals("/api/catalog/medicamentos/1", updated.path());
        assertEquals(payload, updated.body());
        CATALOG.status = 204;
        mvc.perform(delete("/api/bff/catalog/medicamentos/1")).andExpect(status().isNoContent());
        Call deleted = CATALOG.calls.remove();
        assertEquals("DELETE", deleted.method());
        assertEquals("/api/catalog/medicamentos/1", deleted.path());
    }

    @Test
    void upstreamErrorsAreNotReplacedBySuccessOrGeneric500() throws Exception {
        for (int code : new int[] {400, 401, 403, 404, 409, 422, 429, 500, 503}) {
            PRESCRIPTIONS.status = code;
            PRESCRIPTIONS.body = "{\"status\":" + code + ",\"detail\":\"upstream error\"}";
            PRESCRIPTIONS.contentType = "application/problem+json";
            mvc.perform(get("/api/bff/prescriptions/1"))
                    .andExpect(status().is(code))
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.detail").value("upstream error"));
            PRESCRIPTIONS.calls.remove();
        }
    }

    @Test
    void unavailableUpstreamReturns502WithoutInternalDetails() throws Exception {
        PRESCRIPTIONS.disconnect = true;
        mvc.perform(get("/api/bff/prescriptions"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("No fue posible comunicarse con el servicio interno"));
    }

    @Test
    void slowUpstreamIsBoundedByReadTimeout() throws Exception {
        PRESCRIPTIONS.delayMillis = 1500;
        mvc.perform(get("/api/bff/prescriptions")).andExpect(status().isBadGateway());
    }

    @Test
    void corsAllowsConfiguredReactOriginAndRejectsOthers() throws Exception {
        mvc.perform(options("/api/bff/prescriptions").header("Origin", "http://frontend.test")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://frontend.test"));
        mvc.perform(options("/api/bff/prescriptions").header("Origin", "http://other.test")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
        assertTrue(PRESCRIPTIONS.calls.isEmpty());
    }

    @Test
    void invalidIdsAndNonJsonBodiesNeverReachServices() throws Exception {
        mvc.perform(get("/api/bff/prescriptions/not-an-id")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/bff/prescriptions").contentType(MediaType.TEXT_PLAIN).content("invalid"))
                .andExpect(status().isUnsupportedMediaType());
        assertTrue(PRESCRIPTIONS.calls.isEmpty());
    }

    record Call(String method, String path, String query, String body, String authorization) {}

    static class Upstream {
        final HttpServer server;
        final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        final ConcurrentLinkedQueue<Call> calls = new ConcurrentLinkedQueue<>();
        volatile int status = 200;
        volatile String body = "{}";
        volatile String contentType = "application/json";
        volatile boolean disconnect;
        volatile long delayMillis;

        Upstream() {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            } catch (IOException ex) { throw new IllegalStateException(ex); }
            server.setExecutor(executor);
            server.createContext("/", exchange -> {
                int responseStatus = status;
                byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
                String responseType = contentType;
                long delay = delayMillis;
                calls.add(new Call(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                        exchange.getRequestURI().getQuery(),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                        exchange.getRequestHeaders().getFirst("Authorization")));
                if (disconnect) { exchange.close(); return; }
                if (delay > 0) {
                    try { Thread.sleep(delay); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                }
                try {
                    exchange.getResponseHeaders().set("Content-Type", responseType);
                    exchange.sendResponseHeaders(responseStatus, responseStatus == 204 ? -1 : responseBody.length);
                    if (responseStatus != 204) exchange.getResponseBody().write(responseBody);
                } finally { exchange.close(); }
            });
            server.start();
        }

        String url() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
        void reset() {
            status = 200; body = "{}"; contentType = "application/json";
            disconnect = false; delayMillis = 0; calls.clear();
        }
        void close() { server.stop(0); executor.shutdownNow(); }
    }
}
