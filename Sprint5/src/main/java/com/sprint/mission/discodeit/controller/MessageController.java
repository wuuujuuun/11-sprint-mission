package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "메시지 (Message)", description = "메시지 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/messages")
public class MessageController {

  private final MessageService messageService;

  @Operation(summary = "메시지 생성")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Message> create(
      @RequestPart("messageCreateRequest") MessageCreateRequest messageCreateRequest,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
  ) {
    List<BinaryContentCreateRequest> attachmentRequests = Optional.ofNullable(attachments)
        .map(files -> files.stream()
            .map(file -> {
              try {
                return new BinaryContentCreateRequest(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }).toList())
        .orElse(new ArrayList<>());

    Message createdMessage = messageService.create(messageCreateRequest, attachmentRequests);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdMessage);
  }

  @Operation(summary = "메시지 수정")
  @PatchMapping("/{messageId}")
  public ResponseEntity<Message> update(
      @Parameter(description = "메시지 ID") @PathVariable("messageId") UUID messageId,
      @RequestBody MessageUpdateRequest request) {
    Message updatedMessage = messageService.update(messageId, request);
    return ResponseEntity.status(HttpStatus.OK).body(updatedMessage);
  }

  @Operation(summary = "메시지 삭제")
  @DeleteMapping("/{messageId}")
  public ResponseEntity<Void> delete(
      @Parameter(description = "삭제할 메시지 ID") @PathVariable("messageId") UUID messageId) {
    messageService.delete(messageId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Operation(summary = "채널별 메시지 조회")
  @GetMapping("/channel/{channelId}")
  public ResponseEntity<List<Message>> findAllByChannelId(
      @Parameter(description = "조회할 채널 ID") @PathVariable("channelId") UUID channelId) { // 수정됨
    List<Message> messages = messageService.findAllByChannelId(channelId);
    return ResponseEntity.status(HttpStatus.OK).body(messages);
  }
}