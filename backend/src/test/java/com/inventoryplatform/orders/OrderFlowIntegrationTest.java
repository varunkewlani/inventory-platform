package com.inventoryplatform.orders;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test (Phase 7): the full order-creation happy path against
 * real MySQL, plus order-total calculation and tenant isolation — the other
 * two items plan.md calls out for this phase, combined here rather than in
 * separate test classes since they share the same setup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderFlowIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    @Test
    @SuppressWarnings("unchecked")
    void orderCreationHappyPathReservesStockAndCalculatesCorrectTotal() {
        String baseUrl = "http://localhost:" + port + "/api/v1";
        String token = registerAndGetToken(baseUrl, "order-flow");

        Long warehouseId = createWarehouse(baseUrl, token);
        Long product1 = createProduct(baseUrl, token, "OFT-1", 10.50);
        Long product2 = createProduct(baseUrl, token, "OFT-2", 3.25);
        Long customerId = createCustomer(baseUrl, token);

        stockInventory(baseUrl, token, warehouseId, product1, 20);
        stockInventory(baseUrl, token, warehouseId, product2, 20);

        Map<String, Object> body = Map.of(
                "warehouseId", warehouseId,
                "customerId", customerId,
                "items", java.util.List.of(
                        Map.of("productId", product1, "quantity", 2),
                        Map.of("productId", product2, "quantity", 4)
                ));
        ResponseEntity<Map> response = rest.exchange(
                baseUrl + "/orders", HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> order = (Map<String, Object>) response.getBody().get("data");
        assertThat(order.get("status")).isEqualTo("PENDING");

        // 2 * 10.50 + 4 * 3.25 = 21.00 + 13.00 = 34.00
        assertThat(new java.math.BigDecimal(order.get("totalAmount").toString()))
                .isEqualByComparingTo("34.00");

        // Reservation actually happened: available dropped by the ordered quantity.
        Map<String, Object> inv1 = getInventory(baseUrl, token, warehouseId, product1);
        assertThat(inv1.get("availableQuantity")).isEqualTo(18);
        assertThat(inv1.get("reservedQuantity")).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void secondOrganizationCannotSeeFirstOrganizationsOrders() {
        String baseUrl = "http://localhost:" + port + "/api/v1";
        String tokenA = registerAndGetToken(baseUrl, "tenant-a");
        String tokenB = registerAndGetToken(baseUrl, "tenant-b");

        Long warehouseId = createWarehouse(baseUrl, tokenA);
        Long productId = createProduct(baseUrl, tokenA, "TEN-1", 5.00);
        Long customerId = createCustomer(baseUrl, tokenA);
        stockInventory(baseUrl, tokenA, warehouseId, productId, 5);

        Map<String, Object> body = Map.of(
                "warehouseId", warehouseId, "customerId", customerId,
                "items", java.util.List.of(Map.of("productId", productId, "quantity", 1)));
        ResponseEntity<Map> createResponse = rest.exchange(
                baseUrl + "/orders", HttpMethod.POST, new HttpEntity<>(body, authHeaders(tokenA)), Map.class);
        Map<String, Object> orderA = (Map<String, Object>) createResponse.getBody().get("data");
        Object orderId = orderA.get("id");

        // Org B lists orders: must be empty, not Org A's order.
        ResponseEntity<Map> listResponse = rest.exchange(
                baseUrl + "/orders", HttpMethod.GET, new HttpEntity<>(authHeaders(tokenB)), Map.class);
        Map<String, Object> pageB = (Map<String, Object>) listResponse.getBody().get("data");
        assertThat(((Number) pageB.get("totalElements")).longValue()).isEqualTo(0L);

        // Org B guessing Org A's order ID directly: clean 404, not a leak.
        ResponseEntity<Map> getResponse = rest.exchange(
                baseUrl + "/orders/" + orderId, HttpMethod.GET, new HttpEntity<>(authHeaders(tokenB)), Map.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @SuppressWarnings("unchecked")
    private String registerAndGetToken(String baseUrl, String label) {
        Map<String, Object> body = Map.of(
                "organizationName", label + " Org",
                "name", "Test Admin",
                "email", label + "-" + System.nanoTime() + "@test.local",
                "password", "password123");
        ResponseEntity<Map> response = rest.postForEntity(baseUrl + "/auth/register", body, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return (String) data.get("accessToken");
    }

    @SuppressWarnings("unchecked")
    private Long createWarehouse(String baseUrl, String token) {
        ResponseEntity<Map> response = rest.exchange(baseUrl + "/warehouses", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "WH"), authHeaders(token)), Map.class);
        return Long.valueOf(((Map<String, Object>) response.getBody().get("data")).get("id").toString());
    }

    @SuppressWarnings("unchecked")
    private Long createProduct(String baseUrl, String token, String sku, double price) {
        ResponseEntity<Map> response = rest.exchange(baseUrl + "/products", HttpMethod.POST,
                new HttpEntity<>(Map.of("sku", sku, "name", sku, "price", price), authHeaders(token)), Map.class);
        return Long.valueOf(((Map<String, Object>) response.getBody().get("data")).get("id").toString());
    }

    @SuppressWarnings("unchecked")
    private Long createCustomer(String baseUrl, String token) {
        ResponseEntity<Map> response = rest.exchange(baseUrl + "/customers", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Test Customer"), authHeaders(token)), Map.class);
        return Long.valueOf(((Map<String, Object>) response.getBody().get("data")).get("id").toString());
    }

    private void stockInventory(String baseUrl, String token, Long warehouseId, Long productId, int quantity) {
        Map<String, Object> body = Map.of("warehouseId", warehouseId, "productId", productId, "type", "ADD", "quantity", quantity);
        rest.exchange(baseUrl + "/inventory/adjust", HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getInventory(String baseUrl, String token, Long warehouseId, Long productId) {
        ResponseEntity<Map> response = rest.exchange(
                baseUrl + "/inventory?warehouseId=" + warehouseId + "&productId=" + productId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>) data.get("content");
        return content.get(0);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}
