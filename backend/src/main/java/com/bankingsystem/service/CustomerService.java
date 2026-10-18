package com.bankingsystem.service;

import com.bankingsystem.dto.request.UpdateProfileRequest;
import com.bankingsystem.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Customer profile management service.
 */
public interface CustomerService {

  CustomerResponse getMyProfile();

  CustomerResponse updateMyProfile(UpdateProfileRequest request);

  Page<CustomerResponse> searchCustomers(String search, Pageable pageable);

  CustomerResponse getCustomerById(Long customerId);

  CustomerResponse toggleCustomerStatus(Long customerId, boolean active);
}
