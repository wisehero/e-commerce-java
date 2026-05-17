package com.commerce.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.commerce",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class LayerDependencyTest {

    @ArchTest
    static final ArchRule 도메인은_다른_계층을_참조하지_않는다 =
            noClasses().that().resideInAPackage("com.commerce.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.commerce.interfaces..",
                            "com.commerce.application..",
                            "com.commerce.infrastructure..")
                    .because("도메인은 순수 자바. 다른 계층에 의존하지 않는다")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 애플리케이션은_인터페이스나_인프라를_참조하지_않는다 =
            noClasses().that().resideInAPackage("com.commerce.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.commerce.interfaces..",
                            "com.commerce.infrastructure..")
                    .because("application은 domain만 알고 infrastructure 구현체에 직접 의존하지 않는다")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 인터페이스는_도메인이나_인프라를_참조하지_않는다 =
            noClasses().that().resideInAPackage("com.commerce.interfaces..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.commerce.domain..",
                            "com.commerce.infrastructure..")
                    .because("interfaces는 application의 Facade와 Info만 사용한다")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 인프라는_인터페이스나_애플리케이션을_참조하지_않는다 =
            noClasses().that().resideInAPackage("com.commerce.infrastructure..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.commerce.interfaces..",
                            "com.commerce.application..")
                    .because("infrastructure는 domain의 Repository 인터페이스만 구현한다")
                    .allowEmptyShould(true);
}
