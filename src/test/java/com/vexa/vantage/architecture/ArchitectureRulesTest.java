package com.vexa.vantage.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Impone los límites Hexagonal / DDD-lite de la re-arquitectura de la
 * plataforma:
 * <ul>
 *     <li>Los paquetes de dominio de cada contexto delimitado (bounded
 *     context) permanecen independientes de framework (sin dependencia de
 *     Spring, Hibernate/JPA ni Jackson).</li>
 *     <li>El paquete de dominio de un contexto delimitado nunca depende del
 *     paquete de dominio de otro contexto delimitado.</li>
 * </ul>
 */
class ArchitectureRulesTest {

    private static final String BASE_PACKAGE = "com.vexa.vantage";

    private static final String[] FRAMEWORK_PACKAGES = {
            "org.springframework..",
            "org.hibernate..",
            "jakarta.persistence..",
            "com.fasterxml.jackson.."
    };

    private static final String[] BOUNDED_CONTEXTS = {
            "shared", "identity", "delivery", "collaboration"
    };

    /**
     * Contextos delimitados de negocio que no deben depender del dominio del
     * otro. {@code shared} queda fuera a propósito: es el "shared kernel"
     * (objetos de valor y excepciones de dominio comunes, por ejemplo
     * {@code TenantId}/{@code UserId}) que todo contexto delimitado puede y
     * debe reutilizar; solo se le exige pureza de framework (ver la primera
     * regla), no aislamiento respecto de los demás.
     */
    private static final String[] ISOLATED_BOUNDED_CONTEXTS = {
            "identity", "delivery", "collaboration"
    };

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter().importPackages(BASE_PACKAGE);
    }

    @Test
    void domainPackagesDoNotDependOnSpringJpaOrJackson() {
        for (String context : BOUNDED_CONTEXTS) {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(domainPackageOf(context))
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES)
                    .allowEmptyShould(false)
                    .because("the domain layer of '" + context + "' must stay framework-agnostic (Hexagonal/DDD-lite)");
            rule.check(importedClasses);
        }
    }

    @Test
    void domainPackagesDoNotDependOnOtherBoundedContextsDomain() {
        for (String context : ISOLATED_BOUNDED_CONTEXTS) {
            for (String otherContext : ISOLATED_BOUNDED_CONTEXTS) {
                if (context.equals(otherContext)) {
                    continue;
                }
                ArchRule rule = noClasses()
                        .that().resideInAPackage(domainPackageOf(context))
                        .should().dependOnClassesThat().resideInAPackage(domainPackageOf(otherContext))
                        .allowEmptyShould(false)
                        .because("bounded context '" + context + "' domain must not depend on '" + otherContext + "' domain");
                rule.check(importedClasses);
            }
        }
    }

    /**
     * Construye el predicado de paquete de ArchUnit para la capa de dominio
     * de un contexto delimitado, por ejemplo {@code domainPackageOf("identity")}
     * produce {@code "..vantage.identity.domain.."}.
     */
    private static String domainPackageOf(String boundedContext) {
        return "..vantage." + boundedContext + ".domain..";
    }
}
