package com.bankingsystem.security;

import com.bankingsystem.entity.AppUser;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security UserDetails adapter wrapping AppUser.
 */
public class AppUserDetails implements UserDetails {

  private final Long id;
  private final String email;
  private final String password;
  private final boolean active;
  private final Collection<? extends GrantedAuthority> authorities;

  public AppUserDetails(AppUser user) {
    this.id = user.getId();
    this.email = user.getEmail();
    this.password = user.getPasswordHash();
    this.active = user.isActive();

    // Map ROLE_ + role name
    var authList = new java.util.ArrayList<GrantedAuthority>();
    authList.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

    // Map staff permissions as authorities
    if (user.getPermissions() != null) {
      authList.addAll(
          user.getPermissions().stream()
              .map(p -> new SimpleGrantedAuthority(p.name()))
              .collect(Collectors.toList()));
    }
    this.authorities = authList;
  }

  public Long getId() {
    return id;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return active;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
