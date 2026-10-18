package com.bankingsystem.service;

import com.bankingsystem.dto.request.ChangePasswordRequest;
import com.bankingsystem.dto.request.LoginRequest;
import com.bankingsystem.dto.request.RegisterRequest;
import com.bankingsystem.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authentication service interface.
 */
public interface AuthService {

  UserResponse register(RegisterRequest request);

  UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

  void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

  UserResponse getCurrentUser();

  void changePassword(ChangePasswordRequest request);
}
