package com.spring.ai.demo.commons.dto;

import java.util.Map;
import lombok.Data;

@Data
public class GenericTemplateResponse {

  private Map<String, Object> data;
}
