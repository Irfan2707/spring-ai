package com.spring.ai.demo.service.impl;

import com.spring.ai.demo.service.ChatWithMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class ChatWithMemoryServiceImpl implements ChatWithMemoryService {

  private final ChatClient groqChatClientChatMemory;

  public ChatWithMemoryServiceImpl(
      @Qualifier("groqChatClientChatMemory") ChatClient groqChatClientChatMemory) {

    this.groqChatClientChatMemory = groqChatClientChatMemory;
  }

  @Override
  public Flux<String> getChatMemory(String query) {
    return groqChatClientChatMemory.prompt().system("You are a GOAT").user(query).stream()
        .content();
  }

  @Override
  public Flux<String> chatMemoryWithSessionRemembering(String query, String corId) {
    return groqChatClientChatMemory
        .prompt()
        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, corId))
        .system("You are a GOAT")
        .user(query)
        .stream()
        .content();
  }
}
