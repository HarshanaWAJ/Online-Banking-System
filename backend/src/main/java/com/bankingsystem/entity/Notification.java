package com.bankingsystem.entity;

import com.bankingsystem.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * In-app notification entity.
 */
@Entity
@Table(
    name = "notification",
    indexes = {
      @Index(name = "idx_notification_user_read", columnList = "user_id, is_read, created_at")
    })
public class Notification extends BaseEntity {

  @com.fasterxml.jackson.annotation.JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30)
  private NotificationType type;

  @Column(name = "message", nullable = false, length = 500)
  private String message;

  @Column(name = "link", length = 200)
  private String link;

  @Column(name = "is_read", nullable = false)
  private boolean read = false;

  public Notification() {}

  public Notification(AppUser user, NotificationType type, String message, String link) {
    this.user = user;
    this.type = type;
    this.message = message;
    this.link = link;
    this.read = false;
  }

  public AppUser getUser() {
    return user;
  }

  public void setUser(AppUser user) {
    this.user = user;
  }

  public NotificationType getType() {
    return type;
  }

  public void setType(NotificationType type) {
    this.type = type;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public boolean isRead() {
    return read;
  }

  public void setRead(boolean read) {
    this.read = read;
  }
}
