package com.spring.ai.demo.commons.advisors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class TokenUsageAdvisor implements CallAdvisor, StreamAdvisor {

  //    @Override
  //    public ChatClientResponse adviseCall(ChatClientRequest request,
  //                                         CallAdvisorChain chain) {
  //
  //        log.info("Chat request using advise",request.toString());
  //
  //        ChatClientResponse response = chain.nextCall(request);
  //
  //        var usage = response.chatResponse().getMetadata().getUsage();
  //
  //        if (usage != null) {
  //            log.info("Prompt: {}", usage.getPromptTokens());
  //            log.info("Completion: {}", usage.getCompletionTokens());
  //            log.info("Total: {}", usage.getTotalTokens());
  //        }
  //
  //        return response;
  //    }

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

    log.info("===== REQUEST START =====");

    // 🔥 Get all messages
    request
        .prompt()
        .getInstructions()
        .forEach(
            message -> {
              log.info("Role: {}", message.getMessageType());
              log.info("Content: {}", message.getText());
            });

    log.info("===== REQUEST END =====");

    ChatClientResponse response = chain.nextCall(request);

    var usage = response.chatResponse().getMetadata().getUsage();

    if (usage != null) {
      log.info("Prompt: {}", usage.getPromptTokens());
      log.info("Completion: {}", usage.getCompletionTokens());
      log.info("Total: {}", usage.getTotalTokens());
    }

    return response;
  }

  @Override
  public Flux<ChatClientResponse> adviseStream(
      ChatClientRequest request, StreamAdvisorChain chain) {
    return chain.nextStream(request);
  }

  @Override
  public String getName() {
    return "TokenUsageAdvisor";
  }

  @Override
  public int getOrder() {
    return 0;
  }
}
