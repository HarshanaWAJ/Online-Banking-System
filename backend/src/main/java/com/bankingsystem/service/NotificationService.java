package com.bankingsystem.service;

import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.Notification;
import com.bankingsystem.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Service managing user notifications.
 */
public interface NotificationService {

  void createNotification(AppUser user, NotificationType type, String message, String link);

  Page<Notification> getNotificationsForUser(Long userId, Pageable pageable);

  List<Notification> getUnreadNotificationsForUser(Long userId);

  long getUnreadCount(Long userId);

  void markAsRead(Long notificationId, Long userId);

  void markAllAsRead(Long userId);
}
