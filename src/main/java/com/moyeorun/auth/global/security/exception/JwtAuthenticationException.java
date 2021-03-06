package com.moyeorun.auth.global.security.exception;

import com.moyeorun.auth.global.error.ErrorCode;
import com.moyeorun.auth.global.error.exception.AuthenticationFailException;

public class JwtAuthenticationException extends AuthenticationFailException {

  public JwtAuthenticationException() {
    super(ErrorCode.AUTHENTICATION_FAIL);
  }
}
