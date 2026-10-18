package com.bankingsystem.service;

import com.bankingsystem.dto.request.CashOperationRequest;
import com.bankingsystem.dto.response.CashOperationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service managing cash desk deposits and withdrawals.
 */
public interface CashOperationService {

  CashOperationResponse postCashOperation(CashOperationRequest request);

  Page<CashOperationResponse> getCashOperations(Pageable pageable);

  Page<CashOperationResponse> getCashOperationsByAccount(Long accountId, Pageable pageable);
}
