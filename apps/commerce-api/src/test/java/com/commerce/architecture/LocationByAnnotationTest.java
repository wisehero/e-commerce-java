package com.commerce.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@AnalyzeClasses(
        packages = "com.commerce",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class LocationByAnnotationTest {

    @ArchTest
    static final ArchRule Controller_클래스는_interfaces에만_위치한다 =
            classes().that()
                    .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .or().areAnnotatedWith("org.springframework.stereotype.Controller")
                    .should().resideInAPackage("com.commerce.interfaces..")
                    .because("Controller는 HTTP 경계 — interfaces 계층에만 위치")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule ControllerAdvice_클래스는_interfaces에만_위치한다 =
            classes().that()
                    .areAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
                    .or().areAnnotatedWith("org.springframework.web.bind.annotation.ControllerAdvice")
                    .should().resideInAPackage("com.commerce.interfaces..")
                    .because("ControllerAdvice는 HTTP 경계 — interfaces 계층에만 위치")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule Transactional_클래스는_application에만_위치한다 =
            classes().that()
                    .areAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .should().resideInAPackage("com.commerce.application..")
                    .because("트랜잭션 경계는 application Facade — 다른 계층에 @Transactional 금지")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule Transactional_메서드는_application_클래스_내부에만_있는다 =
            methods().that()
                    .areAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .should().beDeclaredInClassesThat().resideInAPackage("com.commerce.application..")
                    .because("트랜잭션 경계는 application Facade — 다른 계층 메서드에 @Transactional 금지")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule JpaRepository_인터페이스는_infrastructure에만_위치한다 =
            classes().that()
                    .areAssignableTo("org.springframework.data.jpa.repository.JpaRepository")
                    .and().areInterfaces()
                    .and().doNotHaveFullyQualifiedName("org.springframework.data.jpa.repository.JpaRepository")
                    .should().resideInAPackage("com.commerce.infrastructure..")
                    .because("Spring Data JpaRepository 정의는 infrastructure에 숨긴다")
                    .allowEmptyShould(true);
}
