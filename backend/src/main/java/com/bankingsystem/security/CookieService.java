package com.bankingsystem.security;

import com.bankingsystem.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * Service managing httpOnly session cookies.
 */
@Service
public class CookieService {

  private final AppProperties appProperties;

  public CookieService(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  public String getCookieName() {
    return appProperties.getCookie().name();
  }

  public void addSessionCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
    ResponseCookie cookie =
        ResponseCookie.from(getCookieName(), token)
            .httpOnly(true)
            .secure(appProperties.getCookie().secure())
            .path("/")
            .maxAge(maxAgeSeconds)
            .sameSite("Lax")
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public void clearSessionCookie(HttpServletResponse response) {
    ResponseCookie cookie =
        ResponseCookie.from(getCookieName(), "")
            .httpOnly(true)
            .secure(appProperties.getCookie().secure())
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public Optional<String> extractToken(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return Optional.empty();
    }
    return Arrays.stream(request.getCookies())
        .filter(c -> getCookieName().equals(c.getName()))
        .map(Cookie::getValue)
        .filter(v -> v != null && !v.isBlank())
        .findFirst();
  }
}
