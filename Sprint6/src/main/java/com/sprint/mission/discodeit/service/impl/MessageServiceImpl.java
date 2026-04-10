package com.sprint.mission.discodeit.service.impl;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ChannelRepository channelRepository;

  @Override
  @Transactional
  public MessageDto create(MessageCreateRequest request,
      List<BinaryContentCreateRequest> binaryRequests) {

    User author = userRepository.findById(request.authorId())
        .orElseThrow(() -> new NoSuchElementException("작성자를 찾을 수 없습니다."));
    Channel channel = channelRepository.findById(request.channelId())
        .orElseThrow(() -> new NoSuchElementException("채널을 찾을 수 없습니다."));

    Message message = Message.builder()
        .content(request.content())
        .author(author)
        .channel(channel)
        .build();

    Message savedMessage = messageRepository.save(message);
    return MessageMapper.toDto(savedMessage); // DTO 변환 후 반환
  }

  @Override
  public MessageDto find(UUID messageId) {
    return messageRepository.findById(messageId)
        .map(MessageMapper::toDto)
        .orElseThrow(() -> new NoSuchElementException("메시지를 찾을 수 없습니다."));
  }

  @Override
  public List<MessageDto> findAllByChannelId(UUID channelId) {
    return messageRepository.findAllByChannelId(channelId).stream()
        .map(MessageMapper::toDto)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public MessageDto update(UUID messageId, MessageUpdateRequest request) {
    Message message = messageRepository.findById(messageId)
        .orElseThrow(() -> new NoSuchElementException("메시지를 찾을 수 없습니다."));

    message.update(request.newContent()); // Dirty Checking 활용
    return MessageMapper.toDto(message);
  }

  @Override
  @Transactional
  public void delete(UUID messageId) {
    messageRepository.deleteById(messageId);
  }
}