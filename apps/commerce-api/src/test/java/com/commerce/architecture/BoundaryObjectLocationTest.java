package com.commerce.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(
        packages = "com.commerce",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class BoundaryObjectLocationTest {

    /**
     * 룰 대상은 5계층(interfaces/application/domain/infrastructure) 안의 클래스로만 한정.
     * 모듈·지원 패키지(com.commerce.config.., com.commerce.support..)는 자연스럽게 제외된다.
     */
    private static final String[] FIVE_LAYER_PACKAGES = {
            "com.commerce.interfaces..",
            "com.commerce.application..",
            "com.commerce.domain..",
            "com.commerce.infrastructure..",
    };

    @ArchTest
    static final ArchRule Request_클래스는_interfaces에만_위치한다 =
            classes().that().haveSimpleNameEndingWith("Request")
                    .and().resideInAnyPackage(FIVE_LAYER_PACKAGES)
                    .should().resideInAPackage("com.commerce.interfaces..")
                    .because("HTTP 입력 경계 객체(*Request)는 interfaces 계층에만 위치")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule Response_클래스는_interfaces에만_위치한다 =
            classes().that().haveSimpleNameEndingWith("Response")
                    .and().resideInAnyPackage(FIVE_LAYER_PACKAGES)
                    .should().resideInAPackage("com.commerce.interfaces..")
                    .because("HTTP 출력 경계 객체(*Response)는 interfaces 계층에만 위치")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule Command_클래스는_application에만_위치한다 =
            classes().that().haveSimpleNameEndingWith("Command")
                    .and().resideInAnyPackage(FIVE_LAYER_PACKAGES)
                    .should().resideInAPackage("com.commerce.application..")
                    .because("Use Case 입력 경계 객체(*Command)는 application 계층에만 위치")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule Criteria_클래스는_application에만_위치한다 =
            classes().that().haveSimpleNameEndingWith("Criteria")
                    .and().resideInAnyPackage(FIVE_LAYER_PACKAGES)
                    .should().resideInAPackage("com.commerce.application..")
                    .because("Use Case 조회 입력 경계 객체(*Criteria)는 application 계층에만 위치")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule Info_클래스는_application에만_위치한다 =
            classes().that().haveSimpleNameEndingWith("Info")
                    .and().resideInAnyPackage(FIVE_LAYER_PACKAGES)
                    .should().resideInAPackage("com.commerce.application..")
                    .because("Use Case 출력 경계 객체(*Info)는 application 계층에만 위치")
                    .allowEmptyShould(true);
}
