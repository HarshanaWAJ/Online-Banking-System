package com.bankingsystem.dto.admin;

/**
 * DTO representing a system setting.
 */
public record SystemSettingResponse(
    Long id,
    String settingKey,
    String settingValue,
    String category,
    String label,
    String description,
    String dataType
) {}
