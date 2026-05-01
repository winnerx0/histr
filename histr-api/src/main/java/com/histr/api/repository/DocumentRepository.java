package com.histr.api.repository;

import com.histr.api.dto.CategorySummaryRow;
import com.histr.api.dto.StatsRow;
import com.histr.api.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("""
           SELECT d FROM Document d JOIN FETCH d.category c
            WHERE (:search = '' OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%'))
                                   OR LOWER(d.recipient) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (CAST(:startDate AS TIMESTAMP) IS NULL OR d.createdAt >= :startDate)
            AND (CAST(:endDate AS TIMESTAMP) IS NULL OR d.createdAt <= :endDate)
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findFiltered(
            @Param("search") String search,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
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
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
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
            """)
    StatsRow computeStats(
            @Param("search") String search,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query("""
            SELECT new com.histr.api.dto.CategorySummaryRow(c.name, SUM(d.amount), COUNT(d))
            FROM Document d JOIN d.category c
            WHERE (:search = '' OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%'))
                                   OR LOWER(d.recipient) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (CAST(:startDate AS TIMESTAMP) IS NULL OR d.createdAt >= :startDate)
              AND (CAST(:endDate AS TIMESTAMP) IS NULL OR d.createdAt <= :endDate)
            GROUP BY c.id, c.name
            ORDER BY SUM(d.amount) DESC
            """)
    List<CategorySummaryRow> categorySummary(
            @Param("search") String search,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );
}
