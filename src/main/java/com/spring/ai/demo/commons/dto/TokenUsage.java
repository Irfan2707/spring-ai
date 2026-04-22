package com.spring.ai.demo.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenUsage {
  private Integer promptTokens;
  private Integer completionTokens;
  private Integer totalTokens;
}
