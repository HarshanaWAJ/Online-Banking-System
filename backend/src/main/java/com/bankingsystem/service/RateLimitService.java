package com.bankingsystem.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * In-memory per-IP rate limiting service based on Bucket4j token bucket algorithm.
 */
@Service
public class RateLimitService {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final int maxAttempts;
  private final int windowMinutes;

  public RateLimitService(
      @Value("${app.rate-limit.login-max-attempts:5}") int maxAttempts,
      @Value("${app.rate-limit.login-window-minutes:15}") int windowMinutes) {
    this.maxAttempts = maxAttempts;
    this.windowMinutes = windowMinutes;
  }

  public boolean tryConsume(String key) {
    Bucket bucket = buckets.computeIfAbsent(key, this::createBucket);
    return bucket.tryConsume(1);
  }

  private Bucket createBucket(String key) {
    Bandwidth limit = Bandwidth.classic(
        maxAttempts,
        Refill.intervally(maxAttempts, Duration.ofMinutes(windowMinutes)));
    return Bucket.builder().addLimit(limit).build();
  }
}
