package com.histr.api.repository;

import com.histr.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO categories (id, name, keywords, embeddings, created_at)
            VALUES (gen_random_uuid(), :name, :keywords, CAST(:embeddings AS vector), NOW())
            """, nativeQuery = true)
    void insertWithEmbeddings(
            @Param("name") String name,
            @Param("keywords") String keywords,
            @Param("embeddings") String embeddings
    );

    @Query(value = "SELECT id::text FROM categories ORDER BY embeddings <#> CAST(:q AS vector) LIMIT 1",
            nativeQuery = true)
    String findNearestId(@Param("q") String queryVector);
}
