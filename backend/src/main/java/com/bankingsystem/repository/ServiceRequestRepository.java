package com.bankingsystem.repository;

import com.bankingsystem.entity.ServiceRequest;
import com.bankingsystem.enums.ServiceRequestStatus;
import com.bankingsystem.enums.ServiceRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

  Page<ServiceRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

  @Query("SELECT sr FROM ServiceRequest sr WHERE " +
         "(:status IS NULL OR sr.status = :status) AND " +
         "(:type IS NULL OR sr.type = :type) " +
         "ORDER BY sr.createdAt DESC")
  Page<ServiceRequest> searchRequests(
      @Param("status") ServiceRequestStatus status,
      @Param("type") ServiceRequestType type,
      Pageable pageable);
}
