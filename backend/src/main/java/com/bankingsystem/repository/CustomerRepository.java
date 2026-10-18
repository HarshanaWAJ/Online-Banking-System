package com.bankingsystem.repository;

import com.bankingsystem.entity.Customer;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Customer.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

  Optional<Customer> findByUserId(Long userId);

  Optional<Customer> findByNicIgnoreCase(String nic);

  boolean existsByNicIgnoreCase(String nic);

  @Query(
      "SELECT c FROM Customer c WHERE "
          + "(:search IS NULL OR :search = '' OR "
          + "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(c.nic) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(c.user.email) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<Customer> searchCustomers(@Param("search") String search, Pageable pageable);
}
