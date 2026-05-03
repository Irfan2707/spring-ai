package com.spring.ai.demo.commons.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

  // In your @Configuration class
  @Bean
  public RestTemplate restTemplate() {
    // RestTemplate is needed by AppTools to call external APIs
    // It's a simple HTTP client — like axios in JS or requests in Python
    return new RestTemplate();
  }
}
