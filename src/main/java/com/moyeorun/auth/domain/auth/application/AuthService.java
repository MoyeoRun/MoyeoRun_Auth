package com.moyeorun.auth.domain.auth.application;

import com.moyeorun.auth.domain.auth.dao.UserRepository;
import com.moyeorun.auth.domain.auth.domain.SnsIdentify;
import com.moyeorun.auth.domain.auth.domain.User;
import com.moyeorun.auth.domain.auth.dto.request.SignUpRequest;
import com.moyeorun.auth.domain.auth.dto.response.SignInResponse;
import com.moyeorun.auth.domain.auth.dto.response.SignUpResponse;
import com.moyeorun.auth.domain.auth.dto.response.TokenDto;
import com.moyeorun.auth.domain.auth.exception.DuplicateNicknameException;
import com.moyeorun.auth.domain.auth.exception.DuplicateSnsUserException;
import com.moyeorun.auth.domain.auth.exception.UserNotFoundException;
import com.moyeorun.auth.global.config.property.JwtProperty;
import com.moyeorun.auth.global.error.exception.InvalidValueException;
import com.moyeorun.auth.global.security.jwt.JwtProvider;
import com.moyeorun.auth.global.util.RedisUtil;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;
  private final StringRedisTemplate stringRedisTemplate;
  private final JwtProperty jwtProperty;
  private final RedisUtil redisUtil;

  @Transactional
  public SignUpResponse signUp(SignUpRequest signUpRequest, SnsIdentify snsIdentify, String email) {
    if (nicknameDuplicateCheck(signUpRequest.getNickName())) {
      throw new DuplicateNicknameException();
    }

    if (snsUserDuplicateCheck(snsIdentify)) {
      throw new DuplicateSnsUserException();
    }

    userRepository.save(signUpRequest.toEntity(snsIdentify, email));
    return new SignUpResponse("회원가입 성공");
  }

  @Transactional
  public SignInResponse signIn(SnsIdentify snsIdentify) {
    Optional<User> findUser = userRepository.findBySnsIdentify(snsIdentify);

    if (findUser.isPresent()) {
      String accessToken = jwtProvider.createAccessToken(findUser.get());
      String refreshToken = jwtProvider.createRefreshToken(findUser.get());

      redisUtil.setStringWidthExpire(refreshToken, findUser.get().getId().toString(),
          jwtProperty.getRefresh_token_expired_time());

      return new SignInResponse(findUser.get(), new TokenDto(accessToken, refreshToken));
    } else {
      return new SignInResponse();
    }
  }

  @Transactional
  public TokenDto refresh(String token) {
    String savedId = redisUtil.getValueByStringKey(token);

    if (savedId == null) {
      throw new InvalidValueException();
    }

    Optional<User> findUser = userRepository.findById(Long.valueOf(savedId));

    if (findUser.isPresent()) {
      String accessToken = jwtProvider.createAccessToken(findUser.get());
      String refreshToken = jwtProvider.createRefreshToken(findUser.get());

      redisUtil.setStringWidthExpire(refreshToken, findUser.get().getId().toString(),
          jwtProperty.getRefresh_token_expired_time());

      redisUtil.deleteByStringKey(token);
      return new TokenDto(accessToken, refreshToken);
    }
    throw new UserNotFoundException();
  }
  

  private boolean nicknameDuplicateCheck(String nickName) {
    return userRepository.existsUserByNickName(nickName);
  }

  private boolean snsUserDuplicateCheck(SnsIdentify snsIdentify) {
    return userRepository.existsUserBySnsIdentify(snsIdentify);
  }

}
