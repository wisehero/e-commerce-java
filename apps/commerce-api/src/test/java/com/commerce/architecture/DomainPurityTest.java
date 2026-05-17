package com.commerce.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(
        packages = "com.commerce",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class DomainPurityTest {

    @ArchTest
    static final ArchRule 도메인의_public_메서드는_set으로_시작하지_않는다 =
            noMethods().that().areDeclaredInClassesThat().resideInAPackage("com.commerce.domain..")
                    .and().arePublic()
                    .should().haveNameStartingWith("set")
                    .because("도메인 모델은 의미 있는 메서드명으로 상태 변경 (public setter 금지)")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 도메인_클래스에_Lombok_Setter_또는_Data_사용_금지 =
            noClasses().that().resideInAPackage("com.commerce.domain..")
                    .should().beAnnotatedWith("lombok.Setter")
                    .orShould().beAnnotatedWith("lombok.Data")
                    .because("도메인은 invariant를 스스로 지킨다 (외부에서 임의 상태 변경 금지)")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 도메인_필드에_Lombok_Setter_사용_금지 =
            noFields().that().areDeclaredInClassesThat().resideInAPackage("com.commerce.domain..")
                    .should().beAnnotatedWith("lombok.Setter")
                    .because("도메인 필드는 의미 있는 메서드로만 변경 (필드 setter 자동 생성 금지)")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 도메인은_Spring_프레임워크를_참조하지_않는다 =
            noClasses().that().resideInAPackage("com.commerce.domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("도메인은 순수 자바. Spring에 의존하지 않는다")
                    .allowEmptyShould(true);
}
