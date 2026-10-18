package com.bankingsystem.service;

import com.bankingsystem.dto.request.AccountOpeningRequest;
import com.bankingsystem.dto.request.CloseAccountRequest;
import com.bankingsystem.dto.request.RejectRequest;
import com.bankingsystem.dto.response.BankAccountResponse;
import com.bankingsystem.enums.AccountStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Bank account and lifecycle service.
 */
public interface AccountService {

  BankAccountResponse requestAccount(AccountOpeningRequest request);

  List<BankAccountResponse> getMyAccounts();

  BankAccountResponse getMyAccountById(Long accountId);

  void cancelPendingRequest(Long accountId);

  Page<BankAccountResponse> searchAccountRequests(AccountStatus status, Pageable pageable);

  BankAccountResponse approveAccountRequest(Long accountId);

  BankAccountResponse rejectAccountRequest(Long accountId, RejectRequest request);

  BankAccountResponse freezeAccount(Long accountId);

  BankAccountResponse unfreezeAccount(Long accountId);

  BankAccountResponse closeAccount(Long accountId, CloseAccountRequest request);

  Page<BankAccountResponse> searchAllAccounts(AccountStatus status, String search, Pageable pageable);

  BankAccountResponse getAccountByIdForStaff(Long accountId);
}
