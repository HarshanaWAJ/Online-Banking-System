package com.bankingsystem.repository;

import com.bankingsystem.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for AppUser.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

  Optional<AppUser> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);
}
