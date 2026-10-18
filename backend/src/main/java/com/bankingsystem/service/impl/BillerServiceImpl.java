package com.bankingsystem.service.impl;

import com.bankingsystem.dto.request.BillerRequest;
import com.bankingsystem.dto.response.BillerResponse;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.Biller;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.BillerStatus;
import com.bankingsystem.exception.BusinessRuleException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.BillerRepository;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.AuditService;
import com.bankingsystem.service.BillerService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillerServiceImpl implements BillerService {

  private final BillerRepository billerRepository;
  private final OwnershipGuard ownershipGuard;
  private final AuditService auditService;

  public BillerServiceImpl(
      BillerRepository billerRepository,
      OwnershipGuard ownershipGuard,
      AuditService auditService) {
    this.billerRepository = billerRepository;
    this.ownershipGuard = ownershipGuard;
    this.auditService = auditService;
  }

  @Override
  @Transactional
  public BillerResponse createBiller(BillerRequest request) {
    AppUser admin = ownershipGuard.getCurrentUser();
    String code = request.code().trim().toUpperCase();

    if (billerRepository.existsByCodeIgnoreCase(code)) {
      throw new BusinessRuleException("A biller with code " + code + " already exists.");
    }

    Biller biller = new Biller(
        code,
        request.name().trim(),
        request.category().trim(),
        request.referenceFormat() != null ? request.referenceFormat().trim() : null,
        BillerStatus.ACTIVE
    );

    biller = billerRepository.save(biller);

    auditService.recordEvent(
        admin.getId(),
        admin.getRole(),
        "BILLER_CREATED",
        "Biller",
        biller.getId(),
        AuditOutcome.SUCCESS,
        "Created biller " + biller.getName() + " (" + biller.getCode() + ")",
        null
    );

    return mapToResponse(biller);
  }

  @Override
  @Transactional
  public BillerResponse updateBiller(Long id, BillerRequest request) {
    AppUser admin = ownershipGuard.getCurrentUser();
    Biller biller = billerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Biller", id));

    String newCode = request.code().trim().toUpperCase();
    if (!biller.getCode().equalsIgnoreCase(newCode) && billerRepository.existsByCodeIgnoreCase(newCode)) {
      throw new BusinessRuleException("A biller with code " + newCode + " already exists.");
    }

    biller.setCode(newCode);
    biller.setName(request.name().trim());
    biller.setCategory(request.category().trim());
    biller.setReferenceFormat(request.referenceFormat() != null ? request.referenceFormat().trim() : null);

    biller = billerRepository.save(biller);

    auditService.recordEvent(
        admin.getId(),
        admin.getRole(),
        "BILLER_UPDATED",
        "Biller",
        biller.getId(),
        AuditOutcome.SUCCESS,
        "Updated biller " + biller.getName() + " (" + biller.getCode() + ")",
        null
    );

    return mapToResponse(biller);
  }

  @Override
  @Transactional
  public BillerResponse updateBillerStatus(Long id, BillerStatus status) {
    AppUser admin = ownershipGuard.getCurrentUser();
    Biller biller = billerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Biller", id));

    biller.setStatus(status);
    biller = billerRepository.save(biller);

    auditService.recordEvent(
        admin.getId(),
        admin.getRole(),
        "BILLER_STATUS_CHANGED",
        "Biller",
        biller.getId(),
        AuditOutcome.SUCCESS,
        "Changed biller " + biller.getCode() + " status to " + status,
        null
    );

    return mapToResponse(biller);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BillerResponse> getActiveBillers() {
    return billerRepository.findByStatusOrderByNameAsc(BillerStatus.ACTIVE)
        .stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BillerResponse> getAllBillers(Pageable pageable) {
    return billerRepository.findAllByOrderByNameAsc(pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public BillerResponse getBillerById(Long id) {
    return billerRepository.findById(id)
        .map(this::mapToResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Biller", id));
  }

  private BillerResponse mapToResponse(Biller b) {
    return new BillerResponse(
        b.getId(),
        b.getCode(),
        b.getName(),
        b.getCategory(),
        b.getReferenceFormat(),
        b.getStatus(),
        b.getCreatedAt()
    );
  }
}
