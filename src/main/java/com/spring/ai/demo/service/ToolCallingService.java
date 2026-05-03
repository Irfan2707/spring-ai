package com.spring.ai.demo.service;

// ToolCallingService.java
public interface ToolCallingService {
  // Single method — user asks anything, tools are available automatically
  // LLM decides on its own which tool(s) to call based on the question
  String chat(String userMessage);
}
