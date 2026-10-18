package com.bankingsystem.repository;

import com.bankingsystem.entity.UserSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for UserSession management.
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

  @Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.jti = :jti")
  Optional<UserSession> findByJti(@Param("jti") String jti);

  void deleteByJti(String jti);

  @Modifying
  @Query("DELETE FROM UserSession s WHERE s.user.id = :userId")
  void deleteAllByUserId(@Param("userId") Long userId);

  @Modifying
  @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
  void deleteExpiredSessions(@Param("now") Instant now);
}
