package com.spring.ai.demo.service.impl;

import com.spring.ai.demo.commons.toolCalling.AppTools;
import com.spring.ai.demo.service.ToolCallingService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

// ToolCallingServiceImpl.java
@Service
public class ToolCallingServiceImpl implements ToolCallingService {

  private final ChatClient chatClient;
  private final AppTools appTools;

  public ToolCallingServiceImpl(
      @Qualifier("groqChatClientWithoutChatMemory") ChatClient chatClient, AppTools appTools) {
    this.chatClient = chatClient;
    this.appTools = appTools;
  }

  @Override
  public String chat(String userMessage) {
    return chatClient
        .prompt()
        // System prompt tells LLM its role and what tools it has
        .system(
            """
                You are a helpful assistant with access to real-time tools.
                Use tools when you need current information like weather, currency rates, or jokes.
                Always use the tool result to give accurate, up-to-date answers.
                """)
        .user(userMessage)
        // This is where you register ALL tools with the LLM
        // LLM reads each @Tool description and decides which one to call
        // You don't tell it WHICH tool to use — it figures that out itself
        .tools(appTools) // pass the whole toolbox — LLM picks what it needs
        .call()
        .content();
    // Internally Spring AI handles the full loop:
    // 1. Sends user message + tool definitions to LLM
    // 2. LLM replies with "call getWeather(Bengaluru)"
    // 3. Spring AI executes getWeather("Bengaluru") on your behalf
    // 4. Sends result back to LLM
    // 5. LLM generates final human-readable answer
    // 6. Returns that answer here
  }
}
