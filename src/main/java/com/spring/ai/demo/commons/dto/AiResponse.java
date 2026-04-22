package com.spring.ai.demo.commons.dto;

import lombok.Data;

@Data
public class AiResponse {
  private String content;
  private TokenUsage tokenUsage;
}
