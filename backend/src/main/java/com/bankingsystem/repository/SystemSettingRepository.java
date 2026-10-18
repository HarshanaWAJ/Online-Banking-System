package com.bankingsystem.repository;

import com.bankingsystem.entity.SystemSetting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for SystemSetting entity.
 */
@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

  Optional<SystemSetting> findBySettingKey(String settingKey);

  List<SystemSetting> findAllByOrderByCategoryAscSettingKeyAsc();
}
