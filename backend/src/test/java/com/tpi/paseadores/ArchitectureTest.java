package com.tpi.paseadores;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture rules, checked on every `./mvnw test` and in CI. A failure names the class
 * and the dependency that broke the rule. Only production code is analyzed: tests may
 * wire repositories directly.
 */
@AnalyzeClasses(packages = "com.tpi.paseadores", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.tpi.paseadores.";

    // a feature uses only its own repositories; data from another feature goes through that feature's service
    @ArchTest
    static final ArchRule repositories_only_used_within_their_feature =
            classes().should(onlyUseRepositoriesOfTheirOwnFeature());

    // controller -> service -> repository: a controller never skips the service layer
    @ArchTest
    static final ArchRule controllers_do_not_use_repositories =
            noClasses().that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().areAssignableTo(Repository.class);

    // the API speaks DTOs only: JPA entities never reach a controller
    @ArchTest
    static final ArchRule controllers_do_not_use_entities =
            noClasses().that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().areAnnotatedWith(Entity.class);

    // services know nothing about HTTP (no ResponseEntity, no HttpServletRequest)
    @ArchTest
    static final ArchRule services_do_not_know_http =
            noClasses().that().haveSimpleNameEndingWith("Service")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework.http..", "org.springframework.web..", "jakarta.servlet..");

    // shared is the base every feature builds on, so it can't depend on any of them
    @ArchTest
    static final ArchRule shared_does_not_depend_on_features =
            noClasses().that().resideInAPackage(ROOT + "shared..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(ROOT + "auth..", ROOT + "user..", ROOT + "zone..",
                            ROOT + "pet..", ROOT + "walk..", ROOT + "review..");

    // no cycles between features (e.g. walk -> review -> walk)
    @ArchTest
    static final ArchRule features_are_free_of_cycles =
            slices().matching(ROOT + "(*)..").should().beFreeOfCycles();

    private static ArchCondition<JavaClass> onlyUseRepositoriesOfTheirOwnFeature() {
        return new ArchCondition<>("only use repositories of their own feature") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                Set<JavaClass> reported = new HashSet<>();
                for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    boolean foreignRepository = target.getPackageName().startsWith(ROOT)
                            && target.isAssignableTo(Repository.class)
                            && !feature(target).equals(feature(clazz));
                    if (foreignRepository && reported.add(target)) {
                        events.add(SimpleConditionEvent.violated(clazz, clazz.getName()
                                + " usa " + target.getSimpleName() + " de la feature '" + feature(target)
                                + "': usar el service de esa feature"));
                    }
                }
            }
        };
    }

    // com.tpi.paseadores.walk.dto.X -> "walk"
    private static String feature(JavaClass clazz) {
        String pkg = clazz.getPackageName();
        if (!pkg.startsWith(ROOT)) {
            return "";
        }
        String rest = pkg.substring(ROOT.length());
        int dot = rest.indexOf('.');
        return dot == -1 ? rest : rest.substring(0, dot);
    }
}
