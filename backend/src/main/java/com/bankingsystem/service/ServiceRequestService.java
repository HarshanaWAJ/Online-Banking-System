package com.bankingsystem.service;

import com.bankingsystem.dto.request.ServiceRequestCreateDto;
import com.bankingsystem.dto.request.ServiceRequestResolveDto;
import com.bankingsystem.dto.response.ServiceRequestResponse;
import com.bankingsystem.enums.ServiceRequestStatus;
import com.bankingsystem.enums.ServiceRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServiceRequestService {

  ServiceRequestResponse createRequest(ServiceRequestCreateDto request);

  Page<ServiceRequestResponse> getMyRequests(Pageable pageable);

  ServiceRequestResponse getRequestById(Long id);

  Page<ServiceRequestResponse> searchRequests(
      ServiceRequestStatus status,
      ServiceRequestType type,
      Pageable pageable);

  ServiceRequestResponse assignToMe(Long id);

  ServiceRequestResponse resolveRequest(Long id, ServiceRequestResolveDto request);
}
