package com.pragma.franquicias.rules;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Hexagonal dependency rules. The Gradle modules already stop the domain from seeing Spring;
 * these tests are the second net and also protect the direction of the dependencies between layers.
 */
class ArchitectureRulesTest {

    private static final String BASE = "com.pragma.franquicias";

    private final com.tngtech.archunit.core.domain.JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    void domainModelDoesNotKnowSpringNorInfrastructure() {
        noClasses().that().resideInAPackage(BASE + ".model..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", BASE + ".infrastructure..",
                        BASE + ".usecase..")
                .allowEmptyShould(true)
                .because("the domain model only depends on itself (ports and entities)")
                .check(classes);
    }

    @Test
    void useCasesDoNotKnowSpringNorAdapters() {
        noClasses().that().resideInAPackage(BASE + ".usecase..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", BASE + ".infrastructure..")
                .allowEmptyShould(true)
                .because("use cases depend on ports, never on adapters or on the framework")
                .check(classes);
    }

    @Test
    void noRestControllersAnywhere() {
        noClasses()
                .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Controller")
                .allowEmptyShould(true)
                .because("routes are RouterFunction + Handler, never controllers")
                .check(classes);
    }
}
