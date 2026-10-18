package com.bankingsystem.service.impl;

import com.bankingsystem.dto.request.ServiceRequestCreateDto;
import com.bankingsystem.dto.request.ServiceRequestResolveDto;
import com.bankingsystem.dto.response.ServiceRequestResponse;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.Customer;
import com.bankingsystem.entity.ServiceRequest;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.NotificationType;
import com.bankingsystem.enums.Role;
import com.bankingsystem.enums.ServiceRequestStatus;
import com.bankingsystem.enums.ServiceRequestType;
import com.bankingsystem.exception.BusinessRuleException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.CustomerRepository;
import com.bankingsystem.repository.ServiceRequestRepository;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.AuditService;
import com.bankingsystem.service.NotificationService;
import com.bankingsystem.service.ServiceRequestService;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceRequestServiceImpl implements ServiceRequestService {

  private final ServiceRequestRepository serviceRequestRepository;
  private final CustomerRepository customerRepository;
  private final OwnershipGuard ownershipGuard;
  private final AuditService auditService;
  private final NotificationService notificationService;

  public ServiceRequestServiceImpl(
      ServiceRequestRepository serviceRequestRepository,
      CustomerRepository customerRepository,
      OwnershipGuard ownershipGuard,
      AuditService auditService,
      NotificationService notificationService) {
    this.serviceRequestRepository = serviceRequestRepository;
    this.customerRepository = customerRepository;
    this.ownershipGuard = ownershipGuard;
    this.auditService = auditService;
    this.notificationService = notificationService;
  }

  @Override
  @Transactional
  public ServiceRequestResponse createRequest(ServiceRequestCreateDto request) {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new BusinessRuleException("Only customers with registered profiles can open tickets."));

    ServiceRequest sr = new ServiceRequest(
        customer,
        request.type(),
        request.subject().trim(),
        request.description().trim(),
        request.relatedAccountId()
    );

    sr = serviceRequestRepository.save(sr);

    auditService.recordEvent(
        user.getId(),
        user.getRole(),
        "SERVICE_REQUEST_CREATED",
        "ServiceRequest",
        sr.getId(),
        AuditOutcome.SUCCESS,
        "Customer created support request: " + sr.getSubject(),
        null
    );

    return mapToResponse(sr);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ServiceRequestResponse> getMyRequests(Pageable pageable) {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

    return serviceRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable)
        .map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public ServiceRequestResponse getRequestById(Long id) {
    AppUser user = ownershipGuard.getCurrentUser();
    ServiceRequest sr = serviceRequestRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", id));

    if (user.getRole() == Role.CUSTOMER && !sr.getCustomer().getUser().getId().equals(user.getId())) {
      throw new ResourceNotFoundException("ServiceRequest", id);
    }

    return mapToResponse(sr);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ServiceRequestResponse> searchRequests(
      ServiceRequestStatus status,
      ServiceRequestType type,
      Pageable pageable) {
    return serviceRequestRepository.searchRequests(status, type, pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional
  public ServiceRequestResponse assignToMe(Long id) {
    AppUser staff = ownershipGuard.getCurrentUser();
    ServiceRequest sr = serviceRequestRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", id));

    if (sr.getStatus() == ServiceRequestStatus.RESOLVED || sr.getStatus() == ServiceRequestStatus.REJECTED) {
      throw new BusinessRuleException("Cannot assign a closed service request.");
    }

    sr.setAssignedStaff(staff);
    sr.setStatus(ServiceRequestStatus.UNDER_REVIEW);
    sr = serviceRequestRepository.save(sr);

    auditService.recordEvent(
        staff.getId(),
        staff.getRole(),
        "SERVICE_REQUEST_ASSIGNED",
        "ServiceRequest",
        sr.getId(),
        AuditOutcome.SUCCESS,
        "Staff member took ownership of request #" + sr.getId(),
        null
    );

    return mapToResponse(sr);
  }

  @Override
  @Transactional
  public ServiceRequestResponse resolveRequest(Long id, ServiceRequestResolveDto request) {
    AppUser staff = ownershipGuard.getCurrentUser();
    ServiceRequest sr = serviceRequestRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", id));

    if (sr.getStatus() == ServiceRequestStatus.RESOLVED || sr.getStatus() == ServiceRequestStatus.REJECTED) {
      throw new BusinessRuleException("Request has already been processed.");
    }

    sr.setStatus(request.status());
    sr.setResolutionNotes(request.resolutionNotes() != null ? request.resolutionNotes().trim() : null);
    sr.setAssignedStaff(staff);
    sr.setResolvedAt(Instant.now());
    sr = serviceRequestRepository.save(sr);

    // Notify customer
    notificationService.createNotification(
        sr.getCustomer().getUser(),
        NotificationType.SERVICE_REQUEST_UPDATE,
        String.format("Your support ticket #%d has been updated to %s", sr.getId(), request.status()),
        "/service-requests/" + sr.getId()
    );

    auditService.recordEvent(
        staff.getId(),
        staff.getRole(),
        "SERVICE_REQUEST_RESOLVED",
        "ServiceRequest",
        sr.getId(),
        AuditOutcome.SUCCESS,
        "Service request #" + sr.getId() + " marked as " + request.status(),
        null
    );

    return mapToResponse(sr);
  }

  private ServiceRequestResponse mapToResponse(ServiceRequest sr) {
    return new ServiceRequestResponse(
        sr.getId(),
        sr.getCustomer().getId(),
        sr.getCustomer().getFullName(),
        sr.getCustomer().getUser().getEmail(),
        sr.getType(),
        sr.getStatus(),
        sr.getSubject(),
        sr.getDescription(),
        sr.getRelatedAccountId(),
        sr.getResolutionNotes(),
        sr.getAssignedStaff() != null ? sr.getAssignedStaff().getId() : null,
        sr.getAssignedStaff() != null ? sr.getAssignedStaff().getEmail() : null,
        sr.getResolvedAt(),
        sr.getCreatedAt()
    );
  }
}
