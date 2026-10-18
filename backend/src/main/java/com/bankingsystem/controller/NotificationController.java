package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.common.PageMeta;
import com.bankingsystem.entity.Notification;
import com.bankingsystem.security.OwnershipGuard;
import com.bankingsystem.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for in-app user notifications.
 */
@RestController
@RequestMapping(ApiPaths.NOTIFICATIONS)
@Tag(name = "Notifications", description = "Endpoints for retrieving and acknowledging in-app notifications")
public class NotificationController {

  private final NotificationService notificationService;
  private final OwnershipGuard ownershipGuard;

  public NotificationController(NotificationService notificationService, OwnershipGuard ownershipGuard) {
    this.notificationService = notificationService;
    this.ownershipGuard = ownershipGuard;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get current user notifications (paginated)")
  public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(
      @PageableDefault(size = 15) Pageable pageable) {
    Long userId = ownershipGuard.getCurrentUserId();
    Page<Notification> page = notificationService.getNotificationsForUser(userId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(page.getContent(), PageMeta.of(page)));
  }

  @GetMapping("/unread")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get latest unread notifications")
  public ResponseEntity<ApiResponse<List<Notification>>> getUnreadNotifications() {
    Long userId = ownershipGuard.getCurrentUserId();
    List<Notification> list = notificationService.getUnreadNotificationsForUser(userId);
    return ResponseEntity.ok(ApiResponse.ok(list));
  }

  @GetMapping("/unread-count")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get unread notifications count")
  public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
    Long userId = ownershipGuard.getCurrentUserId();
    long count = notificationService.getUnreadCount(userId);
    return ResponseEntity.ok(ApiResponse.ok(count));
  }

  @PatchMapping("/{id}/read")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Mark single notification as read")
  public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable("id") Long id) {
    Long userId = ownershipGuard.getCurrentUserId();
    notificationService.markAsRead(id, userId);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }

  @PostMapping("/read-all")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Mark all notifications as read")
  public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
    Long userId = ownershipGuard.getCurrentUserId();
    notificationService.markAllAsRead(userId);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }
}
