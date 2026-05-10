package com.histr.api.repository;

import com.histr.api.dto.CategorySummaryRow;
import com.histr.api.dto.StatsRow;
import com.histr.api.model.Document;
import com.histr.api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query(
            value = """
        SELECT *
        FROM documents d
        WHERE
        (:searchParam = '' OR (d.description LIKE CONCAT('%', :searchParam, '%')) OR (d.recepient LIKE CONCAT('%', :searchParam, '%')))
        AND (CAST(:startDate AS timestamptz) IS NULL OR d.created_at >= CAST(:startDate AS timestamptz))
        AND (CAST(:endDate AS timestamptz) IS NULL OR d.created_at <= CAST(:endDate AS timestamptz))
        AND d.user_id = :#{#user.id}
        ORDER BY d.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM documents d
        WHERE 
        (:searchParam = '' OR (d.description LIKE CONCAT('%', :searchParam, '%')) OR (d.recepient LIKE CONCAT('%', :searchParam, '%')))
        AND (CAST(:startDate AS timestamptz) IS NULL OR d.created_at >= CAST(:startDate AS timestamptz))
        AND (CAST(:endDate AS timestamptz) IS NULL OR d.created_at <= CAST(:endDate AS timestamptz))
        AND d.user_id = :#{#user.id}
        """,
            nativeQuery = true
    )
    Page<Document> findFiltered(
            @Param("searchParam") String searchParam,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("user") User user,
            Pageable pageable
    );

    @Query("""
       SELECT COUNT(d) FROM Document d
       WHERE (:search IS NULL OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%'))
                                   OR LOWER(d.recipient) LIKE LOWER(CONCAT('%', :search, '%')))
       AND (CAST(:startDate AS TIMESTAMP) IS NULL OR d.createdAt >= :startDate)
       AND (CAST(:endDate AS TIMESTAMP) IS NULL OR d.createdAt <= :endDate)
       """)
    long countFiltered(
            @Param("search") String search,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    @Query("""
            SELECT new com.histr.api.dto.StatsRow(
              SUM(CASE WHEN d.amount > 0 THEN d.amount END),
              SUM(CASE WHEN d.amount < 0 THEN d.amount END),
              SUM(d.amount),
              COUNT(d)
            ) FROM Document d
            WHERE (:search = '' OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%'))
                                   OR LOWER(d.recipient) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (CAST(:startDate AS TIMESTAMP) IS NULL OR d.createdAt >= :startDate)
            AND (CAST(:endDate AS TIMESTAMP) IS NULL OR d.createdAt <= :endDate)
            AND d.user = :user
            """)
    StatsRow computeStats(
            @Param("search") String search,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("user") User user
    );

    @Query(value = """
            SELECT c.name as categoryName, SUM(d.amount) as total, COUNT(d) as count
            FROM documents d JOIN categories c ON d.category_id = c.id
            WHERE (:search = '' OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%'))
                                   OR LOWER(d.recepient) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (CAST(:startDate AS TIMESTAMPTZ) IS NULL OR d.created_at >= :startDate)
              AND (CAST(:endDate AS TIMESTAMPTZ) IS NULL OR d.created_at <= :endDate)
            AND d.user_id = :userId
            GROUP BY c.id, c.name
            ORDER BY SUM(d.amount) DESC
            """, nativeQuery = true)
    List<CategorySummaryRow> categorySummary(
            @Param("search") String search,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("userId") String userId
    );
}
