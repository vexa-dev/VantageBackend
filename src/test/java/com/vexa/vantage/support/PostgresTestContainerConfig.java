package com.vexa.vantage.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuración compartida de Testcontainers para PostgreSQL usada por la
 * suite de pruebas.
 *
 * <p>Extienda esta clase desde cualquier {@code @DataJpaTest} o {@code @SpringBootTest}
 * que necesite una base de datos PostgreSQL real. El contenedor se inicia
 * exactamente una vez por JVM mediante un bloque estático de inicialización
 * (el patrón de "contenedor único" o "singleton container") y es reutilizado
 * por cada clase de prueba que extiende esta base, en lugar de iniciar un
 * contenedor nuevo por cada clase.</p>
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
