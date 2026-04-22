package com.spring.ai.demo.controller;

import com.spring.ai.demo.commons.dto.*;
import com.spring.ai.demo.service.ChatService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {

  private final ChatService chatService;

  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  @GetMapping("/groq")
  public ResponseEntity<String> chatGroq(@RequestParam String q) {
    return ResponseEntity.ok(chatService.chatWithGroq(q));
  }

  @GetMapping("/ollama")
  public ResponseEntity<String> chatOllama(@RequestParam String q) {
    return ResponseEntity.ok(chatService.chatWithOllama(q));
  }

  @GetMapping("/any")
  public ResponseEntity<String> chat(
      @RequestParam String q, @RequestParam(defaultValue = "groq") String model) {
    return ResponseEntity.ok(chatService.chatWithModel(q, model));
  }

  @GetMapping("/customize-response")
  public ResponseEntity<StructuredDto> getStructuredResponse(
      @RequestParam String prompt, @RequestParam String model) {
    return ResponseEntity.ok(chatService.getStructuredResponse(prompt, model));
  }

  @GetMapping("/customize-response/list")
  public ResponseEntity<List<StructuredDto>> getStructuredResponseList(
      @RequestParam String prompt, @RequestParam String model) {
    return ResponseEntity.ok(chatService.getStructuredResponseList(prompt, model));
  }

  @PostMapping("/template")
  public ResponseEntity<List<GenericTemplateResponse>> chatTemplateExample(
      @RequestBody GenericTemplateRequest request) {

    return ResponseEntity.ok(chatService.chatTemplate(request.getUseCase(), request.getInput()));
  }

  @PostMapping("/advisors")
  public ResponseEntity<AiResponse> advisors(@RequestBody ChatRequest request) {
    return ResponseEntity.ok(chatService.generateResponse(request));
  }

  @GetMapping("/stream-response")
  public ResponseEntity<Flux<String>> getStreamResponse(@RequestParam String query) {
    return ResponseEntity.ok(chatService.getStreamResponse(query));
  }
}
