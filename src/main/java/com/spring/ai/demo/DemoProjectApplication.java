package com.spring.ai.demo;

import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {OpenAiEmbeddingAutoConfiguration.class})
public class DemoProjectApplication {
  public static void main(String[] args) {
    SpringApplication.run(DemoProjectApplication.class, args);
  }
}
