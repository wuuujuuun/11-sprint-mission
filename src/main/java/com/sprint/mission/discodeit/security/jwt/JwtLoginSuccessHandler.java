package com.sprint.mission.discodeit.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.exception.ErrorResponse;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.security.jwt.JwtDto;
import com.sprint.mission.discodeit.security.jwt.JwtInformation;
import com.sprint.mission.discodeit.security.jwt.JwtRegistry;
import com.sprint.mission.discodeit.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;
  private final JwtRegistry jwtRegistry;
  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    if (authentication.getPrincipal() instanceof DiscodeitUserDetails userDetails) {
      String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
      String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

      jwtRegistry.registerJwtInformation(new JwtInformation(
          userDetails.getUserDto().id(),
          accessToken,
          refreshToken,
          jwtTokenProvider.getExpiration(refreshToken)
      ));

      Cookie refreshTokenCookie = new Cookie(JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME,
          refreshToken);
      refreshTokenCookie.setHttpOnly(true);
      refreshTokenCookie.setPath("/");
      response.addCookie(refreshTokenCookie);

      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write(objectMapper.writeValueAsString(new JwtDto(accessToken)));

    } else {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      ErrorResponse errorResponse = new ErrorResponse(
          new RuntimeException("Authentication failed: Invalid user details"),
          HttpServletResponse.SC_UNAUTHORIZED
      );
      response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
  }
}