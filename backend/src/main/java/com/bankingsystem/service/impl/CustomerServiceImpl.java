package com.bankingsystem.service.impl;

import com.bankingsystem.dto.request.UpdateProfileRequest;
import com.bankingsystem.dto.response.CustomerResponse;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.Customer;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.Role;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.CustomerRepository;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.AuditService;
import com.bankingsystem.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer profile service implementation.
 */
@Service
public class CustomerServiceImpl implements CustomerService {

  private final CustomerRepository customerRepository;
  private final OwnershipGuard ownershipGuard;
  private final AuditService auditService;

  public CustomerServiceImpl(
      CustomerRepository customerRepository,
      OwnershipGuard ownershipGuard,
      AuditService auditService) {
    this.customerRepository = customerRepository;
    this.ownershipGuard = ownershipGuard;
    this.auditService = auditService;
  }

  @Override
  @Transactional(readOnly = true)
  public CustomerResponse getMyProfile() {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + user.getId()));
    return mapToResponse(customer);
  }

  @Override
  @Transactional
  public CustomerResponse updateMyProfile(UpdateProfileRequest request) {
    AppUser user = ownershipGuard.getCurrentUser();
    Customer customer = customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

    customer.setPhone(request.phone().trim());
    customer.setAddress(request.address().trim());
    customer = customerRepository.save(customer);

    auditService.recordEvent(
        user.getId(),
        Role.CUSTOMER,
        "CUSTOMER_PROFILE_UPDATED",
        "Customer",
        customer.getId(),
        AuditOutcome.SUCCESS,
        "Profile contact details updated",
        null);

    return mapToResponse(customer);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CustomerResponse> searchCustomers(String search, Pageable pageable) {
    return customerRepository.searchCustomers(search, pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public CustomerResponse getCustomerById(Long customerId) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    return mapToResponse(customer);
  }

  @Override
  @Transactional
  public CustomerResponse toggleCustomerStatus(Long customerId, boolean active) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

    customer.setActive(active);
    customer.getUser().setActive(active);
    customer = customerRepository.save(customer);

    AppUser staff = ownershipGuard.getCurrentUser();
    auditService.recordEvent(
        staff.getId(),
        staff.getRole(),
        active ? "CUSTOMER_ACTIVATED" : "CUSTOMER_DEACTIVATED",
        "Customer",
        customer.getId(),
        AuditOutcome.SUCCESS,
        "Customer status set to: " + (active ? "ACTIVE" : "DEACTIVATED"),
        null);

    return mapToResponse(customer);
  }

  private CustomerResponse mapToResponse(Customer c) {
    return new CustomerResponse(
        c.getId(),
        c.getUser().getId(),
        c.getUser().getEmail(),
        c.getFullName(),
        c.getNic(),
        c.getPhone(),
        c.getAddress(),
        c.getDateOfBirth(),
        c.isActive());
  }
}
