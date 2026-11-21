package com.redsaludandina.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica la frontera de la Capa Anticorrupcion (ACL) entre el dominio y el legado clinico
 * (Caso RedSalud Andina §3 y §7, ADR-02; vista «DDD 5 · ACL Legado»).
 *
 * <p>La estructura todavia no tiene clases, por eso cada regla usa {@code allowEmptyShould(true)}:
 * pasa mientras no haya nada que revisar y empieza a vigilar en cuanto aparezca la primera clase.
 */
class CapaAnticorrupcionLegadoTest {

    private static final String BASE = "com.redsaludandina";

    private static final String LEGADO = "..legado..";
    private static final String LEGADO_DOMINIO = "..legado.dominio..";
    private static final String LEGADO_APLICACION = "..legado.aplicacion..";
    private static final String LEGADO_INFRAESTRUCTURA = "..legado.infraestructura..";

    private static final String ACL = "..legado.infraestructura.acl..";
    private static final String ACL_ADAPTADOR = "..legado.infraestructura.acl.adaptador..";
    private static final String ACL_MODELO_LEGADO = "..legado.infraestructura.acl.modelo..";
    private static final String ACL_CLIENTES = "..legado.infraestructura.acl.cliente..";

    private static final String HISTORIAL_CLINICO_PORT =
            BASE + ".legado.aplicacion.puerto.salida.HistorialClinicoPort";

    private static final String[] OTROS_MODULOS = {
            BASE + ".agenda..",
            BASE + ".episodio..",
            BASE + ".receta..",
            BASE + ".interconsulta..",
            BASE + ".identidad..",
            BASE + ".auditoria..",
            BASE + ".analitica..",
            BASE + ".sincronizacion.."
    };

    private static JavaClasses clases;

    @BeforeAll
    static void importarClases() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("El modelo y los clientes del legado no salen de la ACL")
    void elModeloDelLegadoNoSaleDeLaAcl() {
        ArchRule regla = noClasses()
                .that().resideOutsideOfPackage(ACL)
                .should().dependOnClassesThat().resideInAnyPackage(ACL_MODELO_LEGADO, ACL_CLIENTES)
                .because("solo la capa anticorrupcion conoce el modelo del legado (seccion 3 y 7)")
                .allowEmptyShould(true);

        regla.check(clases);
    }

    @Test
    @DisplayName("Nada fuera de la infraestructura de legado usa la ACL directamente")
    void laAclSoloSeUsaDesdeLaInfraestructuraDeLegado() {
        ArchRule regla = noClasses()
                .that().resideOutsideOfPackage(LEGADO_INFRAESTRUCTURA)
                .should().dependOnClassesThat().resideInAPackage(ACL)
                .because("el dominio y los demas contextos llegan al historial por puertos, no por la ACL")
                .allowEmptyShould(true);

        regla.check(clases);
    }

    @Test
    @DisplayName("El dominio y la aplicacion de legado no dependen de infraestructura")
    void elNucleoDeLegadoNoDependeDeInfraestructura() {
        ArchRule regla = noClasses()
                .that().resideInAnyPackage(LEGADO_DOMINIO, LEGADO_APLICACION)
                .should().dependOnClassesThat().resideInAPackage(LEGADO_INFRAESTRUCTURA)
                .because("regla de dependencia hexagonal: los adaptadores dependen del nucleo")
                .allowEmptyShould(true);

        regla.check(clases);
    }

    @Test
    @DisplayName("Otros contextos solo usan el OHS del historial")
    void otrosContextosSoloUsanElOhsDelHistorial() {
        ArchRule regla = noClasses()
                .that().resideOutsideOfPackage(LEGADO)
                .should().dependOnClassesThat(internosDeLegado())
                .because("el historial se consume por su Servicio de Host Abierto, nunca por sus internos")
                .allowEmptyShould(true);

        regla.check(clases);
    }

    /** Clases de legado que no forman parte de su OHS ({@code legado.aplicacion.puerto.entrada}). */
    private static DescribedPredicate<JavaClass> internosDeLegado() {
        String legado = BASE + ".legado";
        String ohs = legado + ".aplicacion.puerto.entrada";
        return DescribedPredicate.describe(
                "pertenecen a legado y no son su OHS (" + ohs + ")",
                clase -> {
                    String paquete = clase.getPackageName();
                    boolean esDeLegado = paquete.equals(legado) || paquete.startsWith(legado + ".");
                    boolean esOhs = paquete.equals(ohs) || paquete.startsWith(ohs + ".");
                    return esDeLegado && !esOhs;
                });
    }

    @Test
    @DisplayName("Solo la ACL implementa HistorialClinicoPort")
    void soloLaAclImplementaElPuertoDelHistorial() {
        ArchRule regla = classes()
                .that().implement(HISTORIAL_CLINICO_PORT)
                .should().resideInAPackage(ACL_ADAPTADOR)
                .because("el AdaptadorHistorial de la ACL es la unica implementacion del puerto (seccion 3)")
                .allowEmptyShould(true);

        regla.check(clases);
    }

    @Test
    @DisplayName("La ACL no depende de otros contextos")
    void laAclNoDependeDeOtrosContextos() {
        ArchRule regla = noClasses()
                .that().resideInAPackage(ACL)
                .should().dependOnClassesThat().resideInAnyPackage(OTROS_MODULOS)
                .because("la ACL solo traduce el legado al modelo canonico de Integracion legado")
                .allowEmptyShould(true);

        regla.check(clases);
    }

    @Test
    @DisplayName("Los clientes y el modelo del legado no conocen el modelo canonico")
    void losClientesDelLegadoNoConocenElModeloCanonico() {
        ArchRule regla = noClasses()
                .that().resideInAnyPackage(ACL_CLIENTES, ACL_MODELO_LEGADO)
                .should().dependOnClassesThat().resideInAnyPackage(LEGADO_DOMINIO, LEGADO_APLICACION)
                .because("la traduccion al modelo canonico vive solo en el traductor y el adaptador (seccion 7)")
                .allowEmptyShould(true);

        regla.check(clases);
    }
}
