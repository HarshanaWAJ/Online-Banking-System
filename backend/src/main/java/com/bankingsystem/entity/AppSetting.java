package com.bankingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Key-value system configuration entity managed by Admin.
 */
@Entity
@Table(name = "app_setting")
public class AppSetting extends BaseEntity {

  @Column(name = "setting_key", nullable = false, unique = true, length = 100)
  private String key;

  @Column(name = "setting_value", nullable = false, length = 1000)
  private String value;

  @Column(name = "description", length = 300)
  private String description;

  public AppSetting() {}

  public AppSetting(String key, String value, String description) {
    this.key = key;
    this.value = value;
    this.description = description;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
