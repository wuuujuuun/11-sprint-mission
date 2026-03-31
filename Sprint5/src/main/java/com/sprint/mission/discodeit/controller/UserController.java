package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "사용자 (User)", description = "사용자 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;
  private final UserStatusService userStatusService;

  @Operation(summary = "유저 생성")
  @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  public ResponseEntity<User> create(
      @RequestPart("userCreateRequest") UserCreateRequest userCreateRequest,
      @RequestPart(value = "profile", required = false) MultipartFile profile
  ) {
    Optional<BinaryContentCreateRequest> profileRequest = Optional.ofNullable(profile)
        .flatMap(this::resolveProfileRequest);
    User createdUser = userService.create(userCreateRequest, profileRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  @Operation(summary = "유저 정보 수정")
  @PatchMapping(path = "/{userId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  public ResponseEntity<User> update(
      @Parameter(description = "수정할 유저 ID") @PathVariable("userId") UUID userId,
      @RequestPart("userUpdateRequest") UserUpdateRequest userUpdateRequest,
      @RequestPart(value = "profile", required = false) MultipartFile profile
  ) {
    Optional<BinaryContentCreateRequest> profileRequest = Optional.ofNullable(profile)
        .flatMap(this::resolveProfileRequest);
    User updatedUser = userService.update(userId, userUpdateRequest, profileRequest);
    return ResponseEntity.status(HttpStatus.OK).body(updatedUser);
  }

  @Operation(summary = "유저 삭제")
  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> delete(
      @Parameter(description = "삭제할 유저 ID") @PathVariable("userId") UUID userId) {
    userService.delete(userId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Operation(summary = "전체 유저 조회")
  @GetMapping
  public ResponseEntity<List<UserDto>> findAll() {
    List<List<UserDto>> users = (List) userService.findAll();
    return ResponseEntity.status(HttpStatus.OK).body((List) users);
  }

  @Operation(summary = "유저 상태 수정")
  @PatchMapping("/{userId}/status")
  public ResponseEntity<UserStatus> updateUserStatusByUserId(
      @Parameter(description = "상태를 변경할 유저 ID") @PathVariable("userId") UUID userId,
      @RequestBody UserStatusUpdateRequest request) {
    UserStatus updatedUserStatus = userStatusService.updateByUserId(userId, request);
    return ResponseEntity.status(HttpStatus.OK).body(updatedUserStatus);
  }

  private Optional<BinaryContentCreateRequest> resolveProfileRequest(MultipartFile profileFile) {
    if (profileFile == null || profileFile.isEmpty()) {
      return Optional.empty();
    } else {
      try {
        return Optional.of(new BinaryContentCreateRequest(
            profileFile.getOriginalFilename(),
            profileFile.getContentType(),
            profileFile.getBytes()
        ));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}