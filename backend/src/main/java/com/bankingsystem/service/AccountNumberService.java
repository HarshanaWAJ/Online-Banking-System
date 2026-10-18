package com.bankingsystem.service;

/**
 * Service generating unique, sequential 12-digit bank account numbers.
 */
public interface AccountNumberService {

  String generateAccountNumber();
}
