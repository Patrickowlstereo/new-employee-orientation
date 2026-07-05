package com.gmnl.orientation.user;

import com.gmnl.orientation.common.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserResolver {

  public Long userId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    return Long.valueOf(auth.getPrincipal().toString());
  }
}
