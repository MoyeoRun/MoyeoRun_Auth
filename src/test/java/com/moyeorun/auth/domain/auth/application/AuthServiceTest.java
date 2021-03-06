package com.moyeorun.auth.domain.auth.application;

import com.moyeorun.auth.domain.auth.dao.UserRepository;
import com.moyeorun.auth.domain.auth.domain.SnsIdentify;
import com.moyeorun.auth.domain.auth.domain.User;
import com.moyeorun.auth.domain.auth.domain.contant.GenderType;
import com.moyeorun.auth.domain.auth.domain.contant.RoleType;
import com.moyeorun.auth.domain.auth.domain.contant.SnsProviderType;
import com.moyeorun.auth.domain.auth.dto.request.RefreshRequest;
import com.moyeorun.auth.domain.auth.dto.request.SignUpRequest;
import com.moyeorun.auth.domain.auth.dto.response.RefreshResponse;
import com.moyeorun.auth.domain.auth.dto.response.SignInResponse;
import com.moyeorun.auth.domain.auth.dto.response.SignUpResponse;
import com.moyeorun.auth.domain.auth.exception.DuplicateNicknameException;
import com.moyeorun.auth.domain.auth.exception.DuplicateSnsUserException;
import com.moyeorun.auth.domain.auth.exception.NotSignInException;
import com.moyeorun.auth.global.common.response.MessageResponseDto;
import com.moyeorun.auth.global.config.property.JwtProperty;
import com.moyeorun.auth.global.error.ErrorCode;
import com.moyeorun.auth.global.security.exception.InvalidJwtException;
import com.moyeorun.auth.global.security.jwt.JwtClaimsVo;
import com.moyeorun.auth.global.security.jwt.JwtProvider;
import com.moyeorun.auth.global.security.jwt.JwtResolver;
import com.moyeorun.auth.global.util.RedisUtil;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

  @Mock
  UserRepository userRepository;

  @Mock
  JwtProvider jwtProvider;

  @Mock
  JwtResolver jwtResolver;

  @Mock
  JwtProperty jwtProperty;

  @Mock
  RedisUtil redisUtil;

  @InjectMocks
  AuthService authService;


  @DisplayName("??????????????? ????????? ?????? ??????")
  @Test
  void signUp_????????????????????????() {
    SignUpRequest dto = signUpRequestDtoMock();
    SnsIdentify snsIdentify = new SnsIdentify("12345", SnsProviderType.GOOGLE);
    String email = "test@test.com";

    given(userRepository.existsUserByNickName(any())).willReturn(true);
    DuplicateNicknameException exception = assertThrows(DuplicateNicknameException.class,
        () -> authService.signUp(dto, snsIdentify, email));

    assertEquals(ErrorCode.NICKNAME_DUPLICATE, exception.getErrorCode());
  }

  @DisplayName("??????????????? ?????? ?????? ??????")
  @Test
  void signUp_?????????????????????() {
    SignUpRequest dto = signUpRequestDtoMock();
    SnsIdentify snsIdentify = new SnsIdentify("12345", SnsProviderType.GOOGLE);
    String email = "test@test.com";

    given(userRepository.existsUserByNickName(any())).willReturn(false);
    given(userRepository.existsUserBySnsIdentify(any())).willReturn(true);

    DuplicateSnsUserException exception = assertThrows(DuplicateSnsUserException.class,
        () -> authService.signUp(dto, snsIdentify, email));

    assertEquals(ErrorCode.SNS_USER_DUPLICATE, exception.getErrorCode());
  }

  @DisplayName("???????????? ??????")
  @Test
  void signUp_??????() {
    SignUpRequest dto = signUpRequestDtoMock();
    SnsIdentify snsIdentify = new SnsIdentify("12345", SnsProviderType.GOOGLE);
    String email = "test@test.com";
    Optional<User> user = stubUserOne();

    given(userRepository.existsUserByNickName(any())).willReturn(false);
    given(userRepository.existsUserBySnsIdentify(any())).willReturn(false);
    given(userRepository.save(any())).willReturn(user.get());

    SignUpResponse result = authService.signUp(dto, snsIdentify, email);

    assertEquals(user.get().getId(), result.getUserId());
  }

  @DisplayName("????????? ??? ?????? ?????? ????????? ??????")
  @Test
  void singIn_?????????_isNewUserFalse() {
    SnsIdentify snsIdentify = new SnsIdentify("12345", SnsProviderType.GOOGLE);

    given(userRepository.findBySnsIdentify(any())).willReturn(Optional.ofNullable(null));

    SignInResponse result = authService.signIn(snsIdentify);

    assertTrue(result.getIsNewUser());
  }

  @DisplayName("????????? ??????")
  @Test
  void singIn_??????() {
    SnsIdentify snsIdentify = new SnsIdentify("12345", SnsProviderType.GOOGLE);

    given(userRepository.findBySnsIdentify(any())).willReturn(stubUserOne());

    SignInResponse result = authService.signIn(snsIdentify);

    assertFalse(result.getIsNewUser());
    assertEquals(1L, result.getUserId());
  }

  @DisplayName("refresh ?????????, ???????????? ?????? refreshToken ??????(????????? ??????)")
  @Test
  void refresh_?????????_????????????_??????() {
    String mockAccessToken = "accessToken";
    String mockRefreshToken = "refreshToken";
    RefreshRequest refreshRequest = new RefreshRequest(mockAccessToken, mockRefreshToken);
    JwtClaimsVo jwtClaimsVo = new JwtClaimsVo("1", RoleType.USER);

    given(jwtResolver.getClaimByJwt(any())).willReturn(jwtClaimsVo);
    given(redisUtil.getValueByStringKey(any())).willReturn(null);

    NotSignInException exception = assertThrows(NotSignInException.class, () ->
        authService.refresh(refreshRequest));

    assertEquals(ErrorCode.NOT_SIGN_IN_USER, exception.getErrorCode());
  }

  @DisplayName("refresh ?????????,????????? ????????? ???????????? ??????")
  @Test
  void refresh_??????????????????_?????????() {
    String mockRefreshToken = "refreshToken";
    String userIdString = "1";
    String mockAccessToken = "accessToken";
    RefreshRequest refreshRequest = new RefreshRequest(mockAccessToken, mockRefreshToken);
    JwtClaimsVo jwtClaimsVo = new JwtClaimsVo(userIdString, RoleType.USER);

    given(jwtResolver.getClaimByJwt(any())).willReturn(jwtClaimsVo);
    given(redisUtil.getValueByStringKey(any())).willReturn("otherRefreshToken");

    InvalidJwtException exception = assertThrows(InvalidJwtException.class, () ->
        authService.refresh(refreshRequest));

    assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
  }

  @DisplayName("refresh ????????? ??????")
  @Test
  void refresh_??????() {
    String mockAccessToken = "accessToken";
    String mockRefreshToken = "refreshToken";
    String createdAccessToken = "createdAccessToken";
    String userIdString = "1";
    RefreshRequest refreshRequest = new RefreshRequest(mockAccessToken, mockRefreshToken);
    JwtClaimsVo jwtClaimsVo = new JwtClaimsVo(userIdString, RoleType.USER);

    given(jwtResolver.getClaimByJwt(any())).willReturn(jwtClaimsVo);
    given(redisUtil.getValueByStringKey(any())).willReturn(mockRefreshToken);
    given(jwtProvider.createAccessToken(any(), any())).willReturn(createdAccessToken);

    RefreshResponse result = authService.refresh(refreshRequest);

    assertEquals(createdAccessToken, result.getAccessToken());
  }

  @DisplayName("???????????? ?????????, ???????????? ?????? ?????? ??????")
  @Test
  void logOut_??????() {

    given(redisUtil.getValueByStringKey(any())).willReturn(null);

    NotSignInException exception = assertThrows(NotSignInException.class,
        () -> authService.logout("1"));

    assertEquals(ErrorCode.NOT_SIGN_IN_USER, exception.getErrorCode());
  }

  @DisplayName("???????????? ??????")
  @Test
  void logOut_??????() {
    String mockRefreshToken = "refreshToken";

    given(redisUtil.getValueByStringKey(any())).willReturn(mockRefreshToken);

    MessageResponseDto result = authService.logout("1");

    assertEquals("???????????? ??????", result.getMessage());
  }

  private SignUpRequest signUpRequestDtoMock() {
    return new SignUpRequest("idtokenValue", SnsProviderType.GOOGLE, "imageurl..", "name1",
        "nickname1", GenderType.MALE);
  }

  private Optional<User> stubUserOne() {
    User user = User.builder()
        .email("email@email.com")
        .snsIdentify(new SnsIdentify("12345", SnsProviderType.GOOGLE))
        .gender(GenderType.MALE)
        .nickName("nickname..")
        .name("name..")
        .image("imageurl..")
        .build();
    ReflectionTestUtils.setField(user, "id", 1L);
    return Optional.of(user);
  }
}
