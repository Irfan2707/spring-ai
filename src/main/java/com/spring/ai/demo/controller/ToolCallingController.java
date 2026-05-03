package com.spring.ai.demo.controller;

import com.spring.ai.demo.service.ToolCallingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ToolCallingController.java
@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
@Tag(name = "Tool Calling", description = "LLM with real-time tools — weather, currency, jokes")
public class ToolCallingController {

  private final ToolCallingService toolCallingService;

  @PostMapping("/chat")
  @Operation(
      summary = "Chat with tool-enabled LLM",
      description =
          """
            The LLM automatically picks the right tool based on your question.
            Try asking:
            - "What's the weather in Bengaluru?"
            - "Convert 500 USD to INR"
            - "Tell me a programming joke"
            - "What day is tomorrow?"
            - "Is it going to rain in Mumbai? Should I carry an umbrella?"
            """)
  public ResponseEntity<String> chat(@RequestBody String userMessage) {
    String response = toolCallingService.chat(userMessage);
    return ResponseEntity.ok(response);
  }
}
