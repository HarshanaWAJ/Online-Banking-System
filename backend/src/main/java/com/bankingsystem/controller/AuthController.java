package com.bankingsystem.controller;

import com.bankingsystem.constants.ApiPaths;
import com.bankingsystem.dto.common.ApiResponse;
import com.bankingsystem.dto.request.ChangePasswordRequest;
import com.bankingsystem.dto.request.LoginRequest;
import com.bankingsystem.dto.request.RegisterRequest;
import com.bankingsystem.dto.response.UserResponse;
import com.bankingsystem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller handling user authentication, session lifecycle, and password updates.
 */
@RestController
@RequestMapping(ApiPaths.AUTH)
@Tag(name = "Authentication", description = "Endpoints for user registration, login, logout, and credentials")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  @Operation(summary = "Register customer", description = "Public self-registration for new customers")
  public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
    UserResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @PostMapping("/login")
  @Operation(summary = "Authenticate user", description = "Validates credentials and sets httpOnly session cookie")
  public ResponseEntity<ApiResponse<UserResponse>> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    UserResponse response = authService.login(request, httpRequest, httpResponse);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping("/logout")
  @Operation(summary = "Logout user", description = "Invalidates the active session and clears session cookie")
  public ResponseEntity<ApiResponse<Void>> logout(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    authService.logout(httpRequest, httpResponse);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }

  @GetMapping("/me")
  @Operation(summary = "Get current authenticated user profile")
  public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
    UserResponse response = authService.getCurrentUser();
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @PostMapping("/change-password")
  @Operation(summary = "Change password", description = "Changes password and invalidates all other active sessions")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @Valid @RequestBody ChangePasswordRequest request) {
    authService.changePassword(request);
    return ResponseEntity.ok(ApiResponse.ok(null));
  }
}
