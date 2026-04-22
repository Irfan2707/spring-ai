package com.spring.ai.demo.service;

import reactor.core.publisher.Flux;

public interface ChatWithMemoryService {

  Flux<String> getChatMemory(String query);

  Flux<String> chatMemoryWithSessionRemembering(String query, String corId);
}
