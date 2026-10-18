package com.bankingsystem.repository;

import com.bankingsystem.entity.AccountNumberSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for row-locked AccountNumberSequence generation.
 */
@Repository
public interface AccountNumberSequenceRepository extends JpaRepository<AccountNumberSequence, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM AccountNumberSequence s WHERE s.id = :id")
  Optional<AccountNumberSequence> findByIdForUpdate(@Param("id") Long id);
}
