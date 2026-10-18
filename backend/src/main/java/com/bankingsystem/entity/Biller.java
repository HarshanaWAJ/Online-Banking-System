package com.bankingsystem.entity;

import com.bankingsystem.enums.BillerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Utility biller entity configured by Admin for customer bill payments.
 */
@Entity
@Table(name = "biller")
public class Biller extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true, length = 30)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "category", nullable = false, length = 50)
  private String category;

  @Column(name = "reference_format", length = 100)
  private String referenceFormat;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private BillerStatus status = BillerStatus.ACTIVE;

  public Biller() {}

  public Biller(String code, String name, String category, String referenceFormat, BillerStatus status) {
    this.code = code;
    this.name = name;
    this.category = category;
    this.referenceFormat = referenceFormat;
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getReferenceFormat() {
    return referenceFormat;
  }

  public void setReferenceFormat(String referenceFormat) {
    this.referenceFormat = referenceFormat;
  }

  public BillerStatus getStatus() {
    return status;
  }

  public void setStatus(BillerStatus status) {
    this.status = status;
  }
}
