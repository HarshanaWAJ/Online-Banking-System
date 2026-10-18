package com.bankingsystem.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/**
 * Request payload to update system settings.
 */
public record UpdateSettingsRequest(
    @NotEmpty(message = "Settings map must not be empty")
    Map<String, String> settings
) {}
