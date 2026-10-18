package com.bankingsystem.repository;

import com.bankingsystem.entity.AppSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {

  Optional<AppSetting> findByKeyIgnoreCase(String key);

  boolean existsByKeyIgnoreCase(String key);
}
