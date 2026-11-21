package com.redsaludandina.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Frontera de la capa anticorrupción (ACL) con el legado clínico (Caso §3 y §7, ADR-02). */
class AntiCorruptionLayerTest {

    private static final String BASE = "com.redsaludandina";
    private static final String LEGACY = "..legacy..";
    private static final String LEGACY_DOMAIN = "..legacy.domain..";
    private static final String LEGACY_APPLICATION = "..legacy.application..";
    private static final String LEGACY_INFRASTRUCTURE = "..legacy.infrastructure..";
    private static final String ACL = "..legacy.infrastructure.acl..";
    private static final String ACL_ADAPTER = "..legacy.infrastructure.acl.adapter..";
    private static final String ACL_LEGACY_MODEL = "..legacy.infrastructure.acl.model..";
    private static final String ACL_CLIENTS = "..legacy.infrastructure.acl.client..";
    private static final String[] OTHER_MODULES = {
            BASE + ".identity..", BASE + ".scheduling..", BASE + ".episode..", BASE + ".prescription..",
            BASE + ".referral..", BASE + ".insights.."};

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("El modelo y los clientes del legado no salen de la ACL")
    void legacyModelStaysInsideTheAcl() {
        noClasses().that().resideOutsideOfPackage(ACL)
                .should().dependOnClassesThat().resideInAnyPackage(ACL_LEGACY_MODEL, ACL_CLIENTS)
                .because("solo la capa anticorrupción conoce el modelo del legado")
                .check(classes);
    }

    @Test
    @DisplayName("Nada fuera de la infraestructura de legado usa la ACL")
    void aclIsOnlyUsedFromLegacyInfrastructure() {
        noClasses().that().resideOutsideOfPackage(LEGACY_INFRASTRUCTURE)
                .should().dependOnClassesThat().resideInAPackage(ACL)
                .because("el núcleo llega al legado por puertos, no por la ACL")
                .check(classes);
    }

    @Test
    @DisplayName("El dominio y la aplicación de legado no dependen de infraestructura")
    void legacyCoreDoesNotDependOnInfrastructure() {
        noClasses().that().resideInAnyPackage(LEGACY_DOMAIN, LEGACY_APPLICATION)
                .should().dependOnClassesThat().resideInAPackage(LEGACY_INFRASTRUCTURE)
                .check(classes);
    }

    @Test
    @DisplayName("Otros contextos solo usan la consulta pública (OHS) del historial")
    void otherContextsOnlyUseTheHistoryQuery() {
        noClasses().that().resideOutsideOfPackage(LEGACY)
                .should().dependOnClassesThat(legacyInternals())
                .check(classes);
    }

    private static DescribedPredicate<JavaClass> legacyInternals() {
        String legacy = BASE + ".legacy";
        String ohs = legacy + ".application.query";
        return DescribedPredicate.describe("pertenecen a legacy y no son su OHS (" + ohs + ")",
                type -> type.getPackageName().startsWith(legacy) && !type.getPackageName().startsWith(ohs));
    }

    @Test
    @DisplayName("Solo el adaptador de la ACL implementa LegacyHistorySource")
    void onlyTheAclAdapterImplementsTheLegacyPort() {
        classes().that().implement(BASE + ".legacy.application.port.LegacyHistorySource")
                .should().resideInAPackage(ACL_ADAPTER)
                .check(classes);
    }

    @Test
    @DisplayName("La ACL no depende de otros contextos")
    void aclDoesNotDependOnOtherContexts() {
        noClasses().that().resideInAPackage(ACL)
                .should().dependOnClassesThat().resideInAnyPackage(OTHER_MODULES)
                .check(classes);
    }

    @Test
    @DisplayName("Los clientes y el modelo del legado no conocen el modelo canónico")
    void legacyClientsDoNotKnowTheCanonicalModel() {
        noClasses().that().resideInAnyPackage(ACL_CLIENTS, ACL_LEGACY_MODEL)
                .should().dependOnClassesThat().resideInAnyPackage(LEGACY_DOMAIN, LEGACY_APPLICATION)
                .because("la traducción vive solo en el traductor y el adaptador")
                .check(classes);
    }
}
