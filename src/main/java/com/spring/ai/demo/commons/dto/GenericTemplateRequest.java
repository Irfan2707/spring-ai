package com.spring.ai.demo.commons.dto;

import java.util.Map;
import lombok.Data;

@Data
public class GenericTemplateRequest {

  private String useCase;
  private Map<String, Object> input;
}
