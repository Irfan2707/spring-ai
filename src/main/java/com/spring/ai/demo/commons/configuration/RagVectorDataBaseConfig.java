package com.spring.ai.demo.commons.configuration;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RagVectorDataBaseConfig {

  @Value("${gemini.api-key}")
  private String geminiApiKey;

  @Bean
  @Primary
  public EmbeddingModel geminiEmbeddingModel() {
    OpenAiApi openAiApi =
        OpenAiApi.builder()
            .apiKey(geminiApiKey)
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai")
            .build();

    return new OpenAiEmbeddingModel(
        openAiApi,
        MetadataMode.EMBED,
        OpenAiEmbeddingOptions.builder()
            .model("gemini-embedding-001") // ✅ correct model name
            .build());
  }
}
