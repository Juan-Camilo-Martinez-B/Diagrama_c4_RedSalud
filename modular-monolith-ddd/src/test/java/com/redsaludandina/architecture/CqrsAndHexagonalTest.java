package com.redsaludandina.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CQRS estricto y arquitectura hexagonal en los 7 contextos. */
class CqrsAndHexagonalTest {

    /** Dominios de los contextos (el núcleo compartido, con ClinicalId o Require, sí se puede usar). */
    private static final String[] CONTEXT_DOMAINS = {"..identity.domain..", "..scheduling.domain..",
            "..episode.domain..", "..prescription.domain..", "..referral.domain..", "..legacy.domain..",
            "..insights.domain.."};

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.redsaludandina");
    }

    @Test
    @DisplayName("Los comandos no devuelven datos")
    void commandHandlersReturnNothing() {
        methods().that().areDeclaredInClassesThat().implement(CommandHandler.class).and().haveName("handle")
                .should().haveRawReturnType(void.class)
                .check(classes);
    }

    @Test
    @DisplayName("Las consultas no tocan el lado de escritura (agregados, repositorios, outbox, transacción)")
    void queriesDoNotTouchTheWriteSide() {
        noClasses().that().implement(QueryHandler.class)
                .should().dependOnClassesThat().resideInAnyPackage(CONTEXT_DOMAINS)
                .orShould().dependOnClassesThat().resideInAnyPackage("..application.command..",
                        "..shared.application.messaging..")
                .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                .orShould().dependOnClassesThat().haveSimpleName("UnitOfWork")
                .check(classes);
    }

    @Test
    @DisplayName("Los modelos de lectura no dependen del dominio")
    void readModelsDoNotDependOnTheDomain() {
        noClasses().that().resideInAPackage("..application.query..")
                .should().dependOnClassesThat().resideInAnyPackage(CONTEXT_DOMAINS)
                .check(classes);
    }

    @Test
    @DisplayName("El dominio es puro: ni Spring, ni JPA, ni otras capas")
    void domainIsPure() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..",
                        "..application..", "..infrastructure..")
                .check(classes);
    }

    @Test
    @DisplayName("La aplicación no depende de frameworks ni de infraestructura")
    void applicationIsFrameworkFree() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..",
                        "..infrastructure..")
                .check(classes);
    }

    @Test
    @DisplayName("Los contextos no se conocen entre sí, salvo el OHS de Identidad y el núcleo compartido")
    void contextsOnlyMeetThroughPublishedContracts() {
        String[] contexts = {"scheduling", "episode", "prescription", "referral", "legacy", "insights"};
        for (String context : contexts) {
            noClasses().that().resideInAPackage("com.redsaludandina." + context + "..")
                    .should().dependOnClassesThat(SlicesOf.otherContextInternals(context))
                    .check(classes);
        }
    }

    @Test
    @DisplayName("Los módulos no tienen dependencias cíclicas")
    void noCyclesBetweenModules() {
        SlicesRuleDefinition.slices().matching("com.redsaludandina.(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test
    @DisplayName("Los adaptadores JPA viven en infraestructura")
    void jpaEntitiesLiveInInfrastructure() {
        classes().that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..infrastructure..")
                .check(classes);
    }
}
