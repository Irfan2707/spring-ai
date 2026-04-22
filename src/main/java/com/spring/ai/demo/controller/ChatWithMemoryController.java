package com.spring.ai.demo.controller;

import com.spring.ai.demo.service.ChatWithMemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatWithMemoryController {

  private final ChatWithMemoryService chatWithMemoryService;

  public ChatWithMemoryController(ChatWithMemoryService chatWithMemoryService) {
    this.chatWithMemoryService = chatWithMemoryService;
  }

  @GetMapping("/chat-memory")
  public ResponseEntity<Flux<String>> getChatMemory(@RequestParam String query) {
    return ResponseEntity.ok(chatWithMemoryService.getChatMemory(query));
  }

  @GetMapping("/chat-memory/session")
  public ResponseEntity<Flux<String>> chatMemoryWithSessionRemembering(
      @RequestParam("q") String query, @RequestParam("corId") String corId) {
    return ResponseEntity.ok(chatWithMemoryService.chatMemoryWithSessionRemembering(query, corId));
  }
}
