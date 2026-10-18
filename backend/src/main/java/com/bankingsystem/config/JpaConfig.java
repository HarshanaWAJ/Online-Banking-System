package com.bankingsystem.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * JPA auditing configuration.
 *
 * <p>Provides the {@link AuditorAware} bean that JPA uses to populate the {@code @CreatedBy} and
 * {@code @LastModifiedBy} fields on audited entities. The current principal's username is returned
 * when a request is authenticated; otherwise {@code "system"} is used (e.g., during scheduled
 * tasks or startup seed operations).
 */
@Configuration
public class JpaConfig {

  /**
   * Creates the {@link AuditorAware} bean named {@code auditorProvider}.
   *
   * <p>This bean is referenced by the {@code auditorAwareRef} attribute on
   * {@code @EnableJpaAuditing} in {@link com.bankingsystem.BankingSystemApplication}.
   *
   * @return an {@link AuditorAware} that resolves the current auditor's username
   */
  @Bean
  public AuditorAware<String> auditorProvider() {
    return () -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null
          || !authentication.isAuthenticated()
          || "anonymousUser".equals(authentication.getPrincipal())) {
        return Optional.of("system");
      }
      return Optional.of(authentication.getName());
    };
  }
}
