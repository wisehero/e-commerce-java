package com.commerce.infrastructure.category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {

    boolean existsByParentIdAndName(Long parentId, String name);

    boolean existsByParentIdIsNullAndName(String name);

    boolean existsByParentId(Long parentId);

    @Query("SELECT c.id FROM CategoryJpaEntity c WHERE c.parentId = :parentId")
    List<Long> findIdsByParentId(@Param("parentId") Long parentId);

    @Query("SELECT c.id FROM CategoryJpaEntity c WHERE c.parentId IN :parentIds")
    List<Long> findIdsByParentIdIn(@Param("parentIds") List<Long> parentIds);
}
