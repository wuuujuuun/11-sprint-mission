package com.sprint.mission.discodeit.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @ManyToOne(fetch = FetchType.LAZY) // N+1 예방을 위해 지연 로딩 설정
  @JoinColumn(name = "author_id") // DB 컬럼명 지정
  private User author;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "channel_id")
  private Channel channel;

  // 첨부파일(attachmentIds)은 목차의 'BinaryContent 고도화' 단계에서
  // 별도 엔티티로 매핑할 예정이므로 잠시 비워두거나 필드만 유지합니다.

  @Builder
  public Message(String content, User author, Channel channel) {
    this.content = content;
    this.author = author;
    this.channel = channel;
  }

  public void update(String newContent) {
    if (newContent != null) {
      this.content = newContent;
    }
  }
}