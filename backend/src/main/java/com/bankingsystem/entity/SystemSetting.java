package com.bankingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * System configuration and operational settings entity.
 */
@Entity
@Table(
    name = "system_setting",
    uniqueConstraints = {@UniqueConstraint(name = "uk_setting_key", columnNames = {"setting_key"})})
public class SystemSetting extends BaseEntity {

  @Column(name = "setting_key", nullable = false, unique = true, length = 100)
  private String settingKey;

  @Column(name = "setting_value", nullable = false, length = 1000)
  private String settingValue;

  @Column(name = "category", nullable = false, length = 50)
  private String category;

  @Column(name = "label", nullable = false, length = 200)
  private String label;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "data_type", nullable = false, length = 20)
  private String dataType;

  public SystemSetting() {}

  public SystemSetting(
      String settingKey,
      String settingValue,
      String category,
      String label,
      String description,
      String dataType) {
    this.settingKey = settingKey;
    this.settingValue = settingValue;
    this.category = category;
    this.label = label;
    this.description = description;
    this.dataType = dataType;
  }

  public String getSettingKey() {
    return settingKey;
  }

  public void setSettingKey(String settingKey) {
    this.settingKey = settingKey;
  }

  public String getSettingValue() {
    return settingValue;
  }

  public void setSettingValue(String settingValue) {
    this.settingValue = settingValue;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }
}
