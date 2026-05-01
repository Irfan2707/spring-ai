package com.spring.ai.demo.commons.configuration;

import com.spring.ai.demo.commons.advisors.TokenUsageAdvisor;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

  @Bean
  public SimpleLoggerAdvisor simpleLoggerAdvisor() {
    return new SimpleLoggerAdvisor();
  }



  @Bean
  public SafeGuardAdvisor safeGuardAdvisor() {
    return new SafeGuardAdvisor(List.of("Games"));
  }

  @Bean
  public TokenUsageAdvisor tokenUsageAdvisor() {
    return new TokenUsageAdvisor();
  }

  @Bean
  @Qualifier("groqChatClient")
  public ChatClient groqChatClient(
      OpenAiChatModel openAiChatModel,
      SimpleLoggerAdvisor logger,
      SafeGuardAdvisor safeGuard,
      TokenUsageAdvisor tokenUsageAdvisor,
      ChatMemory chatMemory) {

    MessageChatMemoryAdvisor messageChatMemoryAdvisor =
        MessageChatMemoryAdvisor.builder(chatMemory).build();

    return ChatClient.builder(openAiChatModel)
        .defaultSystem("You are a senior level SDE")
        .defaultOptions(
            OpenAiChatOptions.builder()
                .model("llama-3.3-70b-versatile")
                .temperature(0.7)
                .maxTokens(100)
                .build())
        .defaultAdvisors(logger, messageChatMemoryAdvisor, safeGuard, tokenUsageAdvisor)
        .build();
  }

  @Bean
  @Qualifier("groqChatClientChatMemory")
  public ChatClient groqChatClientChatMemory(
      OpenAiChatModel openAiChatModel,
      TokenUsageAdvisor tokenUsageAdvisor,
      SimpleLoggerAdvisor simpleLoggerAdvisor,
      ChatMemory chatMemory) {

    MessageChatMemoryAdvisor messageChatMemoryAdvisor =
        MessageChatMemoryAdvisor.builder(chatMemory).build();

    return ChatClient.builder(openAiChatModel)
        .defaultOptions(
            OpenAiChatOptions.builder()
                .model("llama-3.3-70b-versatile")
                .temperature(0.7)
                .maxTokens(100)
                .build())
        .defaultAdvisors(simpleLoggerAdvisor, tokenUsageAdvisor, messageChatMemoryAdvisor)
        .build();
  }


  @Bean
  public ChatMemory groqChatClientChatMemoryWithSession(
          JdbcChatMemoryRepository jdbcChatMemoryRepository) {
           return MessageWindowChatMemory.builder()
                   .chatMemoryRepository(jdbcChatMemoryRepository)
                   .maxMessages(10).build();
  }


  @Bean
  @Qualifier("ollamaChatClient")
  public ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {
    return ChatClient.builder(ollamaChatModel).build();
  }

  @Bean
  @Qualifier("groqChatClientWithoutChatMemory")
  public ChatClient groqChatClientWithoutChatMemory(OpenAiChatModel groqChatClientWithoutChatMemory) {
    return ChatClient.builder(groqChatClientWithoutChatMemory).build();
  }
}