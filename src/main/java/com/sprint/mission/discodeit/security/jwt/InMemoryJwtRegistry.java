package com.sprint.mission.discodeit.security.jwt;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InMemoryJwtRegistry implements JwtRegistry {

  // <userId, Queue<JwtInformation>>
  private final Map<UUID, Queue<JwtInformation>> origin = new ConcurrentHashMap<>();
  private final int maxActiveJwtCount;

  public InMemoryJwtRegistry(
      @Value("${discodeit.jwt.max-active-count:1}") int maxActiveJwtCount
  ) {
    this.maxActiveJwtCount = maxActiveJwtCount;
  }

  @Override
  public synchronized void registerJwtInformation(JwtInformation jwtInformation) {
    Queue<JwtInformation> queue = origin.computeIfAbsent(
        jwtInformation.userId(), id -> new ArrayDeque<>()
    );
    queue.offer(jwtInformation);
    while (queue.size() > maxActiveJwtCount) {
      JwtInformation evicted = queue.poll();
      log.debug("최대 동시 로그인 수 초과로 토큰이 무효화되었습니다. userId={}",
          evicted != null ? evicted.userId() : null);
    }
  }

  @Override
  public synchronized void invalidateJwtInformationByUserId(UUID userId) {
    Queue<JwtInformation> removed = origin.remove(userId);
    if (removed != null && !removed.isEmpty()) {
      log.debug("{}개의 토큰이 무효화되었습니다. userId={}", removed.size(), userId);
    }
  }

  @Override
  public boolean hasActiveJwtInformationByUserId(UUID userId) {
    Queue<JwtInformation> queue = origin.get(userId);
    return queue != null && !queue.isEmpty();
  }

  @Override
  public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
    return origin.values().stream()
        .flatMap(Queue::stream)
        .anyMatch(info -> info.accessToken().equals(accessToken) && !info.isExpired());
  }

  @Override
  public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
    return origin.values().stream()
        .flatMap(Queue::stream)
        .anyMatch(info -> info.refreshToken().equals(refreshToken) && !info.isExpired());
  }

  @Override
  public synchronized JwtInformation rotateJwtInformation(String oldRefreshToken,
      JwtInformation newJwtInformation) {
    Queue<JwtInformation> queue = origin.get(newJwtInformation.userId());
    if (queue != null) {
      queue.removeIf(info -> info.refreshToken().equals(oldRefreshToken));
    }
    registerJwtInformation(newJwtInformation);
    log.debug("토큰이 로테이션되었습니다. userId={}", newJwtInformation.userId());
    return newJwtInformation;
  }

  @Override
  @Scheduled(fixedDelay = 1000 * 60 * 5)
  public synchronized void clearExpiredJwtInformation() {
    int[] removedCount = {0};
    origin.forEach((userId, queue) -> {
      int before = queue.size();
      queue.removeIf(JwtInformation::isExpired);
      removedCount[0] += before - queue.size();
    });
    origin.values().removeIf(Queue::isEmpty);
    if (removedCount[0] > 0) {
      log.debug("{}개의 만료된 토큰 정보가 정리되었습니다.", removedCount[0]);
    }
  }
}