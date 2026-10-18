package com.bankingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Customer profile entity linked 1:1 with AppUser.
 */
@Entity
@Table(name = "customer")
public class Customer extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private AppUser user;

  @Column(name = "full_name", nullable = false, length = 200)
  private String fullName;

  @Column(name = "nic", nullable = false, unique = true, length = 20)
  private String nic;

  @Column(name = "phone", nullable = false, length = 20)
  private String phone;

  @Column(name = "address", nullable = false, length = 500)
  private String address;

  @Column(name = "date_of_birth", nullable = false)
  private LocalDate dateOfBirth;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  public Customer() {}

  public AppUser getUser() {
    return user;
  }

  public void setUser(AppUser user) {
    this.user = user;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getNic() {
    return nic;
  }

  public void setNic(String nic) {
    this.nic = nic;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
