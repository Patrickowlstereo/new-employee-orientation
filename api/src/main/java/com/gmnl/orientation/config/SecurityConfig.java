package com.gmnl.orientation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Minimal security config for the seed phase.
 *
 * Only the PasswordEncoder bean is defined here so that DataInitializer
 * (Task 6) can hash the admin password at startup. The full SecurityFilterChain
 * and related security configuration will be added in Task 7, at which point
 * this class may be extended or replaced.
 */
@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}
