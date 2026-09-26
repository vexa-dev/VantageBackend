package com.vexa.vantage.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the Hexagonal / DDD-lite boundaries of the platform rearchitecture:
 * <ul>
 *     <li>Domain packages of every bounded context stay framework-agnostic
 *     (no Spring, Hibernate/JPA, or Jackson dependency).</li>
 *     <li>A bounded context's domain package never depends on another bounded
 *     context's domain package.</li>
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
        for (String context : BOUNDED_CONTEXTS) {
            for (String otherContext : BOUNDED_CONTEXTS) {
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
     * Builds the ArchUnit package predicate for a bounded context's domain
     * layer, e.g. {@code domainPackageOf("identity")} yields
     * {@code "..vantage.identity.domain.."}.
     */
    private static String domainPackageOf(String boundedContext) {
        return "..vantage." + boundedContext + ".domain..";
    }
}
