package com.vexa.vantage.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared PostgreSQL Testcontainers configuration for the test suite.
 *
 * <p>Extend this class from any {@code @DataJpaTest} or {@code @SpringBootTest} that
 * needs a real PostgreSQL database. The container is started exactly once per JVM via
 * a static initializer block (the "singleton container" pattern) and is reused by every
 * test class that extends this base, instead of starting a fresh container per class.</p>
 */
public abstract class PostgresTestContainerConfig {

    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                .withDatabaseName("vantage_test")
                .withUsername("vantage_test")
                .withPassword("vantage_test")
                .withReuse(true);
        POSTGRES_CONTAINER.start();
    }

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES_CONTAINER::getDriverClassName);
    }
}
