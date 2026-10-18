package com.bankingsystem.repository;

import com.bankingsystem.entity.AuditEvent;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for AuditEvent.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

  @Query(
      "SELECT a FROM AuditEvent a WHERE "
          + "(:actorId IS NULL OR a.actorId = :actorId) AND "
          + "(:action IS NULL OR :action = '' OR a.action = :action) AND "
          + "(:from IS NULL OR a.createdAt >= :from) AND "
          + "(:to IS NULL OR a.createdAt <= :to)")
  Page<AuditEvent> searchAuditEvents(
      @Param("actorId") Long actorId,
      @Param("action") String action,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);
}
