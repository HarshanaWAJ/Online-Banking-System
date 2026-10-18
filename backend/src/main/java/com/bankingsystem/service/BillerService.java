package com.bankingsystem.service;

import com.bankingsystem.dto.request.BillerRequest;
import com.bankingsystem.dto.response.BillerResponse;
import com.bankingsystem.enums.BillerStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BillerService {

  BillerResponse createBiller(BillerRequest request);

  BillerResponse updateBiller(Long id, BillerRequest request);

  BillerResponse updateBillerStatus(Long id, BillerStatus status);

  List<BillerResponse> getActiveBillers();

  Page<BillerResponse> getAllBillers(Pageable pageable);

  BillerResponse getBillerById(Long id);
}
