package com.commerce.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(
        packages = "com.commerce",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class JpaBoundaryTest {

    private static final String[] JPA_ASSOCIATION_ANNOTATIONS = {
            "jakarta.persistence.ManyToOne",
            "jakarta.persistence.OneToMany",
            "jakarta.persistence.OneToOne",
            "jakarta.persistence.ManyToMany",
            "jakarta.persistence.JoinColumn",
            "jakarta.persistence.JoinTable",
    };

    @ArchTest
    static final ArchRule 도메인은_jakarta_persistence를_import하지_않는다 =
            noClasses().that().resideInAPackage("com.commerce.domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                    .because("도메인 모델은 순수 자바. JPA 어노테이션 금지")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule JPA_연관_어노테이션을_필드에_사용하지_않는다 =
            noFields().should().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[0])
                    .orShould().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[1])
                    .orShould().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[2])
                    .orShould().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[3])
                    .orShould().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[4])
                    .orShould().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[5])
                    .because("Aggregate 경계 보호. 엔티티 간 관계는 ID로만 보유")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule JPA_연관_어노테이션을_메서드에_사용하지_않는다 =
            noMethods().should().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[0])
                    .orShould().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[1])
                    .orShould().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[2])
                    .orShould().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[3])
                    .orShould().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[4])
                    .orShould().beAnnotatedWith(JPA_ASSOCIATION_ANNOTATIONS[5])
                    .because("Aggregate 경계 보호. 엔티티 간 관계는 ID로만 보유")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule Entity_어노테이션_클래스는_infrastructure에만_위치한다 =
            classes().that().areAnnotatedWith("jakarta.persistence.Entity")
                    .should().resideInAPackage("com.commerce.infrastructure..")
                    .because("@Entity 매핑은 infrastructure 패키지 안에서만 등장")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule Table_어노테이션_클래스는_infrastructure에만_위치한다 =
            classes().that().areAnnotatedWith("jakarta.persistence.Table")
                    .should().resideInAPackage("com.commerce.infrastructure..")
                    .because("@Table 매핑은 infrastructure 패키지 안에서만 등장")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule JpaRepository는_5계층_중_infrastructure에서만_사용한다 =
            noClasses().that().resideInAnyPackage(
                            "com.commerce.interfaces..",
                            "com.commerce.application..",
                            "com.commerce.domain..")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("org.springframework.data.jpa.repository.JpaRepository")
                    .because("JpaRepository는 infrastructure 내부에 숨긴다")
                    .allowEmptyShould(true);
}
