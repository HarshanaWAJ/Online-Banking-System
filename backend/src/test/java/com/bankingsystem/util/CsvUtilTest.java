package com.bankingsystem.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CsvUtilTest {

  @Test
  @DisplayName("Formula injection characters =, +, -, @ should be prepended with a single quote")
  void testFormulaNeutralization() {
    assertEquals("'=cmd|' /C calc'!A0", CsvUtil.neutralizeFormula("=cmd|' /C calc'!A0"));
    assertEquals("'+12345", CsvUtil.neutralizeFormula("+12345"));
    assertEquals("'-100.00", CsvUtil.neutralizeFormula("-100.00"));
    assertEquals("'@SUM(A1:A10)", CsvUtil.neutralizeFormula("@SUM(A1:A10)"));
  }

  @Test
  @DisplayName("Normal strings should not be modified")
  void testNormalStrings() {
    assertEquals("Regular String", CsvUtil.neutralizeFormula("Regular String"));
    assertEquals("", CsvUtil.neutralizeFormula(""));
    assertEquals("", CsvUtil.neutralizeFormula(null));
  }
}
