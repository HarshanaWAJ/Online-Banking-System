package com.bankingsystem.service.impl;

import com.bankingsystem.dto.admin.SystemSettingResponse;
import com.bankingsystem.dto.admin.UpdateSettingsRequest;
import com.bankingsystem.entity.SystemSetting;
import com.bankingsystem.enums.AuditOutcome;
import com.bankingsystem.enums.Role;
import com.bankingsystem.repository.SystemSettingRepository;
import com.bankingsystem.service.AdminSettingsService;
import com.bankingsystem.service.AuditService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AdminSettingsService.
 */
@Service
public class AdminSettingsServiceImpl implements AdminSettingsService {

  private static final Logger log = LoggerFactory.getLogger(AdminSettingsServiceImpl.class);

  private final SystemSettingRepository settingRepository;
  private final AuditService auditService;

  public AdminSettingsServiceImpl(
      SystemSettingRepository settingRepository,
      AuditService auditService) {
    this.settingRepository = settingRepository;
    this.auditService = auditService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<SystemSettingResponse> getAllSettings() {
    return settingRepository.findAllByOrderByCategoryAscSettingKeyAsc().stream()
        .map(s -> new SystemSettingResponse(
            s.getId(),
            s.getSettingKey(),
            s.getSettingValue(),
            s.getCategory(),
            s.getLabel(),
            s.getDescription(),
            s.getDataType()))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void updateSettings(UpdateSettingsRequest request, Long adminUserId) {
    for (Map.Entry<String, String> entry : request.settings().entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue() != null ? entry.getValue().trim() : "";

      settingRepository.findBySettingKey(key).ifPresent(setting -> {
        setting.setSettingValue(value);
        settingRepository.save(setting);
      });
    }

    auditService.recordEvent(
        adminUserId,
        Role.ADMIN,
        "UPDATE_SYSTEM_SETTINGS",
        "SystemSetting",
        null,
        AuditOutcome.SUCCESS,
        "Updated " + request.settings().size() + " system setting(s)",
        null);
  }

  @Override
  @Transactional(readOnly = true)
  public String getSettingValue(String key, String defaultValue) {
    return settingRepository.findBySettingKey(key)
        .map(SystemSetting::getSettingValue)
        .orElse(defaultValue);
  }

  @Override
  @Transactional
  public void seedDefaultSettings() {
    createIfMissing("DAILY_TRANSFER_LIMIT", "500000.00", "FINANCIAL", "Daily Transfer Limit (LKR)", "Maximum aggregated daily outbound fund transfer limit per account in LKR.", "NUMBER");
    createIfMissing("SINGLE_TRANSACTION_LIMIT", "200000.00", "FINANCIAL", "Single Transaction Limit (LKR)", "Maximum single transaction limit for internal and third-party transfers in LKR.", "NUMBER");
    createIfMissing("SAVINGS_INTEREST_RATE", "7.50", "FINANCIAL", "Savings Annual Interest Rate (%)", "Annual interest percentage credited monthly on active savings accounts.", "NUMBER");
    createIfMissing("MIN_OPENING_DEPOSIT", "1000.00", "FINANCIAL", "Minimum Opening Deposit (LKR)", "Minimum initial deposit required upon savings account approval.", "NUMBER");
    createIfMissing("SESSION_TIMEOUT_MINUTES", "30", "SECURITY", "Session Inactivity Timeout (Minutes)", "JWT authentication session lifetime before automatic re-authentication.", "NUMBER");
    createIfMissing("MAX_LOGIN_ATTEMPTS", "5", "SECURITY", "Max Failed Login Attempts", "Maximum consecutive invalid login attempts allowed before IP throttle.", "NUMBER");
    createIfMissing("MAINTENANCE_MODE", "false", "SYSTEM", "Maintenance Mode", "When enabled, restricts customer access during scheduled operational maintenance.", "BOOLEAN");
    createIfMissing("MAINTENANCE_MESSAGE", "Scheduled system maintenance in progress. All core online banking services will resume shortly.", "SYSTEM", "Maintenance Banner Notice", "Notice displayed to users during active or scheduled maintenance windows.", "STRING");
    createIfMissing("SYSTEM_CURRENCY", "LKR", "SYSTEM", "System Operating Currency", "Primary currency standard for all accounts, ledgers, and transactions.", "STRING");
  }

  private void createIfMissing(String key, String value, String category, String label, String description, String dataType) {
    if (settingRepository.findBySettingKey(key).isEmpty()) {
      var s = new SystemSetting(key, value, category, label, description, dataType);
      settingRepository.save(s);
      log.info("Initialized default system setting: {}", key);
    }
  }
}
