package com.gmnl.orientation.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版登录防爆破:按用户名统计连续失败次数,达到阈值后锁定一段时间。
 * 成功登录即清零;过期锁在访问时惰性清理。单体内网部署足够,无需外部存储。
 */
@Service
public class LoginAttemptService {

  private final int maxAttempts;
  private final Duration lockDuration;
  private final Clock clock;

  private static final class State {
    int failures;
    Instant lockedUntil;
  }

  private final Map<String, State> states = new ConcurrentHashMap<>();

  @org.springframework.beans.factory.annotation.Autowired
  public LoginAttemptService(
      @Value("${app.login.max-attempts:5}") int maxAttempts,
      @Value("${app.login.lock-duration-minutes:10}") long lockDurationMinutes) {
    this(maxAttempts, Duration.ofMinutes(lockDurationMinutes), Clock.systemUTC());
  }

  /** 测试可用短阈值/短时长与可控时钟。 */
  LoginAttemptService(int maxAttempts, Duration lockDuration, Clock clock) {
    this.maxAttempts = maxAttempts;
    this.lockDuration = lockDuration;
    this.clock = clock;
  }

  public boolean isLocked(String username) {
    State s = states.get(username);
    if (s == null) return false;
    synchronized (s) {
      if (s.lockedUntil == null) return false;
      if (!Instant.now(clock).isBefore(s.lockedUntil)) {
        // 锁已过期:惰性清理
        states.remove(username, s);
        return false;
      }
      return true;
    }
  }

  public void onFailure(String username) {
    State s = states.computeIfAbsent(username, k -> new State());
    synchronized (s) {
      // 已过期的旧锁先清理,重新计数
      if (s.lockedUntil != null && !Instant.now(clock).isBefore(s.lockedUntil)) {
        s.failures = 0;
        s.lockedUntil = null;
      }
      if (s.lockedUntil != null) return; // 已锁定,不再累加
      s.failures++;
      if (s.failures >= maxAttempts) {
        s.lockedUntil = Instant.now(clock).plus(lockDuration);
      }
    }
  }

  public void onSuccess(String username) {
    states.remove(username);
  }
}
