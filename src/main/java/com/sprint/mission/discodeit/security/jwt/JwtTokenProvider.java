package com.sprint.mission.discodeit.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

  private static final String CLAIM_ROLE = "role";

  private final JWSSigner signer;
  private final JWSVerifier verifier;
  private final long accessTokenExpiration;
  private final long refreshTokenExpiration;

  public JwtTokenProvider(
      @Value("${discodeit.jwt.secret}") String secret,
      @Value("${discodeit.jwt.access-token-expiration}") long accessTokenExpiration,
      @Value("${discodeit.jwt.refresh-token-expiration}") long refreshTokenExpiration
  ) {
    try {
      byte[] secretBytes = secret.getBytes();
      this.signer = new MACSigner(secretBytes);
      this.verifier = new MACVerifier(secretBytes);
    } catch (JOSEException e) {
      throw new IllegalStateException("JWT 서명 키 초기화에 실패했습니다.", e);
    }
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  public String generateAccessToken(DiscodeitUserDetails userDetails) {
    Instant now = Instant.now();
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject(userDetails.getUserDto().id().toString())
        .claim(CLAIM_ROLE, userDetails.getUserDto().role().name())
        .issueTime(Date.from(now))
        .expirationTime(Date.from(now.plusMillis(accessTokenExpiration)))
        .build();
    return sign(claimsSet);
  }

  public String generateRefreshToken(DiscodeitUserDetails userDetails) {
    Instant now = Instant.now();
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject(userDetails.getUserDto().id().toString())
        .issueTime(Date.from(now))
        .expirationTime(Date.from(now.plusMillis(refreshTokenExpiration)))
        .build();
    return sign(claimsSet);
  }

  private String sign(JWTClaimsSet claimsSet) {
    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
    try {
      signedJWT.sign(signer);
    } catch (JOSEException e) {
      throw new IllegalStateException("토큰 서명에 실패했습니다.", e);
    }
    return signedJWT.serialize();
  }

  public boolean validateToken(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      if (!signedJWT.verify(verifier)) {
        return false;
      }
      Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
      return expiration != null && expiration.after(new Date());
    } catch (ParseException | JOSEException e) {
      log.debug("유효하지 않은 토큰입니다: {}", e.getMessage());
      return false;
    }
  }

  public Instant getExpiration(String token) {
    return getClaimsSet(token).getExpirationTime().toInstant();
  }

  public String getSubject(String token) {
    return getClaimsSet(token).getSubject();
  }

  public String reissueAccessToken(String refreshToken) {
    if (!validateToken(refreshToken)) {
      throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
    }
    JWTClaimsSet claims = getClaimsSet(refreshToken);
    Instant now = Instant.now();
    JWTClaimsSet newClaims = new JWTClaimsSet.Builder()
        .subject(claims.getSubject())
        .issueTime(Date.from(now))
        .expirationTime(Date.from(now.plusMillis(accessTokenExpiration)))
        .build();
    return sign(newClaims);
  }

  private JWTClaimsSet getClaimsSet(String token) {
    try {
      return SignedJWT.parse(token).getJWTClaimsSet();
    } catch (ParseException e) {
      throw new IllegalArgumentException("유효하지 않은 토큰 형식입니다.", e);
    }
  }
}