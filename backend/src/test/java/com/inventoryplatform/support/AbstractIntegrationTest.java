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

    // withReuse: this sandbox is memory-constrained enough that spinning up
    // a fresh MySQL + Redis container (and a fresh Spring context) per test
    // class caused real "Communications link failure" errors under memory
    // pressure — not a logic bug, an environment one. Reuse keeps one
    // physical container alive across test classes/runs (keyed by config
    // hash), cutting both startup cost and peak memory. Requires
    // ~/.testcontainers.properties to have testcontainers.reuse.enable=true.
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("inventory_platform_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

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
    }
}
