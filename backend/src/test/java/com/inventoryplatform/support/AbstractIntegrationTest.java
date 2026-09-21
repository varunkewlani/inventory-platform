package com.inventoryplatform.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Real MySQL via Testcontainers — not H2. The concurrency test in
 * particular depends on MySQL's actual row-locking behavior under an atomic
 * conditional UPDATE, which an in-memory substitute doesn't reliably
 * replicate.
 *
 * <p>Also a real Redis container: RateLimitFilter runs on every request and
 * fails open if Redis is unreachable (see its javadoc), but the TCP
 * connect-timeout on every single request adds real latency across 100
 * concurrent requests — a live Redis avoids that entirely and matches how
 * the app actually runs outside tests.
 */
@Testcontainers
public abstract class AbstractIntegrationTest {

    // Deliberately NOT using withReuse(true) here: it was tried as a fix for
    // this project's local dev sandbox (memory-constrained enough that fresh
    // containers per class caused real connection failures), but on CI's
    // GitHub-hosted runners it correlated with a Redis command hanging for
    // exactly Lettuce's 60s default timeout on the first request of the test
    // class *after* a long-running one -- consistent with a stale/half-torn-
    // down connection from a previous test class's closed Spring context on
    // the reused container. Plain per-run (Ryuk-managed) containers are
    // Testcontainers' standard, most-tested path and CI has the headroom
    // for them (GitHub-hosted runners: 7GB RAM vs. this project's sandbox).
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("inventory_platform_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        // High enough that no integration test's own request volume trips
        // it — rate limiting is tested at the unit level (RateLimitFilter
        // exercised directly), not by fighting it here in tests that are
        // about something else entirely.
        registry.add("app.rate-limit.requests-per-minute", () -> "100000");

        // No Kafka Testcontainer here (order publishing is exercised via the
        // ApplicationEvent, not a real broker) -- against the default
        // localhost:9092 with nothing listening, topic auto-provisioning at
        // startup and the @KafkaListener consumers' retry loop both burn
        // real time/CPU on the shared JVM these tests run in, which was
        // very likely why ConcurrencyTest's Redis calls were timing out
        // under contention (same mechanism that broke the Render deploy
        // before these two properties were introduced there).
        registry.add("app.kafka.topic-provisioning-enabled", () -> "false");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
    }
}
