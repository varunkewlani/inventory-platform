package com.inventoryplatform.inventory;

import com.inventoryplatform.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The spec's mandatory concurrency test: 100 simultaneous requests against
 * an available inventory of 10 must result in exactly 10 successes.
 *
 * <p>Drives this through the real HTTP endpoint (not calling
 * InventoryService directly), against real MySQL (Testcontainers), so it
 * exercises the actual mechanism end to end: HTTP -> Spring Security ->
 * TenantContext -> InventoryRepository's atomic conditional UPDATE -> MySQL
 * row lock. All 100 requests are released together via a CountDownLatch so
 * they genuinely race each other, not just run "concurrently" in name.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrencyTest extends AbstractIntegrationTest {

    private static final int STARTING_STOCK = 10;
    private static final int CONCURRENT_REQUESTS = 100;

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    @Test
    void oneHundredConcurrentReservationsAgainstTenUnitsYieldsExactlyTenSuccesses() throws Exception {
        String baseUrl = "http://localhost:" + port + "/api/v1";

        String token = registerAndGetToken(baseUrl);
        Long warehouseId = createWarehouse(baseUrl, token);
        Long productId = createProduct(baseUrl, token);
        stockInventory(baseUrl, token, warehouseId, productId, STARTING_STOCK);

        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger insufficientCount = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            tasks.add(() -> {
                ready.countDown();
                start.await();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(token);
                Map<String, Object> body = Map.of("warehouseId", warehouseId, "productId", productId, "quantity", 1);

                ResponseEntity<Map> response = rest.exchange(
                        baseUrl + "/inventory/reserve", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    successCount.incrementAndGet();
                } else if (response.getStatusCode() == HttpStatus.CONFLICT) {
                    Map<?, ?> error = (Map<?, ?>) response.getBody().get("error");
                    if ("INSUFFICIENT_INVENTORY".equals(error.get("code"))) {
                        insufficientCount.incrementAndGet();
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();

        for (Future<Void> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(successCount.get()).isEqualTo(STARTING_STOCK);
        assertThat(insufficientCount.get()).isEqualTo(CONCURRENT_REQUESTS - STARTING_STOCK);

        Map<String, Object> finalInventory = getInventory(baseUrl, token, warehouseId, productId);
        assertThat(finalInventory.get("availableQuantity")).isEqualTo(0);
        assertThat(finalInventory.get("reservedQuantity")).isEqualTo(STARTING_STOCK);
    }

    @SuppressWarnings("unchecked")
    private String registerAndGetToken(String baseUrl) {
        Map<String, Object> body = Map.of(
                "organizationName", "Concurrency Test Org",
                "name", "Test Admin",
                "email", "concurrency-" + System.nanoTime() + "@test.local",
                "password", "password123");
        ResponseEntity<Map> response = rest.postForEntity(baseUrl + "/auth/register", body, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return (String) data.get("accessToken");
    }

    @SuppressWarnings("unchecked")
    private Long createWarehouse(String baseUrl, String token) {
        HttpHeaders headers = authHeaders(token);
        Map<String, Object> body = Map.of("name", "Test Warehouse");
        ResponseEntity<Map> response = rest.exchange(baseUrl + "/warehouses", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return Long.valueOf(data.get("id").toString());
    }

    @SuppressWarnings("unchecked")
    private Long createProduct(String baseUrl, String token) {
        HttpHeaders headers = authHeaders(token);
        Map<String, Object> body = Map.of("sku", "CONC-1", "name", "Concurrency Product", "price", 9.99);
        ResponseEntity<Map> response = rest.exchange(baseUrl + "/products", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return Long.valueOf(data.get("id").toString());
    }

    private void stockInventory(String baseUrl, String token, Long warehouseId, Long productId, int quantity) {
        HttpHeaders headers = authHeaders(token);
        Map<String, Object> body = Map.of("warehouseId", warehouseId, "productId", productId, "type", "ADD", "quantity", quantity);
        rest.exchange(baseUrl + "/inventory/adjust", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getInventory(String baseUrl, String token, Long warehouseId, Long productId) {
        HttpHeaders headers = authHeaders(token);
        ResponseEntity<Map> response = rest.exchange(
                baseUrl + "/inventory?warehouseId=" + warehouseId + "&productId=" + productId,
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        return content.get(0);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}
