package com.bankingsystem.service;

import com.bankingsystem.entity.AppUser;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;

/**
 * Service managing system audit logs.
 */
public interface AuditService {

  void recordEvent(
      Long actorId,
      Role actorRole,
      String action,
      String entityType,
      Long entityId,
      AuditOutcome outcome,
      String details,
      String ipAddress);

  Page<?> searchAuditEvents(Long actorId, String action, Instant from, Instant to, Pageable pageable);
}
