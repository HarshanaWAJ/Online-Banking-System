package com.bankingsystem.service.impl;

import com.bankingsystem.entity.AuditEvent;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.Role;
import com.bankingsystem.repository.AuditEventRepository;
import com.bankingsystem.service.AuditService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit service implementation.
 * Logs privileged operations in a separate transaction so failures don't drop audit records.
 */
@Service
public class AuditServiceImpl implements AuditService {

  private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

  private final AuditEventRepository auditEventRepository;

  public AuditServiceImpl(AuditEventRepository auditEventRepository) {
    this.auditEventRepository = auditEventRepository;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordEvent(
      Long actorId,
      Role actorRole,
      String action,
      String entityType,
      Long entityId,
      AuditOutcome outcome,
      String details,
      String ipAddress) {
    try {
      var event = new AuditEvent(
          actorId,
          actorRole,
          action,
          entityType,
          entityId,
          outcome,
          details != null && details.length() > 1000 ? details.substring(0, 1000) : details,
          ipAddress);
      auditEventRepository.save(event);
    } catch (Exception ex) {
      log.error("Failed to record audit event for action {}: {}", action, ex.getMessage());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AuditEvent> searchAuditEvents(Long actorId, String action, Instant from, Instant to, Pageable pageable) {
    return auditEventRepository.searchAuditEvents(actorId, action, from, to, pageable);
  }
}
