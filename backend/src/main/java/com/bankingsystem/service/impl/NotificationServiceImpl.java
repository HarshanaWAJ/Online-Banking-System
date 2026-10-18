package com.bankingsystem.service.impl;

import com.bankingsystem.entity.AppUser;
import com.bankingsystem.entity.Notification;
import com.bankingsystem.enums.NotificationType;
import com.bankingsystem.exception.AccessDeniedException;
import com.bankingsystem.exception.ResourceNotFoundException;
import com.bankingsystem.repository.NotificationRepository;
import com.bankingsystem.service.NotificationService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notification service implementation.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;

  public NotificationServiceImpl(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  @Override
  @Transactional
  public void createNotification(AppUser user, NotificationType type, String message, String link) {
    var notification = new Notification(user, type, message, link);
    notificationRepository.save(notification);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Notification> getNotificationsForUser(Long userId, Pageable pageable) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Notification> getUnreadNotificationsForUser(Long userId) {
    return notificationRepository.findTop10ByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
  }

  @Override
  @Transactional(readOnly = true)
  public long getUnreadCount(Long userId) {
    return notificationRepository.countByUserIdAndReadFalse(userId);
  }

  @Override
  @Transactional
  public void markAsRead(Long notificationId, Long userId) {
    var notification = notificationRepository
        .findById(notificationId)
        .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

    if (!notification.getUser().getId().equals(userId)) {
      throw new AccessDeniedException("You do not own this notification");
    }

    notification.setRead(true);
    notificationRepository.save(notification);
  }

  @Override
  @Transactional
  public void markAllAsRead(Long userId) {
    notificationRepository.markAllAsRead(userId);
  }
}
