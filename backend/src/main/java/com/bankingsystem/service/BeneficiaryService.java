package com.bankingsystem.service;

import com.bankingsystem.dto.request.BeneficiaryRequest;
import com.bankingsystem.dto.response.BeneficiaryResponse;
import java.util.List;

/**
 * Service managing customer beneficiaries.
 */
public interface BeneficiaryService {

  List<BeneficiaryResponse> getMyBeneficiaries();

  BeneficiaryResponse addBeneficiary(BeneficiaryRequest request);

  BeneficiaryResponse updateBeneficiary(Long id, BeneficiaryRequest request);

  void deleteBeneficiary(Long id);
}
