package com.bankingsystem.repository;

import com.bankingsystem.entity.Beneficiary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for customer Beneficiaries.
 */
@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

  List<Beneficiary> findByCustomerIdOrderByNameAsc(Long customerId);

  Optional<Beneficiary> findByIdAndCustomerId(Long id, Long customerId);

  boolean existsByCustomerIdAndAccountNumber(Long customerId, String accountNumber);
}
