package com.bankingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Beneficiary entity saved by a customer for fast, repeat fund transfers.
 */
@Entity
@Table(
    name = "beneficiary",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_customer_beneficiary_account",
          columnNames = {"customer_id", "account_number"})
    },
    indexes = {
      @Index(name = "idx_beneficiary_customer_id", columnList = "customer_id")
    })
public class Beneficiary extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "account_number", nullable = false, length = 12)
  private String accountNumber;

  @Column(name = "nickname", length = 100)
  private String nickname;

  public Beneficiary() {}

  public Beneficiary(Customer customer, String name, String accountNumber, String nickname) {
    this.customer = customer;
    this.name = name;
    this.accountNumber = accountNumber;
    this.nickname = nickname;
  }

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public void setAccountNumber(String accountNumber) {
    this.accountNumber = accountNumber;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }
}
