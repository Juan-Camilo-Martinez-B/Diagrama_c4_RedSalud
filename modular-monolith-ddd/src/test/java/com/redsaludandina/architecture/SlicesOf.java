package com.redsaludandina.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import java.util.List;

/** Predicados de ArchUnit sobre los paquetes de los contextos. */
final class SlicesOf {

    private static final String BASE = "com.redsaludandina.";
    private static final List<String> CONTEXTS =
            List.of("identity", "scheduling", "episode", "prescription", "referral", "legacy", "insights");
    /** Lo único de otro contexto que se puede usar: el comando OHS de autorización de Identidad. */
    private static final String IDENTITY_OHS = BASE + "identity.application.command.AuthorizeAccessCommand";

    private SlicesOf() {
    }

    static DescribedPredicate<JavaClass> otherContextInternals(String context) {
        return DescribedPredicate.describe("clases de otro contexto (salvo el OHS de Identidad)",
                type -> belongsToAnotherContext(type.getName(), context) && !type.getName().equals(IDENTITY_OHS));
    }

    private static boolean belongsToAnotherContext(String className, String context) {
        return CONTEXTS.stream().filter(other -> !other.equals(context))
                .anyMatch(other -> className.startsWith(BASE + other + "."));
    }
}
