package com.bankingsystem.service;

import com.bankingsystem.dto.admin.SystemSettingResponse;
import com.bankingsystem.dto.admin.UpdateSettingsRequest;
import java.util.List;

/**
 * Service for managing global system configurations.
 */
public interface AdminSettingsService {

  List<SystemSettingResponse> getAllSettings();

  void updateSettings(UpdateSettingsRequest request, Long adminUserId);

  String getSettingValue(String key, String defaultValue);

  void seedDefaultSettings();
}
