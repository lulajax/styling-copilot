package com.company.fashion.modules.match.service;

import com.company.fashion.common.exception.BusinessException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TaskRateLimitService {

  private final Map<String, Deque<Instant>> requests = new ConcurrentHashMap<>();

  @Value("${app.match.rate-limit.max-requests-per-second:3}")
  private int maxRequestsPerSecond;

  public void assertAllowed(String operatorUsername) {
    if (operatorUsername == null || operatorUsername.isBlank()) {
      throw new BusinessException(401, "Missing operator identity");
    }

    Instant now = Instant.now();
    Instant threshold = now.minusSeconds(1);
    Deque<Instant> deque = requests.computeIfAbsent(operatorUsername, ignored -> new ArrayDeque<>());

    synchronized (deque) {
      while (!deque.isEmpty() && deque.peekFirst().isBefore(threshold)) {
        deque.pollFirst();
      }
      if (deque.size() >= maxRequestsPerSecond) {
        throw new BusinessException(429, "Task creation rate limit exceeded");
      }
      deque.addLast(now);
    }
  }
}
