package com.bankingsystem.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bankingsystem.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyUtilTest {

  @Test
  @DisplayName("Valid positive amounts with two decimals should pass")
  void testValidAmount() {
    BigDecimal input = new BigDecimal("1250.50");
    BigDecimal result = MoneyUtil.requireValidAmount(input);
    assertEquals(new BigDecimal("1250.50"), result);
  }

  @Test
  @DisplayName("Zero or negative amounts should throw BusinessRuleException")
  void testNegativeOrZeroAmount() {
    assertThrows(BusinessRuleException.class, () -> MoneyUtil.requireValidAmount(BigDecimal.ZERO));
    assertThrows(BusinessRuleException.class, () -> MoneyUtil.requireValidAmount(new BigDecimal("-100.00")));
    assertThrows(BusinessRuleException.class, () -> MoneyUtil.requireValidAmount(null));
  }

  @Test
  @DisplayName("Amounts with more than 2 decimal places should throw BusinessRuleException")
  void testTooManyDecimals() {
    assertThrows(BusinessRuleException.class, () -> MoneyUtil.requireValidAmount(new BigDecimal("100.555")));
  }
}
