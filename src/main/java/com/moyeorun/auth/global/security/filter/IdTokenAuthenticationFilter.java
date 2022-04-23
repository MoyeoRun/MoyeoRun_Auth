package com.moyeorun.auth.global.security.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeorun.auth.domain.auth.dto.request.SignUpRequest;
import com.moyeorun.auth.global.error.exception.InvalidValueException;
import com.moyeorun.auth.global.security.authentication.AppleAuthenticationIdToken;
import com.moyeorun.auth.global.security.authentication.GoogleAuthenticationIdToken;
import com.moyeorun.auth.domain.auth.dto.request.SignInRequest;
import com.moyeorun.auth.global.security.dto.IdTokenDto;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Slf4j
public class IdTokenAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  protected IdTokenAuthenticationFilter(String defaultFilterProcessesUrl) {
    super(defaultFilterProcessesUrl);
  }

  public IdTokenAuthenticationFilter(RequestMatcher requestMatcher) {
    super(requestMatcher);
  }
  
  @Override
  public Authentication attemptAuthentication(HttpServletRequest request,
      HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
    log.info("signIn Filter");

    String body = request.getReader().lines()
        .collect(Collectors.joining(System.lineSeparator()));

    String requestPath = request.getRequestURI();
    IdTokenDto idTokenDto;

    if (requestPath.equals("/api/auth/sign-in")) {
      idTokenDto = extractIdTokenSignIn(body);
    } else {
      idTokenDto = extractIdTokenSignUp(body);
    }

    if (idTokenDto.getProviderType() == null) {
      log.error("null Error");
      throw new InvalidValueException();
    }

    switch (idTokenDto.getProviderType()) {
      case GOOGLE -> {
        return super.getAuthenticationManager()
            .authenticate(new GoogleAuthenticationIdToken(idTokenDto.getIdToken()));
      }
      case APPLE -> {
        return super.getAuthenticationManager()
            .authenticate(new AppleAuthenticationIdToken(idTokenDto.getIdToken()));
      }
      default -> throw new InvalidValueException();
    }
  }


  private IdTokenDto extractIdTokenSignIn(String body) {
    try {
      SignInRequest signInRequestDto = objectMapper.readValue(body, SignInRequest.class);
      return new IdTokenDto(signInRequestDto.getIdToken(), signInRequestDto.getProviderType());
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
      throw new InvalidValueException();
    }
  }

  private IdTokenDto extractIdTokenSignUp(String body) {
    try {
      SignUpRequest signUpRequestDto = objectMapper.readValue(body, SignUpRequest.class);
      return new IdTokenDto(signUpRequestDto.getIdToken(), signUpRequestDto.getProviderType());
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
      throw new InvalidValueException();
    }
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain, Authentication authResult) throws IOException, ServletException {
    GoogleAuthenticationIdToken authenticationIdToken = (GoogleAuthenticationIdToken) authResult;
    log.info("success auth");
    SecurityContextHolder.getContext().setAuthentication(authenticationIdToken);
    chain.doFilter(request, response);
  }

  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request,
      HttpServletResponse response, AuthenticationException failed)
      throws IOException, ServletException {
    log.info("인증 실패");
    super.unsuccessfulAuthentication(request, response, failed);
  }
}