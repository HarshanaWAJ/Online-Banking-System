package com.bankingsystem.service;

import com.bankingsystem.dto.request.PostTransferRequest;
import com.bankingsystem.dto.request.TransferPreviewRequest;
import com.bankingsystem.dto.response.TransferPreviewResponse;
import com.bankingsystem.dto.response.TransferResponse;

/**
 * Service managing internal bank fund transfers.
 */
public interface TransferService {

  TransferPreviewResponse previewTransfer(TransferPreviewRequest request);

  TransferResponse postTransfer(PostTransferRequest request);

  TransferResponse getTransferById(Long id);
}
