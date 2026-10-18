package com.bankingsystem.util;

import com.bankingsystem.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility for strict currency validation and rounding rules.
 * Enforces positive amounts, 2 decimal places, and HALF_UP rounding.
 */
public final class MoneyUtil {

  private MoneyUtil() {}

  public static BigDecimal requireValidAmount(BigDecimal amount) {
    if (amount == null) {
      throw new BusinessRuleException("Amount must not be null.");
    }
    if (amount.scale() > 2) {
      throw new BusinessRuleException("Amount cannot have more than 2 decimal places.");
    }
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessRuleException("Amount must be greater than zero.");
    }
    return amount.setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal zero() {
    return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
  }
}
