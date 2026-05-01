package com.spring.ai.demo.service.impl;

import com.spring.ai.demo.commons.dto.*;
import com.spring.ai.demo.service.ChatService;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class ChatImplementation implements ChatService {

  @Value("classpath:/prompts/system-prompt.st")
  private Resource systemMessage;

  @Value("classpath:/prompts/user-prompt.st")
  private Resource userMessage;

  private final ChatClient groqChatClient;
  private final ChatClient ollamaChatClient;
  private final ChatClient groqChatClientChatMemory;

  public ChatImplementation(
      @Qualifier("groqChatClient") ChatClient groqChatClient,
      @Qualifier("ollamaChatClient") ChatClient ollamaChatClient,
      @Qualifier("groqChatClientChatMemory") ChatClient groqChatClientChatMemory) {
    this.groqChatClient = groqChatClient;
    this.ollamaChatClient = ollamaChatClient;
    this.groqChatClientChatMemory = groqChatClientChatMemory;
  }

  @Override
  public String chatWithGroq(String prompt) {
    return groqChatClient.prompt(prompt).call().content();
  }

  @Override
  public String chatWithOllama(String prompt) {
    return ollamaChatClient.prompt(prompt).call().content();
  }

  @Override
  public String chatWithModel(String prompt, String model) {
    ChatClient client = model.equals("ollama") ? ollamaChatClient : groqChatClient;
    return client.prompt(prompt).call().content();
  }

  @Override
  public StructuredDto getStructuredResponse(
      @RequestParam String prompt, @RequestParam String model) {

    //        call the llm for response

    ChatClient chatClient = model.equals("ollama") ? ollamaChatClient : groqChatClient;

    return chatClient
        .prompt()
        .user(prompt)
        .system("As an expert in cricket")
        .call()
        .entity(StructuredDto.class);
  }

  @Override
  public List<StructuredDto> getStructuredResponseList(String prompt, String model) {

    ChatClient chatClient = model.equals("ollama") ? ollamaChatClient : groqChatClient;

    return chatClient
        .prompt()
        .user(prompt)
        .system("As an expert in cricket")
        .call()
        .entity(new ParameterizedTypeReference<List<StructuredDto>>() {});
  }

  @Override
  public String templateUsingFluentApi() {
    return groqChatClient
        .prompt()
        .advisors(new SimpleLoggerAdvisor())
        .system(promptSystemSpec -> promptSystemSpec.text(systemMessage).param("name", "john"))
        .user(
            promptUserSpec ->
                promptUserSpec
                    .text(userMessage)
                    .params(Map.of("language", "java", "case", "interface")))
        .call()
        .content();
  }

  @Override
  public List<GenericTemplateResponse> chatTemplate(String useCase, Map<String, Object> input) {

    String systemPrompt;
    String userPrompt;

    switch (useCase.toLowerCase()) {
      case "resume":
        systemPrompt =
            """
                        You are a senior technical interviewer.
                        Return ONLY valid JSON array.

                        Structure:
                        {
                          "skill": "string",
                          "rating": "1-10",
                          "remarks": "string"
                        }
                        """;

        userPrompt =
            String.format(
                """
                        Candidate Profile:
                        Experience: %s years
                        Skills: %s
                        Projects: %s

                        Evaluate the candidate.
                        """,
                input.get("experience"), input.get("skills"), input.get("projects"));
        break;

      case "ecommerce":
        systemPrompt =
            """
                        You are a recommendation engine.
                        Return ONLY valid JSON array.

                        Structure:
                        {
                          "product": "string",
                          "reason": "string",
                          "score": "1-10"
                        }
                        """;

        userPrompt =
            String.format(
                """
                        User Preferences:
                        Category: %s
                        Budget: %s
                        Interest: %s

                        Recommend products.
                        """,
                input.get("category"), input.get("budget"), input.get("interest"));
        break;

      case "fraud":
        systemPrompt =
            """
                        You are a fraud detection system.
                        Return ONLY valid JSON array.

                        Structure:
                        {
                          "transaction": "string",
                          "riskLevel": "LOW/MEDIUM/HIGH",
                          "reason": "string"
                        }
                        """;

        userPrompt =
            String.format(
                """
                        Transactions:
                        %s

                        Analyze risk.
                        """,
                input.get("transactions"));
        break;

      default:
        throw new IllegalArgumentException("Invalid use case");
    }

    log.info("UseCase: {}, Input: {}", useCase, input);

    try {
      List<Map<String, Object>> response =
          groqChatClient
              .prompt()
              .system(systemPrompt)
              .user(userPrompt)
              .call()
              .entity(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

      return response.stream()
          .map(
              map -> {
                GenericTemplateResponse dto = new GenericTemplateResponse();
                dto.setData(map);
                return dto;
              })
          .toList();

    } catch (Exception e) {
      log.error("Groq failed, falling back to Ollama", e);

      List<Map<String, Object>> response =
          ollamaChatClient
              .prompt()
              .system(systemPrompt)
              .user(userPrompt)
              .call()
              .entity(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

      return response.stream()
          .map(
              map -> {
                GenericTemplateResponse dto = new GenericTemplateResponse();
                dto.setData(map);
                return dto;
              })
          .toList();
    }
  }

  @Override
  public AiResponse generateResponse(ChatRequest request) {

    ChatClientResponse response =
        groqChatClient
            .prompt()
            .system(spec -> spec.text(systemMessage))
            .user(
                spec ->
                    spec.text(userMessage)
                        .param("name", request.getName())
                        .param("topic", request.getTopic()))
            .call()
            .chatClientResponse();

    String content = response.chatResponse().getResult().getOutput().getText();

    var usage = response.chatResponse().getMetadata().getUsage();

    TokenUsage tokenUsage =
        new TokenUsage(
            usage != null ? usage.getPromptTokens() : 0,
            usage != null ? usage.getCompletionTokens() : 0,
            usage != null ? usage.getTotalTokens() : 0);

    AiResponse aiResponse = new AiResponse();
    aiResponse.setContent(content);
    aiResponse.setTokenUsage(tokenUsage);

    return aiResponse;
  }

  @Override
  public Flux<String> getStreamResponse(String query) {

    return groqChatClient
        .prompt()
        .system(promptSystemSpec -> promptSystemSpec.text(systemMessage))
        .user(
            promptUserSpec ->
                promptUserSpec.text(userMessage).param("name", "kim").param("topic", query))
        .stream()
        .content();
  }
}
