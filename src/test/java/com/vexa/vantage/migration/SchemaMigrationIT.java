package com.vexa.vantage.migration;

import com.vexa.vantage.support.PostgresTestContainerConfig;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración de la migración de esquema (Fase 2): verifica que
 * Flyway, y solo Flyway, es responsable de gestionar el esquema contra una
 * base de datos PostgreSQL real (vía {@link PostgresTestContainerConfig}).
 *
 * <p>Usa el perfil {@code test} (con {@code spring.jpa.hibernate.ddl-auto=none}
 * y {@code spring.flyway.enabled=true} explícitos) para que Hibernate no
 * interfiera con las tablas creadas por las migraciones {@code V1} y
 * {@code V2}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class SchemaMigrationIT extends PostgresTestContainerConfig {

    private static final List<String> EXPECTED_TABLES = List.of(
            "tenant", "app_user", "refresh_token", "workflow_definition",
            "project", "project_membership", "board", "work_item", "comment", "contact");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesBaselineAndSeedMigrationsCleanlyAgainstAnEmptyDatabase() {
        List<MigrationInfo> applied = List.of(flyway.info().applied());

        assertThat(applied)
                .extracting(info -> info.getVersion().toString())
                .contains("1", "2", "3");
        assertThat(applied)
                .allSatisfy(info -> assertThat(info.getState().isFailed()).isFalse());
    }

    @Test
    void keyTablesFromTheThreeBoundedContextsExist() {
        for (String table : EXPECTED_TABLES) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = 'public' AND table_name = ?",
                    Integer.class, table);

            assertThat(count).as("la tabla '%s' debe existir", table).isEqualTo(1);
        }
    }

    @Test
    void atLeastOneBuiltinWorkflowTemplateExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workflow_definition WHERE is_builtin_template = true",
                Integer.class);

        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void v3AddsTheNullableTshirtSizeColumnOnWorkItem() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'work_item' AND column_name = 'tshirt_size'",
                Integer.class);

        assertThat(count).isEqualTo(1);
    }
}
