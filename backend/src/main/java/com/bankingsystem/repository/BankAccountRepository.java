package com.bankingsystem.repository;

import com.bankingsystem.entity.BankAccount;
import com.bankingsystem.enums.AccountStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for BankAccount.
 */
@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

  List<BankAccount> findByCustomerId(Long customerId);

  List<BankAccount> findByCustomerIdAndStatus(Long customerId, AccountStatus status);

  Optional<BankAccount> findByAccountNumber(String accountNumber);

  boolean existsByAccountNumber(String accountNumber);

  Page<BankAccount> findByStatus(AccountStatus status, Pageable pageable);

  long countByStatus(AccountStatus status);

  @Query(
      "SELECT b FROM BankAccount b WHERE "
          + "(:status IS NULL OR b.status = :status) AND "
          + "(:search IS NULL OR :search = '' OR "
          + "b.accountNumber LIKE CONCAT('%', :search, '%') OR "
          + "LOWER(b.customer.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(b.customer.nic) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(b.customer.user.email) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<BankAccount> searchAccounts(
      @Param("status") AccountStatus status,
      @Param("search") String search,
      Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT b FROM BankAccount b WHERE b.id = :id")
  Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);
}
