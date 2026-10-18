package com.bankingsystem.service.impl;

import com.bankingsystem.dto.request.BeneficiaryRequest;
import com.bankingsystem.dto.response.BeneficiaryResponse;
import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.Beneficiary;
import com.bankingsystem.entity.Customer;
import com.bankingsystem.exception.BusinessRuleException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.BankAccountRepository;
import com.bankingsystem.repository.BeneficiaryRepository;
import com.bankingsystem.repository.CustomerRepository;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.BeneficiaryService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Beneficiary service implementation.
 */
@Service
public class BeneficiaryServiceImpl implements BeneficiaryService {

  private final BeneficiaryRepository beneficiaryRepository;
  private final CustomerRepository customerRepository;
  private final BankAccountRepository accountRepository;
  private final OwnershipGuard ownershipGuard;

  public BeneficiaryServiceImpl(
      BeneficiaryRepository beneficiaryRepository,
      CustomerRepository customerRepository,
      BankAccountRepository accountRepository,
      OwnershipGuard ownershipGuard) {
    this.beneficiaryRepository = beneficiaryRepository;
    this.customerRepository = customerRepository;
    this.accountRepository = accountRepository;
    this.ownershipGuard = ownershipGuard;
  }

  @Override
  @Transactional(readOnly = true)
  public List<BeneficiaryResponse> getMyBeneficiaries() {
    Customer customer = getCurrentCustomer();
    return beneficiaryRepository.findByCustomerIdOrderByNameAsc(customer.getId())
        .stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public BeneficiaryResponse addBeneficiary(BeneficiaryRequest request) {
    Customer customer = getCurrentCustomer();
    String targetAccNum = request.accountNumber().trim();

    if (beneficiaryRepository.existsByCustomerIdAndAccountNumber(customer.getId(), targetAccNum)) {
      throw new BusinessRuleException("A beneficiary with this account number already exists in your list.");
    }

    // Verify account exists in bank
    if (!accountRepository.existsByAccountNumber(targetAccNum)) {
      throw new BusinessRuleException("Account number " + targetAccNum + " does not exist in this bank.");
    }

    Beneficiary beneficiary = new Beneficiary(
        customer,
        request.name().trim(),
        targetAccNum,
        request.nickname() != null ? request.nickname().trim() : null);

    beneficiary = beneficiaryRepository.save(beneficiary);
    return mapToResponse(beneficiary);
  }

  @Override
  @Transactional
  public BeneficiaryResponse updateBeneficiary(Long id, BeneficiaryRequest request) {
    Customer customer = getCurrentCustomer();
    Beneficiary beneficiary = beneficiaryRepository.findByIdAndCustomerId(id, customer.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", id));

    String targetAccNum = request.accountNumber().trim();
    if (!beneficiary.getAccountNumber().equals(targetAccNum)) {
      if (!accountRepository.existsByAccountNumber(targetAccNum)) {
        throw new BusinessRuleException("Target account number does not exist.");
      }
    }

    beneficiary.setName(request.name().trim());
    beneficiary.setAccountNumber(targetAccNum);
    beneficiary.setNickname(request.nickname() != null ? request.nickname().trim() : null);

    beneficiary = beneficiaryRepository.save(beneficiary);
    return mapToResponse(beneficiary);
  }

  @Override
  @Transactional
  public void deleteBeneficiary(Long id) {
    Customer customer = getCurrentCustomer();
    Beneficiary beneficiary = beneficiaryRepository.findByIdAndCustomerId(id, customer.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", id));

    beneficiaryRepository.delete(beneficiary);
  }

  private Customer getCurrentCustomer() {
    AppUser user = ownershipGuard.getCurrentUser();
    return customerRepository.findByUserId(user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
  }

  private BeneficiaryResponse mapToResponse(Beneficiary b) {
    return new BeneficiaryResponse(b.getId(), b.getName(), b.getAccountNumber(), b.getNickname());
  }
}
