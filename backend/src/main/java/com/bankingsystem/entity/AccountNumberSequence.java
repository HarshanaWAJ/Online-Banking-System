package com.bankingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Atomic sequence generator for 12-digit unique account numbers.
 */
@Entity
@Table(name = "account_number_sequence")
public class AccountNumberSequence {

  @Id
  private Long id = 1L;

  @Column(name = "next_value", nullable = false)
  private Long nextValue = 100000000000L;

  public AccountNumberSequence() {}

  public AccountNumberSequence(Long id, Long nextValue) {
    this.id = id;
    this.nextValue = nextValue;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getNextValue() {
    return nextValue;
  }

  public void setNextValue(Long nextValue) {
    this.nextValue = nextValue;
  }
}
