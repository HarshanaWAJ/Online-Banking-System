package com.bankingsystem.service;

import com.bankingsystem.dto.request.BillPaymentRequest;
import com.bankingsystem.dto.response.BillPaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BillPaymentService {

  BillPaymentResponse payBill(BillPaymentRequest request);

  Page<BillPaymentResponse> getMyBillPayments(Long accountId, Pageable pageable);

  BillPaymentResponse getBillPaymentById(Long id);
}
