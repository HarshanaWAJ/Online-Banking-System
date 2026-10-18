package com.bankingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.bankingsystem.config.AppProperties;
import com.bankingsystem.config.JwtProperties;

/**
 * Entry point for the Web Based Banking System backend application.
 *
 * <p>Bootstraps Spring Boot, enables JPA auditing (so {@code @CreatedDate},
 * {@code @LastModifiedDate}, {@code @CreatedBy}, and {@code @LastModifiedBy} fields are
 * automatically populated), and enables scheduled task execution.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableScheduling
@EnableConfigurationProperties({AppProperties.class, JwtProperties.class})
public class BankingSystemApplication {

  /**
   * Application main method.
   *
   * @param args command-line arguments passed to Spring Boot
   */
  public static void main(String[] args) {
    SpringApplication.run(BankingSystemApplication.class, args);
  }
}
