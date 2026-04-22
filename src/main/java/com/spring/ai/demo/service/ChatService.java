package com.spring.ai.demo.service;

import com.spring.ai.demo.commons.dto.AiResponse;
import com.spring.ai.demo.commons.dto.ChatRequest;
import com.spring.ai.demo.commons.dto.GenericTemplateResponse;
import com.spring.ai.demo.commons.dto.StructuredDto;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;

public interface ChatService {

  String chatWithGroq(String prompt);

  String chatWithOllama(String prompt);

  String chatWithModel(String prompt, String model);

  StructuredDto getStructuredResponse(String prompt, String model);

  List<StructuredDto> getStructuredResponseList(String prompt, String model);

  List<GenericTemplateResponse> chatTemplate(String useCase, Map<String, Object> input);

  String templateUsingFluentApi();

  AiResponse generateResponse(ChatRequest request);

  Flux<String> getStreamResponse(String query);
}
