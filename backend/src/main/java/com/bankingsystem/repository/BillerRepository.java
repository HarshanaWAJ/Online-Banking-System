package com.bankingsystem.repository;

import com.bankingsystem.entity.Biller;
import com.bankingsystem.enums.BillerStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing utility billers.
 */
public interface BillerRepository extends JpaRepository<Biller, Long> {

  Optional<Biller> findByCodeIgnoreCase(String code);

  boolean existsByCodeIgnoreCase(String code);

  List<Biller> findByStatusOrderByNameAsc(BillerStatus status);

  Page<Biller> findAllByOrderByNameAsc(Pageable pageable);
}
